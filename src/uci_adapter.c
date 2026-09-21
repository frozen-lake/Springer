#include "uci_adapter.h"

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "attack_data.h"
#include "search.h"
#include "transposition_table.h"
#include "move_gen.h"

#define UCI_LINE_MAX 1024

static char* skip_spaces(char* s){
    while(s != NULL && *s != '\0' && isspace((unsigned char)*s)){
        s++;
    }
    return s;
}

static void strip_trailing_newline(char* s){
    size_t len;
    if(s == NULL){
        return;
    }
    len = strlen(s);
    while(len > 0 && (s[len - 1] == '\n' || s[len - 1] == '\r')){
        s[len - 1] = '\0';
        len--;
    }
}

static int starts_with_token(const char* text, const char* token){
    size_t token_len;
    if(text == NULL || token == NULL){
        return 0;
    }

    token_len = strlen(token);
    if(strncmp(text, token, token_len) != 0){
        return 0;
    }

    return text[token_len] == '\0' || isspace((unsigned char)text[token_len]);
}

typedef struct {
    int depth;
    int has_depth;
    int movetime_ms;
} UciGoParams;

static int apply_uci_moves(Game* game, char* moves_text){
    char* token = NULL;

    if(game == NULL){
        return 0;
    }
    if(moves_text == NULL || *moves_text == '\0'){
        return 1;
    }

    token = strtok(moves_text, " \t");
    while(token != NULL){
        Move move = 0;
        if(!parse_uci_move(token, game, &move)){
            return 0;
        }
        make_move(game, move);
        token = strtok(NULL, " \t");
    }

    return 1;
}

static int handle_position_command(Game* game, const char* args){
    char buffer[UCI_LINE_MAX];
    char* cursor;
    char* moves_marker;

    if(game == NULL || args == NULL){
        return 0;
    }

    if(strlen(args) >= sizeof(buffer)){
        return 0;
    }

    strcpy(buffer, args);
    cursor = skip_spaces(buffer);

    if(starts_with_token(cursor, "startpos")){
        initialize_game(game);
        cursor += strlen("startpos");
        cursor = skip_spaces(cursor);
        if(starts_with_token(cursor, "moves")){
            cursor += strlen("moves");
            cursor = skip_spaces(cursor);
            return apply_uci_moves(game, cursor);
        }
        return 1;
    }

    if(starts_with_token(cursor, "fen")){
        char fen[256] = "";
        char* fen_tokens[6] = {0};
        int i;

        cursor += strlen("fen");
        cursor = skip_spaces(cursor);
        moves_marker = strstr(cursor, " moves ");
        if(moves_marker != NULL){
            *moves_marker = '\0';
        }

        {
            char* token = strtok(cursor, " \t");
            for(i = 0; i < 6 && token != NULL; i++){
                fen_tokens[i] = token;
                token = strtok(NULL, " \t");
            }
            if(i != 6 || token != NULL){
                return 0;
            }
        }

        for(i = 0; i < 6; i++){
            size_t needed = strlen(fen) + strlen(fen_tokens[i]) + 2;
            if(needed >= sizeof(fen)){
                return 0;
            }
            if(i > 0){
                strcat(fen, " ");
            }
            strcat(fen, fen_tokens[i]);
        }

        if(!load_fen(game, fen)){
            return 0;
        }

        if(moves_marker != NULL){
            char* move_text = moves_marker + strlen(" moves ");
            return apply_uci_moves(game, move_text);
        }

        return 1;
    }

    return 0;
}

static UciGoParams parse_go_params(const char* args, int default_depth){
    char buffer[UCI_LINE_MAX];
    char* token;
    UciGoParams params;

    params.depth = default_depth;
    params.has_depth = 0;
    params.movetime_ms = 0;

    if(args == NULL){
        return params;
    }
    if(strlen(args) >= sizeof(buffer)){
        return params;
    }

    strcpy(buffer, args);
    token = strtok(buffer, " \t");
    while(token != NULL){
        if(strcmp(token, "depth") == 0){
            char* value = strtok(NULL, " \t");
            if(value != NULL){
                int parsed = atoi(value);
                if(parsed > 0){
                    params.depth = parsed;
                    params.has_depth = 1;
                }
            }
        } else if(strcmp(token, "movetime") == 0){
            char* value = strtok(NULL, " \t");
            if(value != NULL){
                int parsed = atoi(value);
                if(parsed > 0){
                    params.movetime_ms = parsed;
                }
            }
        }
        token = strtok(NULL, " \t");
    }

    return params;
}

