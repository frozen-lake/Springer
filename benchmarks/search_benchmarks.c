#include "search_benchmarks.h"

#include <stdio.h>
#include <string.h>
#include <time.h>

#include "../src/game.h"
#include "../src/move.h"
#include "../src/move_gen.h"
#include "../src/search.h"
#include "../src/transposition_table.h"

#define OPENING_BENCHMARK_DEPTH 4
#define KIWIPETE_BENCHMARK_DEPTH 3
#define TACTICAL_BENCHMARK_DEPTH 3
#define ENDGAME_BENCHMARK_DEPTH 5
#define BENCHMARK_RUNS 5
#define BENCHMARK_RESULTS_PATH "benchmarks/search_benchmark_results.jsonl"

static int append_benchmark_result(const char* benchmark_name,
	int depth,
	const char* best_move,
	int root_moves,
	int nodes_per_run,
	int alpha_beta_nodes_per_run,
	int quiescence_nodes_per_run,
	int tt_probes_per_run,
	int tt_hits_per_run,
	int tt_exact_cutoffs_per_run,
	int tt_bound_cutoffs_per_run,
	int runs,
	double elapsed_seconds,
	double nodes_per_second,
	int state_preserved,
	int success){
	FILE* results_file = fopen(BENCHMARK_RESULTS_PATH, "a");
	if(results_file == NULL){
		fprintf(stderr, "unable to open %s\n", BENCHMARK_RESULTS_PATH);
		return 0;
	}

	time_t now = time(NULL);
	struct tm* utc_time = gmtime(&now);
	char timestamp[21] = "";
	if(utc_time == NULL || strftime(timestamp, sizeof(timestamp),
		"%Y-%m-%dT%H:%M:%SZ", utc_time) == 0){
		fprintf(stderr, "unable to create UTC timestamp\n");
		fclose(results_file);
		return 0;
	}

	int written = fprintf(results_file,
		"{\"timestamp_utc\":\"%s\",\"benchmark\":\"%s\",\"depth\":%d,\"best_move\":\"%s\",\"root_moves\":%d,\"nodes_per_run\":%d,\"alpha_beta_nodes_per_run\":%d,\"quiescence_nodes_per_run\":%d,\"tt_probes_per_run\":%d,\"tt_hits_per_run\":%d,\"tt_exact_cutoffs_per_run\":%d,\"tt_bound_cutoffs_per_run\":%d,\"runs\":%d,\"elapsed_seconds\":%.6f,\"nodes_per_second\":%.0f,\"state_preserved\":%s,\"success\":%s}\n",
		timestamp,
		benchmark_name,
		depth,
		best_move,
		root_moves,
		nodes_per_run,
		alpha_beta_nodes_per_run,
		quiescence_nodes_per_run,
		tt_probes_per_run,
		tt_hits_per_run,
		tt_exact_cutoffs_per_run,
		tt_bound_cutoffs_per_run,
		runs,
		elapsed_seconds,
		nodes_per_second,
		state_preserved ? "true" : "false",
		success ? "true" : "false");
	if(written < 0 || fclose(results_file) != 0){
		fprintf(stderr, "unable to write %s\n", BENCHMARK_RESULTS_PATH);
		return 0;
	}

	return 1;
}

