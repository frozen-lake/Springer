#include "quiescence_tests.h"
#include "tests.h"
#include "../src/game.h"
#include "../src/search.h"

int test_quiesce_quiet_position(void){
	Game* game = create_game();
	if(game == NULL || !load_fen(game, "4k3/8/8/8/8/8/8/4KQ2 w - - 0 1")){
		destroy_game(game);
		return 0;
	}

	SearchState search_state = (SearchState){0};
	int score = quiesce(&search_state, game, -INF, INF, 0, 0);
	int success = score == evaluate(game);

	destroy_game(game);
	return success;
}

int test_quiesce_finds_winning_capture(void){
	Game* game = create_game();
	if(game == NULL || !load_fen(game, "4k3/8/8/8/8/8/3q4/3RK3 w - - 0 1")){
		destroy_game(game);
		return 0;
	}

	SearchState search_state = (SearchState){0};
	int score = quiesce(&search_state, game, -INF, INF, 0, 0);
	int success = score == 500;

	destroy_game(game);
	return success;
}

int test_quiesce_finds_capture_with_promotion(void){
	Game* game = create_game();
	if(game == NULL || !load_fen(game, "3r3k/4P3/8/8/8/8/8/4K3 w - - 0 1")){
		destroy_game(game);
		return 0;
	}

	SearchState search_state = (SearchState){0};
	int score = quiesce(&search_state, game, -INF, INF, 0, 0);
	int success = (score == 900);

	destroy_game(game);
	return success;
}


int test_quiesce_check(void){
	Game* game = create_game();
	if(game == NULL || !load_fen(game, "k7/8/8/8/8/8/4r3/4K3 w - - 0 1")){
		destroy_game(game);
		return 0;
	}

	SearchState search_state = (SearchState){0};
	int score = quiesce(&search_state, game, -INF, INF, 0, 0);
	int success = score == 0;

	destroy_game(game);
	return success;
}

int test_quiesce_finds_immediate_quiet_checkmate(void){
	Game* game = create_game();
	if(game == NULL || !load_fen(game, "7k/8/6K1/8/8/8/8/7Q w - - 0 1")){
		destroy_game(game);
		return 0;
	}

	SearchState search_state = (SearchState){0};
	int score = quiesce(&search_state, game, -INF, INF, 0, 0);
	int success = score == MATE_SCORE - 1;

	destroy_game(game);
	return success;
}

int test_quiesce_checkmate(void){
	Game* game = create_game();
	if(game == NULL || !load_fen(game, "7k/6Q1/6K1/8/8/8/8/8 b - - 0 1")){
		destroy_game(game);
		return 0;
	}

	SearchState search_state = (SearchState){0};
	int score = quiesce(&search_state, game, -INF, INF, 0, 0);
	int success = score == -MATE_SCORE;

	destroy_game(game);
	return success;
}

int test_quiesce_stalemate(void){
	Game* game = create_game();
	if(game == NULL || !load_fen(game, "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")){
		destroy_game(game);
		return 0;
	}

	SearchState search_state = (SearchState){0};
	int score = quiesce(&search_state, game, -INF, INF, 0, 0);
	int success = score == 0;

	destroy_game(game);
	return success;
}

int quiescence_tests(void){
	int (*test_cases[7])(void) = {
		test_quiesce_quiet_position,
        test_quiesce_finds_winning_capture,
        test_quiesce_finds_capture_with_promotion,
        test_quiesce_check,
		test_quiesce_finds_immediate_quiet_checkmate,
		test_quiesce_checkmate,
		test_quiesce_stalemate
	};
	char* test_case_names[7] = {
		"test_quiesce_quiet_position",
        "test_quiesce_finds_winning_capture",
        "test_quiesce_finds_capture_with_promotion",
        "test_quiesce_check",
		"test_quiesce_finds_immediate_quiet_checkmate",
		"test_quiesce_checkmate",
		"test_quiesce_stalemate"
	};
	return run_tests(test_cases, test_case_names, 7);
}