static int elapsed_ms_since(clock_t start){
    clock_t now = clock();
    if(now < start){
        return 0;
    }
    return (int)(((double)(now - start) * 1000.0) / (double)CLOCKS_PER_SEC);
}

static int parse_uci_square(char file, char rank){
    if(file < 'a' || file > 'h' || rank < '1' || rank > '8'){
        return -1;
    }

    return (rank - '1') * 8 + (file - 'a');
}

static int promotion_char_to_enum(char promotion_char, int* out_promotion){
    if(out_promotion == NULL){
        return 0;
    }

    switch((char)tolower((unsigned char)promotion_char)){
        case 'n':
            *out_promotion = KNIGHT_PROMOTION;
            return 1;
        case 'b':
            *out_promotion = BISHOP_PROMOTION;
            return 1;
        case 'r':
            *out_promotion = ROOK_PROMOTION;
            return 1;
        case 'q':
            *out_promotion = QUEEN_PROMOTION;
            return 1;
        default:
            return 0;
    }
}

static int promotion_enum_to_char(int promotion, char* out_promotion_char){
    if(out_promotion_char == NULL){
        return 0;
    }

    switch(promotion){
        case KNIGHT_PROMOTION:
            *out_promotion_char = 'n';
            return 1;
        case BISHOP_PROMOTION:
            *out_promotion_char = 'b';
            return 1;
        case ROOK_PROMOTION:
            *out_promotion_char = 'r';
            return 1;
        case QUEEN_PROMOTION:
            *out_promotion_char = 'q';
            return 1;
        default:
            return 0;
    }
}

int move_to_uci(Move move, char* out, size_t out_size){
    int src;
    int dest;
    int promotion;

    if(out == NULL || out_size < 5 || move == 0){
        return 0;
    }

    src = get_move_src(move);
    dest = get_move_dest(move);
    promotion = get_move_promotion(move);

    if(src < 0 || src >= 64 || dest < 0 || dest >= 64){
        return 0;
    }

    out[0] = (char)('a' + (src % 8));
    out[1] = (char)('1' + (src / 8));
    out[2] = (char)('a' + (dest % 8));
    out[3] = (char)('1' + (dest / 8));

    if(promotion == NO_PROMOTION){
        out[4] = '\0';
        return 1;
    }

    if(out_size < UCI_MOVE_STR_LEN){
        return 0;
    }

    if(!promotion_enum_to_char(promotion, &out[4])){
        return 0;
    }

    out[5] = '\0';
    return 1;
}

int parse_uci_move(const char* uci, Game* game, Move* out_move){
    size_t len;
    int src;
    int dest;
    int has_promotion;
    int requested_promotion;
    int i;

    if(uci == NULL || game == NULL || out_move == NULL){
        return 0;
    }

    len = strlen(uci);
    if(len != 4 && len != 5){
        return 0;
    }

    src = parse_uci_square((char)tolower((unsigned char)uci[0]), uci[1]);
    dest = parse_uci_square((char)tolower((unsigned char)uci[2]), uci[3]);
    if(src < 0 || dest < 0){
        return 0;
    }

    has_promotion = len == 5;
    requested_promotion = NO_PROMOTION;
    if(has_promotion && !promotion_char_to_enum(uci[4], &requested_promotion)){
        return 0;
    }

    generate_legal_moves(game, game->state.side_to_move);

    for(i = 0; i < game->legal_moves.size; i++){
        Move candidate = game->legal_moves.moves[i];
        int candidate_promotion;

        if(get_move_src(candidate) != src || get_move_dest(candidate) != dest){
            continue;
        }

        candidate_promotion = get_move_promotion(candidate);
        if(has_promotion){
            if(candidate_promotion != requested_promotion){
                continue;
            }
        } else if(candidate_promotion != NO_PROMOTION){
            continue;
        }

        *out_move = candidate;
        return 1;
    }

    return 0;
}

int algebraic_to_uci(const char* algebraic, Game* game, char* out, size_t out_size){
    Move move;

    if(algebraic == NULL || game == NULL || out == NULL){
        return 0;
    }

    move = parse_algebraic_move((char*)algebraic, game);
    if(move == 0){
        return 0;
    }

    generate_legal_moves(game, game->state.side_to_move);
    if(!is_legal_player_move(game, move)){
        return 0;
    }

    return move_to_uci(move, out, out_size);
}

