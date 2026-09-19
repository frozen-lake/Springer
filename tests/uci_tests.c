#include "uci_tests.h"

#include <string.h>

#include "tests.h"
#include "../src/board.h"
#include "../src/game.h"
#include "../src/move.h"
#include "../src/uci_adapter.h"

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

int uci_tests(void){
    int (*test_cases[9])(void) = {
        test_move_to_uci_basic,
        test_move_to_uci_promotion,
        test_parse_uci_move_basic,
        test_parse_uci_move_castling,
        test_parse_uci_move_en_passant,
        test_parse_uci_move_promotion,
        test_parse_uci_move_rejects_invalid,
        test_algebraic_to_uci,
        test_uci_to_simplified_algebraic
    };
    char* test_case_names[9] = {
        "test_move_to_uci_basic",
        "test_move_to_uci_promotion",
        "test_parse_uci_move_basic",
        "test_parse_uci_move_castling",
        "test_parse_uci_move_en_passant",
        "test_parse_uci_move_promotion",
        "test_parse_uci_move_rejects_invalid",
        "test_algebraic_to_uci",
        "test_uci_to_simplified_algebraic"
    };

    return run_tests(test_cases, test_case_names, 9);
}