static int benchmark_position(const char* benchmark_name, const char* fen, int depth){
	Game* game = create_game();
	if(game == NULL || !load_fen(game, (char*)fen)){
		fprintf(stderr, "%s: unable to load position\n", benchmark_name);
		destroy_game(game);
		return 0;
	}

	MoveList root_moves;
	generate_all_moves(&root_moves, game, game->state.side_to_move);
	filter_legal_moves(&root_moves, game);
	int root_move_count = root_moves.size;

	TranspositionTable table = (TranspositionTable){0};
	tt_init(&table);

	SearchState search_state = (SearchState){0};
	int stop = 0;
	if(!initialize_searchstate(&search_state, &table, depth, &stop)){
		fprintf(stderr, "%s: unable to initialize search state\n", benchmark_name);
		tt_free(&table);
		destroy_game(game);
		return 0;
	}

	BoardState state_before = game->state;
	clock_t start = clock();
	Move best_move = 0;
	int total_nodes = 0;
	int total_alpha_beta_nodes = 0;
	int total_quiescence_nodes = 0;
	int total_tt_probes = 0;
	int total_tt_hits = 0;
	int total_tt_exact_cutoffs = 0;
	int total_tt_bound_cutoffs = 0;
	int state_preserved = 1;
	for(int run = 0; run < BENCHMARK_RUNS; run++){
		tt_clear(&table);
		best_move = search_root(game, &search_state, depth);
		total_nodes += search_state.nodes;
		total_alpha_beta_nodes += search_state.alpha_beta_nodes;
		total_quiescence_nodes += search_state.quiescence_nodes;
		total_tt_probes += search_state.tt_probes;
		total_tt_hits += search_state.tt_hits;
		total_tt_exact_cutoffs += search_state.tt_exact_cutoffs;
		total_tt_bound_cutoffs += search_state.tt_bound_cutoffs;
		state_preserved = state_preserved
			&& memcmp(&state_before, &game->state, sizeof(state_before)) == 0;
	}
	clock_t finish = clock();
	double elapsed_seconds = (double)(finish - start) / CLOCKS_PER_SEC;
	double nodes_per_second = elapsed_seconds > 0.0
		? total_nodes / elapsed_seconds
		: 0.0;
	int nodes_per_run = total_nodes / BENCHMARK_RUNS;
	int alpha_beta_nodes_per_run = total_alpha_beta_nodes / BENCHMARK_RUNS;
	int quiescence_nodes_per_run = total_quiescence_nodes / BENCHMARK_RUNS;
	int tt_probes_per_run = total_tt_probes / BENCHMARK_RUNS;
	int tt_hits_per_run = total_tt_hits / BENCHMARK_RUNS;
	int tt_exact_cutoffs_per_run = total_tt_exact_cutoffs / BENCHMARK_RUNS;
	int tt_bound_cutoffs_per_run = total_tt_bound_cutoffs / BENCHMARK_RUNS;
	char move_text[16] = "(none)";
	if(best_move != 0){
		move_to_simplified_algebraic(best_move, move_text, sizeof(move_text));
	}

	printf("%s: depth=%d root_moves=%d move=%s nodes/run=%d runs=%d time=%.3fs nps=%.0f state=%s\n",
		benchmark_name,
		depth,
		root_move_count,
		move_text,
		nodes_per_run,
		BENCHMARK_RUNS,
		elapsed_seconds,
		nodes_per_second,
		state_preserved ? "preserved" : "changed");
	int success = root_move_count > 0 && best_move != 0 && state_preserved;
	int recorded = append_benchmark_result(benchmark_name,
		depth,
		move_text,
		root_move_count,
		nodes_per_run,
		alpha_beta_nodes_per_run,
		quiescence_nodes_per_run,
		tt_probes_per_run,
		tt_hits_per_run,
		tt_exact_cutoffs_per_run,
		tt_bound_cutoffs_per_run,
		BENCHMARK_RUNS,
		elapsed_seconds,
		nodes_per_second,
		state_preserved,
		success);

	destroy_searchstate(&search_state);
	tt_free(&table);
	destroy_game(game);
	return success && recorded;
}