int uci_to_simplified_algebraic(const char* uci, Game* game, char* out, size_t out_size){
    Move move;

    if(uci == NULL || game == NULL || out == NULL){
        return 0;
    }

    if(!parse_uci_move(uci, game, &move)){
        return 0;
    }

    return move_to_simplified_algebraic(move, out, out_size);
}

// Run Springer in UCI mode over stdin/stdout.
int run_uci_loop(FILE* in, FILE* out, FILE* err){
    char line[UCI_LINE_MAX];
    Game* game = NULL;
    TranspositionTable tt = (TranspositionTable){0};
    SearchState search_state = (SearchState){0};
    int stop = 0;
    int status = 0;

    if(in == NULL || out == NULL || err == NULL){
        return 0;
    }

    initialize_attack_data();

    game = create_game();
    if(game == NULL){
        fprintf(err, "uci error: unable to create game\n");
        return 0;
    }
    initialize_game(game);

    tt_init(&tt);
    if(!initialize_searchstate(&search_state, &tt, 4, &stop)){
        fprintf(err, "uci error: unable to initialize search state\n");
        tt_free(&tt);
        destroy_game(game);
        return 0;
    }

    status = 1;
    while(fgets(line, sizeof(line), in) != NULL){
        char* cmd = line;
        strip_trailing_newline(cmd);
        cmd = skip_spaces(cmd);
        if(*cmd == '\0'){
            continue;
        }

        if(strcmp(cmd, "uci") == 0){
            fprintf(out, "id name Springer\n");
            fprintf(out, "id author Springer contributors\n");
            fprintf(out, "uciok\n");
            fflush(out);
            continue;
        }

        if(strcmp(cmd, "isready") == 0){
            fprintf(out, "readyok\n");
            fflush(out);
            continue;
        }

        if(strcmp(cmd, "ucinewgame") == 0){
            initialize_game(game);
            tt_clear(&tt);
            clear_search_stop(&search_state);
            reset_searchstate(&search_state);
            continue;
        }

        if(starts_with_token(cmd, "position")){
            char* args = skip_spaces(cmd + strlen("position"));
            if(!handle_position_command(game, args)){
                fprintf(err, "uci warning: invalid position command\n");
            }
            continue;
        }

        if(starts_with_token(cmd, "go")){
            char* args = skip_spaces(cmd + strlen("go"));
            UciGoParams go_params = parse_go_params(args, search_state.max_depth);
            int depth = go_params.depth;
            int movetime_ms = go_params.movetime_ms;
            Move best_move = 0;
            char move_text[UCI_MOVE_STR_LEN] = "0000";
            int old_max_depth = search_state.max_depth;
            int requested_max_depth = depth;

            clear_search_stop(&search_state);

            if(movetime_ms > 0 && !go_params.has_depth){
                requested_max_depth = MAX_SEARCH_PLY;
            } else if(requested_max_depth <= 0){
                requested_max_depth = old_max_depth;
            }

            if(requested_max_depth <= 0){
                requested_max_depth = MAX_SEARCH_PLY;
            }
            if(requested_max_depth > MAX_SEARCH_PLY){
                requested_max_depth = MAX_SEARCH_PLY;
            }
            search_state.max_depth = requested_max_depth;

            if(movetime_ms > 0){
                clock_t start = clock();
                int max_depth = requested_max_depth;

                for(int current_depth = 1; current_depth <= max_depth; current_depth++){
                    Move candidate;

                    if(best_move != 0 && elapsed_ms_since(start) >= movetime_ms){
                        break;
                    }

                    candidate = search_root(game, &search_state, current_depth);
                    if(candidate == 0){
                        break;
                    }

                    best_move = candidate;

                    if(elapsed_ms_since(start) >= movetime_ms){
                        break;
                    }
                }
            } else {
                search_state.root_depth = depth;
                best_move = search_best_move(game, &search_state);
            }
            search_state.max_depth = old_max_depth;

            if(best_move != 0){
                (void)move_to_uci(best_move, move_text, sizeof(move_text));
            }

            fprintf(out, "bestmove %s\n", move_text);
            fflush(out);
            continue;
        }

        if(strcmp(cmd, "stop") == 0){
            request_search_stop(&search_state);
            continue;
        }

        if(strcmp(cmd, "quit") == 0){
            break;
        }
    }

    destroy_searchstate(&search_state);
    tt_free(&tt);
    destroy_game(game);
    return status;
}

#ifdef UCI_MODE_MAIN
int main(void){
    return run_uci_loop(stdin, stdout, stderr) ? 0 : 1;
}
#endif
