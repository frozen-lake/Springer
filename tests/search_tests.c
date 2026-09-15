#include <stdio.h>
#include <string.h>
#include "tests.h"
#include "../src/game.h"
#include "../src/search.h"


int test_initialize_searchstate_null(){
    int stop = 0;
    return initialize_searchstate(NULL, NULL, 4, &stop) == 0;
}

int test_initialize_searchstate_defaults_and_clamp(){
    SearchState state = (SearchState){0};
    int stop = 0;

    int success = initialize_searchstate(&state, NULL, 0, &stop);
    success = success && (state.tt == NULL);
    success = success && (state.nodes == 0);
    success = success && (state.root_depth == 0);
    success = success && (state.max_depth == MAX_SEARCH_PLY);
    success = success && (state.stop == &stop);
    success = success && (state.pv_lengths[0] == 0);
    success = success && (state.pv_table[0][0] == 0);

    success = success && initialize_searchstate(&state, NULL, MAX_SEARCH_PLY + 10, &stop);
    success = success && (state.max_depth == MAX_SEARCH_PLY);

    success = success && initialize_searchstate(&state, NULL, 12, &stop);
    success = success && (state.max_depth == 12);

    return success;
}

int test_reset_searchstate(){
    SearchState state = (SearchState){0};
    int stop = 0;

    if(!initialize_searchstate(&state, NULL, 8, &stop)){
        return 0;
    }

    state.nodes = 123;
    state.root_depth = 5;
    state.pv_lengths[0] = 3;
    state.pv_table[0][0] = 1;

    reset_searchstate(&state);

    int success = (state.nodes == 0);
    success = success && (state.root_depth == 0);
    success = success && (state.pv_lengths[0] == 0);
    success = success && (state.pv_table[0][0] == 0);
    success = success && (state.max_depth == 8);

    return success;
}

int test_stop_helpers(){
    SearchState state = (SearchState){0};
    int stop = 0;

    if(!initialize_searchstate(&state, NULL, 8, &stop)){
        return 0;
    }

    int success = !is_search_stop_requested(&state);

    request_search_stop(&state);
    success = success && (stop == 1);
    success = success && is_search_stop_requested(&state);

    clear_search_stop(&state);
    success = success && (stop == 0);
    success = success && !is_search_stop_requested(&state);

    request_search_stop(NULL);
    clear_search_stop(NULL);
    success = success && !is_search_stop_requested(NULL);

    return success;
}


int test_evaluation(){
    Game* game = create_game();

    char* fen = "3rk3/2bp4/8/7N/5p2/5P2/52P/R3K3 w K - 0 2";
    int success = load_fen(game, fen);

    int eval = evaluate(game);

    success = success && (eval == 0);

    destroy_game(game);
    return success;
}

int test_search_finds_mate_in_one(){
    Game* game = create_game();
    if(game == NULL || !load_fen(game, "7k/8/5KQ1/8/8/8/8/8 w - - 0 1")){
        destroy_game(game);
        return 0;
    }

    SearchState search_state = (SearchState){0};
    int stop = 0;
    int success = initialize_searchstate(&search_state, NULL, 2, &stop);
    Move best_move = search_best_move(game, &search_state);

    success = success && get_move_src(best_move) == G6;
    success = success && get_move_dest(best_move) == G7;
    success = success && is_legal_player_move(game, best_move);

    destroy_searchstate(&search_state);
    destroy_game(game);
    return success;
}

int test_search_checkmate_score(){
    Game* game = create_game();
    if(game == NULL || !load_fen(game, "7k/6Q1/6K1/8/8/8/8/8 b - - 0 1")){
        destroy_game(game);
        return 0;
    }

    SearchState search_state = (SearchState){0};
    int stop = 0;
    int success = initialize_searchstate(&search_state, NULL, 2, &stop);
    int score = alpha_beta(&search_state, game, -INF, INF, 1, 0);
    success = success && score == -MATE_SCORE;

    destroy_searchstate(&search_state);
    destroy_game(game);
    return success;
}

int test_search_stalemate_score(){
    Game* game = create_game();
    if(game == NULL || !load_fen(game, "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")){
        destroy_game(game);
        return 0;
    }

    SearchState search_state = (SearchState){0};
    int stop = 0;
    int success = initialize_searchstate(&search_state, NULL, 2, &stop);
    int score = alpha_beta(&search_state, game, -INF, INF, 1, 0);
    success = success && score == 0;

    destroy_searchstate(&search_state);
    destroy_game(game);
    return success;
}

