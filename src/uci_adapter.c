#include "uci_adapter.h"

#include <ctype.h>
#include <stdio.h>
#include <string.h>

#include "move_gen.h"

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
