#include "uci_tests.h"

#include <stdio.h>
#include <string.h>

#include "tests.h"
#include "../src/board.h"
#include "../src/game.h"
#include "../src/move.h"
#include "../src/uci.h"

int test_move_to_uci_basic(void){
    Game* game = create_game();
    char out[UCI_MOVE_STR_LEN];
    Move move;
    int success;

    if(game == NULL){
        return 0;
    }

    initialize_game(game);
    move = encode_move(E2, E4, &game->state);

    success = move_to_uci(move, out, sizeof(out));
    success = success && strcmp(out, "e2e4") == 0;

    destroy_game(game);
    return success;
}

int test_move_to_uci_promotion(void){
    Game* game = create_game();
    char out[UCI_MOVE_STR_LEN];
    Move promotion;
    int success;

    if(game == NULL){
        return 0;
    }

    success = load_fen(game, "4k3/3P4/8/8/8/8/8/4K3 w - - 0 1");
    promotion = encode_promotion(D7, D8, &game->state, QUEEN_PROMOTION);

    success = success && move_to_uci(promotion, out, sizeof(out));
    success = success && strcmp(out, "d7d8q") == 0;

    destroy_game(game);
    return success;
}

int test_parse_uci_move_basic(void){
    Game* game = create_game();
    Move move;
    Move expected;
    int success;

    if(game == NULL){
        return 0;
    }

    initialize_game(game);
    expected = encode_move(E2, E4, &game->state);

    success = parse_uci_move("e2e4", game, &move);
    success = success && (move == expected);

    destroy_game(game);
    return success;
}