int test_stopped_search_preserves_state(){
    Game* game = create_game();
    if(game == NULL){
        return 0;
    }
    initialize_game(game);

    BoardState state_before = game->state;
    int stop = 1;
    SearchState search_state = (SearchState){0};
    int success = initialize_searchstate(&search_state, NULL, 3, &stop);
    Move best_move = search_best_move(game, &search_state);

    success = success && best_move != 0;
    success = success && is_legal_player_move(game, best_move);
    success = success && memcmp(&state_before, &game->state, sizeof(state_before)) == 0;

    destroy_searchstate(&search_state);
    destroy_game(game);
    return success;
}

int test_tt_mate_score_normalization(){
    int positive_mate = MATE_SCORE - 7;
    int negative_mate = -MATE_SCORE + 7;
    int positive_stored = score_to_tt(positive_mate, 5);
    int negative_stored = score_to_tt(negative_mate, 5);

    int success = positive_stored == MATE_SCORE - 2;
    success = success && negative_stored == -MATE_SCORE + 2;
    success = success && score_from_tt(positive_stored, 5) == positive_mate;
    success = success && score_from_tt(negative_stored, 5) == negative_mate;
    success = success && score_from_tt(positive_stored, 2) == MATE_SCORE - 4;
    success = success && score_from_tt(negative_stored, 2) == -MATE_SCORE + 4;
    success = success && score_to_tt(300, 5) == 300;
    success = success && score_from_tt(-300, 5) == -300;
    return success;
}

int test_tt_exact_entry_avoids_search(){
    Game* game = create_game();
    if(game == NULL){
        return 0;
    }
    initialize_game(game);

    TranspositionTable table = (TranspositionTable){0};
    tt_init(&table);
    SearchState search_state = (SearchState){0};
    int stop = 0;
    int success = initialize_searchstate(&search_state, &table, 3, &stop);
    BoardState state_before = game->state;
    tt_add(&table, game->state.zobrist_hash, 0, score_to_tt(321, 0), 3, TT_EXACT);
    int score = alpha_beta(&search_state, game, -INF, INF, 3, 0);

    success = success && score == 321;
    success = success && search_state.nodes == 1;
    success = success && memcmp(&state_before, &game->state, sizeof(state_before)) == 0;

    destroy_searchstate(&search_state);
    tt_free(&table);
    destroy_game(game);
    return success;
}

int test_tt_reuses_completed_search(){
    Game* game = create_game();
    if(game == NULL || !load_fen(game, "4k3/8/8/8/8/8/8/4KQ2 w - - 0 1")){
        destroy_game(game);
        return 0;
    }

    TranspositionTable table = (TranspositionTable){0};
    tt_init(&table);
    SearchState search_state = (SearchState){0};
    int stop = 0;
    int success = initialize_searchstate(&search_state, &table, 2, &stop);
    BoardState state_before = game->state;
    int first_score = alpha_beta(&search_state, game, -INF, INF, 2, 0);
    int first_nodes = search_state.nodes;
    search_state.nodes = 0;
    int second_score = alpha_beta(&search_state, game, -INF, INF, 2, 0);

    success = success && first_nodes > 1;
    success = success && second_score == first_score;
    success = success && search_state.nodes == 1;
    success = success && memcmp(&state_before, &game->state, sizeof(state_before)) == 0;

    destroy_searchstate(&search_state);
    tt_free(&table);
    destroy_game(game);
    return success;
}

int search_tests(){
    int num_tests = 12;

	int (*test_cases[num_tests])();
	char* test_case_names[num_tests];

    test_cases[0] = test_initialize_searchstate_null;
    test_cases[1] = test_initialize_searchstate_defaults_and_clamp;
    test_cases[2] = test_reset_searchstate;
    test_cases[3] = test_stop_helpers;
    test_cases[4] = test_evaluation;
    test_cases[5] = test_search_finds_mate_in_one;
    test_cases[6] = test_search_checkmate_score;
    test_cases[7] = test_search_stalemate_score;
    test_cases[8] = test_stopped_search_preserves_state;
    test_cases[9] = test_tt_mate_score_normalization;
    test_cases[10] = test_tt_exact_entry_avoids_search;
    test_cases[11] = test_tt_reuses_completed_search;

    test_case_names[0] = "test_initialize_searchstate_null";
    test_case_names[1] = "test_initialize_searchstate_defaults_and_clamp";
    test_case_names[2] = "test_reset_searchstate";
    test_case_names[3] = "test_stop_helpers";
    test_case_names[4] = "test_evaluation";
    test_case_names[5] = "test_search_finds_mate_in_one";
    test_case_names[6] = "test_search_checkmate_score";
    test_case_names[7] = "test_search_stalemate_score";
    test_case_names[8] = "test_stopped_search_preserves_state";
	test_case_names[9] = "test_tt_mate_score_normalization";
	test_case_names[10] = "test_tt_exact_entry_avoids_search";
	test_case_names[11] = "test_tt_reuses_completed_search";

    return run_tests(test_cases, test_case_names, num_tests);
}