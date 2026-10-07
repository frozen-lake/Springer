#include "perft_tests.h"
#include "tests.h"
#include "../src/move_gen.h"
#include <string.h>

static uint64_t perft_recursive(Game* game, int depth, int* state_error){
	if(game == NULL || depth < 0){
		return 0;
	}
	if(depth == 0){
		return 1;
	}

	MoveList moves;
	generate_all_moves(&moves, game, game->state.side_to_move);
	filter_legal_moves(&moves, game);

	uint64_t nodes = 0;
	for(int i = 0; i < moves.size; i++){
		BoardState before = game->state;
		UndoInfo undo;
		make_move_on_state(&game->state, moves.moves[i], &undo);
		nodes += perft_recursive(game, depth - 1, state_error);
		unmake_move_on_state(&game->state, moves.moves[i], &undo);
		if(memcmp(&before, &game->state, sizeof(before)) != 0){
			*state_error = 1;
		}
	}

	return nodes;
}

uint64_t perft(Game* game, int depth){
	int state_error = 0;
	uint64_t nodes = perft_recursive(game, depth, &state_error);
	return state_error ? 0 : nodes;
}

int test_perft_starting_position(void){
	Game* game = create_game();
	if(game == NULL){
		return 0;
	}

	initialize_game(game);
	uint64_t depth_one = perft(game, 1);
	uint64_t depth_two = perft(game, 2);
	uint64_t depth_three = perft(game, 3);
	uint64_t depth_four = perft(game, 4);

	int success = depth_one == 20;
	success = success && depth_two == 400;
	success = success && depth_three == 8902;
	success = success && depth_four == 197281;

	destroy_game(game);
	return success;
}

static int test_perft_position(const char* fen,
	const uint64_t expected[3]){
	Game* game = create_game();
	if(game == NULL || !load_fen(game, (char*)fen)){
		destroy_game(game);
		return 0;
	}

	uint64_t depth_one = perft(game, 1);
	uint64_t depth_two = perft(game, 2);
	uint64_t depth_three = perft(game, 3);
	int success = depth_one == expected[0]
		&& depth_two == expected[1]
		&& depth_three == expected[2];
	destroy_game(game);
	return success;
}

int test_perft_kiwipete(void){
	static const uint64_t expected[3] = {48, 2039, 97862};
	return test_perft_position(
		"r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
		expected);
}

int test_perft_promotions_and_en_passant(void){
	static const uint64_t expected[3] = {14, 191, 2812};
	return test_perft_position(
		"8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
		expected);
}

int test_perft_pins_and_promotions(void){
	static const uint64_t expected[3] = {6, 264, 9467};
	return test_perft_position(
		"r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
		expected);
}

int test_perft_promotion_capture_position(void){
	static const uint64_t expected[3] = {44, 1486, 62379};
	return test_perft_position(
		"rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
		expected);
}

int test_perft_middlegame_position(void){
	static const uint64_t expected[3] = {46, 2079, 89890};
	return test_perft_position(
		"r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
		expected);
}

static int test_perft_depth(const char* fen, int depth, uint64_t expected){
	Game* game = create_game();
	if(game == NULL || !load_fen(game, (char*)fen)){
		destroy_game(game);
		return 0;
	}

	int success = perft(game, depth) == expected;
	destroy_game(game);
	return success;
}

int test_perft_deep_starting_position(void){
	return test_perft_depth(
		"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 5, 4865609);
}

int test_perft_deep_kiwipete(void){
	return test_perft_depth(
		"r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1", 4, 4085603);
}

int test_perft_deep_promotions_and_en_passant(void){
	return test_perft_depth("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1", 5, 674624);
}

int test_perft_deep_pins_and_promotions(void){
	return test_perft_depth(
		"r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1", 4, 422333);
}

int perft_tests(void){
	int (*test_cases[10])(void) = {
		test_perft_starting_position,
		test_perft_kiwipete,
		test_perft_promotions_and_en_passant,
		test_perft_pins_and_promotions,
		test_perft_promotion_capture_position,
		test_perft_middlegame_position,
		test_perft_deep_starting_position,
		test_perft_deep_kiwipete,
		test_perft_deep_promotions_and_en_passant,
		test_perft_deep_pins_and_promotions
	};
	char* test_case_names[10] = {
		"test_perft_starting_position",
		"test_perft_kiwipete",
		"test_perft_promotions_and_en_passant",
		"test_perft_pins_and_promotions",
		"test_perft_promotion_capture_position",
		"test_perft_middlegame_position",
		"test_perft_deep_starting_position",
		"test_perft_deep_kiwipete",
		"test_perft_deep_promotions_and_en_passant",
		"test_perft_deep_pins_and_promotions"
	};
	return run_tests(test_cases, test_case_names, 10);
}