int test_parse_uci_move_castling(void){
    Game* game = create_game();
    Move move;
    int success;

    if(game == NULL){
        return 0;
    }

    success = load_fen(game, "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
    success = success && parse_uci_move("e1g1", game, &move);
    success = success && get_move_special(move) == Kingside;
    success = success && get_move_src(move) == E1;
    success = success && get_move_dest(move) == G1;

    destroy_game(game);
    return success;
}

int test_parse_uci_move_en_passant(void){
    Game* game = create_game();
    Move move;
    int success;

    if(game == NULL){
        return 0;
    }

    success = load_fen(game, "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
    success = success && parse_uci_move("e5d6", game, &move);
    success = success && get_move_special(move) == EnPassant;
    success = success && get_move_src(move) == E5;
    success = success && get_move_dest(move) == D6;

    destroy_game(game);
    return success;
}

int test_parse_uci_move_promotion(void){
    Game* game = create_game();
    Move move;
    int success;

    if(game == NULL){
        return 0;
    }

    success = load_fen(game, "4k3/3P4/8/8/8/8/8/4K3 w - - 0 1");
    success = success && parse_uci_move("d7d8q", game, &move);
    success = success && get_move_promotion(move) == QUEEN_PROMOTION;
    success = success && get_move_src(move) == D7;
    success = success && get_move_dest(move) == D8;

    destroy_game(game);
    return success;
}

int test_parse_uci_move_rejects_invalid(void){
    Game* game = create_game();
    Move move;
    int success;

    if(game == NULL){
        return 0;
    }

    initialize_game(game);
    success = !parse_uci_move("e2e9", game, &move);
    success = success && !parse_uci_move("e2e4x", game, &move);
    success = success && !parse_uci_move("i2e4", game, &move);

    success = success && load_fen(game, "4k3/3P4/8/8/8/8/8/4K3 w - - 0 1");
    success = success && !parse_uci_move("d7d8", game, &move);

    destroy_game(game);
    return success;
}

int test_algebraic_to_uci(void){
    Game* game = create_game();
    char out[UCI_MOVE_STR_LEN];
    int success;

    if(game == NULL){
        return 0;
    }

    initialize_game(game);
    success = algebraic_to_uci("Nf3", game, out, sizeof(out));
    success = success && strcmp(out, "g1f3") == 0;

    destroy_game(game);
    return success;
}

int test_uci_to_simplified_algebraic(void){
    Game* game = create_game();
    char out[8];
    int success;

    if(game == NULL){
        return 0;
    }

    initialize_game(game);
    success = uci_to_simplified_algebraic("g1f3", game, out, sizeof(out));
    success = success && strcmp(out, "Nf3") == 0;

    destroy_game(game);
    return success;
}

#define UCI_TEST_IN_PATH "uci_test_in.tmp"
#define UCI_TEST_OUT_PATH "uci_test_out.tmp"
#define UCI_TEST_ERR_PATH "uci_test_err.tmp"

/* Runs a UCI session and copies the move from the bestmove line into move_out.
 * Uses files in the working directory because tmpfile() fails on Windows without admin rights. */
static int uci_bestmove(const char* input, char* move_out, size_t move_out_size){
    FILE* in = fopen(UCI_TEST_IN_PATH, "w+");
    FILE* out = fopen(UCI_TEST_OUT_PATH, "w+");
    FILE* err = fopen(UCI_TEST_ERR_PATH, "w+");
    char line[256];
    int found = 0;

    if(in == NULL || out == NULL || err == NULL){
        if(in != NULL){ fclose(in); }
        if(out != NULL){ fclose(out); }
        if(err != NULL){ fclose(err); }
        remove(UCI_TEST_IN_PATH);
        remove(UCI_TEST_OUT_PATH);
        remove(UCI_TEST_ERR_PATH);
        return 0;
    }

    fputs(input, in);
    rewind(in);
    run_uci_loop(in, out, err);
    rewind(out);

    while(fgets(line, sizeof(line), out) != NULL){
        if(strncmp(line, "bestmove ", 9) == 0){
            size_t length = strcspn(line + 9, "\r\n");
            if(length > 0 && length < move_out_size){
                memcpy(move_out, line + 9, length);
                move_out[length] = '\0';
                found = 1;
            }
        }
    }

    fclose(in);
    fclose(out);
    fclose(err);
    remove(UCI_TEST_IN_PATH);
    remove(UCI_TEST_OUT_PATH);
    remove(UCI_TEST_ERR_PATH);
    return found;
}

#define TACTICAL_POSITION_CMD "position fen rnbqkbnr/ppp1p1pp/5p2/3p4/7P/5N2/PPPPPPP1/RNBQKB1R w KQkq - 0 3 " \
    "moves b1c3 e7e5 d2d4 f8b4 d4e5 b8c6 c1f4 d5d4\n"

/* Longer than the UCI input buffer. */
#define UCI_TEST_OVERSIZED_LENGTH 20000

/* Depth 1 must finish even when the time budget is smaller than a depth-1 search. */
int test_uci_go_tiny_increment_returns_move(void){
    char move[16];

    return uci_bestmove(TACTICAL_POSITION_CMD "go wtime 5000 btime 5000 winc 100 binc 100\nquit\n",
        move, sizeof(move)) && strcmp(move, "0000") != 0;
}

int test_uci_go_movetime_one_returns_move(void){
    char move[16];

    return uci_bestmove(TACTICAL_POSITION_CMD "go movetime 1\nquit\n",
        move, sizeof(move)) && strcmp(move, "0000") != 0;
}

int test_uci_go_clock_uses_side_to_move(void){
    char move[16];

    /* White to move has almost no time left while black has plenty. */
    return uci_bestmove(TACTICAL_POSITION_CMD "go wtime 5 btime 60000 winc 0 binc 0\nquit\n",
        move, sizeof(move)) && strcmp(move, "0000") != 0;
}

/* A position command longer than the old 1024-character buffer must be applied in full. */
int test_uci_long_position_command_is_fully_applied(void){
    static char input[4096];
    char move[16];
    size_t length = (size_t)snprintf(input, sizeof(input), "position startpos moves");

    for(int i = 0; i < 100; i++){
        length += (size_t)snprintf(input + length, sizeof(input) - length,
            " g1f3 g8f6 f3g1 f6g8");
    }
    snprintf(input + length, sizeof(input) - length, " e2e4\ngo depth 2\nquit\n");

    /* Black is to move, so the move must start from rank 7 or 8. */
    return uci_bestmove(input, move, sizeof(move))
        && (move[1] == '7' || move[1] == '8');
}

int test_uci_oversized_line_is_ignored(void){
    static char input[UCI_TEST_OVERSIZED_LENGTH + 64];
    char move[16];
    size_t length = (size_t)snprintf(input, sizeof(input), "position startpos moves e2e4\n");

    memset(input + length, 'x', UCI_TEST_OVERSIZED_LENGTH);
    length += UCI_TEST_OVERSIZED_LENGTH;
    snprintf(input + length, sizeof(input) - length, "\ngo depth 2\nquit\n");

    /* The junk line is dropped without breaking the position that came before it. */
    return uci_bestmove(input, move, sizeof(move))
        && (move[1] == '7' || move[1] == '8');
}

int uci_tests(void){
    int (*test_cases[14])(void) = {
        test_move_to_uci_basic,
        test_move_to_uci_promotion,
        test_parse_uci_move_basic,
        test_parse_uci_move_castling,
        test_parse_uci_move_en_passant,
        test_parse_uci_move_promotion,
        test_parse_uci_move_rejects_invalid,
        test_algebraic_to_uci,
        test_uci_to_simplified_algebraic,
        test_uci_go_tiny_increment_returns_move,
        test_uci_go_movetime_one_returns_move,
        test_uci_go_clock_uses_side_to_move,
        test_uci_long_position_command_is_fully_applied,
        test_uci_oversized_line_is_ignored
    };
    char* test_case_names[14] = {
        "test_move_to_uci_basic",
        "test_move_to_uci_promotion",
        "test_parse_uci_move_basic",
        "test_parse_uci_move_castling",
        "test_parse_uci_move_en_passant",
        "test_parse_uci_move_promotion",
        "test_parse_uci_move_rejects_invalid",
        "test_algebraic_to_uci",
        "test_uci_to_simplified_algebraic",
        "test_uci_go_tiny_increment_returns_move",
        "test_uci_go_movetime_one_returns_move",
        "test_uci_go_clock_uses_side_to_move",
        "test_uci_long_position_command_is_fully_applied",
        "test_uci_oversized_line_is_ignored"
    };

    return run_tests(test_cases, test_case_names, 14);
}