int benchmark_opening_position(void){
	Game* game = create_game();
	if(game == NULL){
		fprintf(stderr, "opening: unable to create game\n");
		return 0;
	}
	initialize_game(game);
	MoveList root_moves;
	generate_all_moves(&root_moves, game, game->state.side_to_move);
	filter_legal_moves(&root_moves, game);
	int root_move_count = root_moves.size;

	TranspositionTable table = (TranspositionTable){0};
	tt_init(&table);

	SearchState search_state = (SearchState){0};
	int stop = 0;
	if(!initialize_searchstate(&search_state, &table, OPENING_BENCHMARK_DEPTH, &stop)){
		fprintf(stderr, "opening: unable to initialize search state\n");
		tt_free(&table);
		destroy_game(game);
		return 0;
	}

	BoardState state_before = game->state;
	clock_t start = clock();
	Move best_move = 0;
	int total_nodes = 0;
	int total_alpha_beta_nodes = 0;
	int total_quiescence_nodes = 0;
	int total_tt_probes = 0;
	int total_tt_hits = 0;
	int total_tt_exact_cutoffs = 0;
	int total_tt_bound_cutoffs = 0;
	int state_preserved = 1;
	for(int run = 0; run < BENCHMARK_RUNS; run++){
		tt_clear(&table);
		best_move = search_root(game, &search_state, OPENING_BENCHMARK_DEPTH);
		total_nodes += search_state.nodes;
		total_alpha_beta_nodes += search_state.alpha_beta_nodes;
		total_quiescence_nodes += search_state.quiescence_nodes;
		total_tt_probes += search_state.tt_probes;
		total_tt_hits += search_state.tt_hits;
		total_tt_exact_cutoffs += search_state.tt_exact_cutoffs;
		total_tt_bound_cutoffs += search_state.tt_bound_cutoffs;
		state_preserved = state_preserved
			&& memcmp(&state_before, &game->state, sizeof(state_before)) == 0;
	}
	clock_t finish = clock();
	double elapsed_seconds = (double)(finish - start) / CLOCKS_PER_SEC;
	double nodes_per_second = elapsed_seconds > 0.0
		? total_nodes / elapsed_seconds
		: 0.0;
	int nodes_per_run = total_nodes / BENCHMARK_RUNS;
	int alpha_beta_nodes_per_run = total_alpha_beta_nodes / BENCHMARK_RUNS;
	int quiescence_nodes_per_run = total_quiescence_nodes / BENCHMARK_RUNS;
	int tt_probes_per_run = total_tt_probes / BENCHMARK_RUNS;
	int tt_hits_per_run = total_tt_hits / BENCHMARK_RUNS;
	int tt_exact_cutoffs_per_run = total_tt_exact_cutoffs / BENCHMARK_RUNS;
	int tt_bound_cutoffs_per_run = total_tt_bound_cutoffs / BENCHMARK_RUNS;
	char move_text[16] = "(none)";
	if(best_move != 0){
		move_to_simplified_algebraic(best_move, move_text, sizeof(move_text));
	}

	printf("opening: depth=%d root_moves=%d move=%s nodes/run=%d runs=%d time=%.3fs nps=%.0f state=%s\n",
		OPENING_BENCHMARK_DEPTH,
		root_move_count,
		move_text,
		nodes_per_run,
		BENCHMARK_RUNS,
		elapsed_seconds,
		nodes_per_second,
		state_preserved ? "preserved" : "changed");
	int success = root_move_count > 0 && best_move != 0 && state_preserved;
	int recorded = append_benchmark_result("opening",
		OPENING_BENCHMARK_DEPTH,
		move_text,
		root_move_count,
		nodes_per_run,
		alpha_beta_nodes_per_run,
		quiescence_nodes_per_run,
		tt_probes_per_run,
		tt_hits_per_run,
		tt_exact_cutoffs_per_run,
		tt_bound_cutoffs_per_run,
		BENCHMARK_RUNS,
		elapsed_seconds,
		nodes_per_second,
		state_preserved,
		success);

	destroy_searchstate(&search_state);
	tt_free(&table);
	destroy_game(game);
	return success && recorded;
}

int benchmark_kiwipete_position(void){
	return benchmark_position("kiwipete",
		"r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
		KIWIPETE_BENCHMARK_DEPTH);
}

int benchmark_tactical_position(void){
	return benchmark_position("tactical",
		"4k3/8/8/8/8/8/3q4/3RK3 w - - 0 1",
		TACTICAL_BENCHMARK_DEPTH);
}

int benchmark_endgame_position(void){
	return benchmark_position("endgame",
		"8/8/8/3k4/8/3K4/4P3/8 w - - 0 1",
		ENDGAME_BENCHMARK_DEPTH);
}

int main(void){
	int success = benchmark_opening_position();
	success = benchmark_kiwipete_position() && success;
	success = benchmark_tactical_position() && success;
	success = benchmark_endgame_position() && success;
	return success ? 0 : 1;
}
