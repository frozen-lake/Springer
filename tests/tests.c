#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "attack_and_move_tests.h"
#include "move_gen_tests.h"
#include "endgame_tests.h"
#include "search_tests.h"
#include "transposition_table_tests.h"
#include "perft_tests.h"
#include "quiescence_tests.h"
#include "uci_tests.h"
#include "../src/game.h"
#include "../src/move.h"
#include "../src/attack_data.h"
#include "../src/move_gen.h"

int test_game_init(){
	Game* game = create_game();
	initialize_game(game);

	int success = get_knight_attacks(G1) & U64_MASK(F3);

	destroy_game(game);
	return success;
}

int test_load_fen(){
	/* Nominal case */
	Game* game = create_game();
	
	char* fen = "4k3/8/8/1n2p3/4P1Pp/8/8/3BK3 b - g3 0 1";
	int success = load_fen(game, fen);
	success = !game->state.side_to_move;
	
	if(!success){
		destroy_game(game);
		return 0;
	}

	success = success && (game->state.pieces[Pawn] == (U64_MASK(E4) | U64_MASK(E5) | U64_MASK(G4) | U64_MASK(H4)));
	success = success && (get_knight_attacks(B5) == (uint64_t) 0b101000010000000000000001000000001010000000000000000);
	success = success && (game->state.pieces[White] == 0b1010000000000000000000000011000);
	success = success && (game->state.pieces[Black] == 0b1000000000000000000000001001010000000000000000000000000000000);
	
	/* Bad FEN should return 0 */
	success = success && !load_fen(game, "4k3/8/8/1n3p3/4P1Pp/8/8/3BK3 b - g3 0 1");

	success = load_fen(game, "3rk2r/1p6/4p3/8/2N2p2/5P2/P3P1PP/R3K2R w KQk - 0 2");
	success = success && (game->state.castling_rights == 0b1101);

	destroy_game(game);
	return success;
}

int test_load_fen_invalid(){
	Game* game = create_game();

	char* valid_fen = "4k3/8/8/1n2p3/4P1Pp/8/8/3BK3 b - g3 0 1";
	if(!load_fen(game, valid_fen)){
		destroy_game(game);
		return 0;
	}

	uint64_t pieces_before[8];
	memcpy(pieces_before, game->state.pieces, sizeof(pieces_before));
	int side_before = game->state.side_to_move;
	int en_passant_before = game->state.en_passant;
	uint8_t castling_before = game->state.castling_rights;
	uint8_t halfmove_before = game->state.halfmove_clock;
	int8_t white_king_before = game->state.king_sq[White];
	int8_t black_king_before = game->state.king_sq[Black];
	uint64_t zobrist_before = game->state.zobrist_hash;

	int result = load_fen(game, "4k3/8/8/1n3p3/4P1Pp/8/8/3BK3 b - g3 0 1");

	int success = (result == 0);
	success = success && (memcmp(pieces_before, game->state.pieces, sizeof(pieces_before)) == 0);
	success = success && (side_before == game->state.side_to_move);
	success = success && (en_passant_before == game->state.en_passant);
	success = success && (castling_before == game->state.castling_rights);
	success = success && (halfmove_before == game->state.halfmove_clock);
	success = success && (white_king_before == game->state.king_sq[White]);
	success = success && (black_king_before == game->state.king_sq[Black]);
	success = success && (zobrist_before == game->state.zobrist_hash);

	destroy_game(game);
	return success;
}

int test_load_fen_rejects_kingless_by_default(){
	Game* game = create_game();
	if(game == NULL){
		return 0;
	}

	int success = !load_fen(game, "4r3/8/8/8/8/8/8/R3K2R w K - 0 1");

	destroy_game(game);
	return success;
}

int test_load_fen_ex_allows_kingless(){
	Game* game = create_game();
	if(game == NULL){
		return 0;
	}

	int success = load_fen_ex(game, "4r3/8/8/8/8/8/8/R3K2R w K - 0 1", FEN_ALLOW_KINGLESS);
	success = success && (game->state.side_to_move == White);
	success = success && (game->state.castling_rights == (1 << 2));

	destroy_game(game);
	return success;
}

int test_save_fen_initial_position(){
	Game* game = create_game();
	char fen[128];
	const char* expected = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

	if(game == NULL){
		return 0;
	}

	initialize_game(game);
	int success = save_fen(game, fen, sizeof(fen));
	success = success && (strcmp(fen, expected) == 0);

	destroy_game(game);
	return success;
}

int test_save_fen_round_trip_state(){
	Game* game = create_game();
	Game* game2 = create_game();
	char fen[128];
	int success;

	if(game == NULL || game2 == NULL){
		destroy_game(game);
		destroy_game(game2);
		return 0;
	}

	success = load_fen(game, "4k3/8/8/1n2p3/4P1Pp/8/8/3BK3 b - g3 7 23");
	success = success && save_fen(game, fen, sizeof(fen));
	success = success && load_fen(game2, fen);

	success = success && (memcmp(game->state.pieces, game2->state.pieces, sizeof(game->state.pieces)) == 0);
	success = success && (game->state.side_to_move == game2->state.side_to_move);
	success = success && (game->state.castling_rights == game2->state.castling_rights);
	success = success && (game->state.en_passant == game2->state.en_passant);
	success = success && (game->state.halfmove_clock == game2->state.halfmove_clock);

	destroy_game(game);
	destroy_game(game2);
	return success;
}

int test_make_move(){
	Game* game = create_game();
	
	char* fen = "4k3/8/8/1n2p3/4P1Pp/2P5/8/3BK3 b - g3 0 1";
	int success = load_fen(game, fen);

	Move move = B5 | (C3 << 6) | (Knight << 12) | (Pawn << 15); // 
	make_move(game, move);

	/* Piece is off of source square */
	success = success && ((game->state.pieces[Knight] & U64_MASK(B5)) == 0);
	success = success && ((game->state.pieces[Black] & U64_MASK(B5)) == 0);

	/* Piece is on destination square */
	success = success && (game->state.pieces[Knight] & U64_MASK(C3));
	success = success && (game->state.pieces[Black] & U64_MASK(C3));

	/* Captured piece is gone */
	success = success && ((game->state.pieces[Pawn] & U64_MASK(C3)) == 0);
	success = success && ((game->state.pieces[White] & U64_MASK(C3)) == 0);

	destroy_game(game);
	return success;
}

int test_zobrist_hash_after_move(){
	Game* game = create_game();
	initialize_game(game);

	uint64_t hash_before = game->state.zobrist_hash;
	Move e4 = encode_move(E2, E4, &game->state);
	make_move(game, e4);
	uint64_t hash_after = game->state.zobrist_hash;

	destroy_game(game);
	return hash_after != hash_before;
}

int test_unmake_move_round_trip(){
	Game* game = create_game();
	initialize_game(game);

	uint64_t pieces_before[8];
	memcpy(pieces_before, game->state.pieces, sizeof(pieces_before));
	uint64_t hash_before = game->state.zobrist_hash;
	int side_before = game->state.side_to_move;
	int en_passant_before = game->state.en_passant;
	uint8_t castling_before = game->state.castling_rights;

	Move e4 = encode_move(E2, E4, &game->state);
	make_move(game, e4);
	unmake_move(game, e4);

	int success = memcmp(pieces_before, game->state.pieces, sizeof(pieces_before)) == 0;
	success = success && (hash_before == game->state.zobrist_hash);
	success = success && (side_before == game->state.side_to_move);
	success = success && (en_passant_before == game->state.en_passant);
	success = success && (castling_before == game->state.castling_rights);

	destroy_game(game);
	return success;
}

int test_promotion_round_trip(){
	Game* game = create_game();
	char* fen = "4nk2/3P4/8/8/8/8/8/4K3 w - - 0 1";
	if(!load_fen(game, fen)){
		destroy_game(game);
		return 0;
	}

	uint64_t pieces_before[8];
	memcpy(pieces_before, game->state.pieces, sizeof(pieces_before));
	uint64_t hash_before = game->state.zobrist_hash;
	int side_before = game->state.side_to_move;
	int en_passant_before = game->state.en_passant;
	uint8_t castling_before = game->state.castling_rights;

	Move promotion = encode_promotion(D7, D8, &game->state, QUEEN_PROMOTION);
	if(!is_legal_player_move(game, promotion)){
		destroy_game(game);
		return 0;
	}

	make_move(game, promotion);

	int success = 1;
	success = success && ((game->state.pieces[Pawn] & U64_MASK(D8)) == 0);
	success = success && ((game->state.pieces[Queen] & U64_MASK(D8)) != 0);
	success = success && ((game->state.pieces[White] & U64_MASK(D8)) != 0);
	success = success && ((game->state.pieces[Pawn] & U64_MASK(D7)) == 0);

	unmake_move(game, promotion);

	success = success && (memcmp(pieces_before, game->state.pieces, sizeof(pieces_before)) == 0);
	success = success && (hash_before == game->state.zobrist_hash);
	success = success && (side_before == game->state.side_to_move);
	success = success && (en_passant_before == game->state.en_passant);
	success = success && (castling_before == game->state.castling_rights);

	destroy_game(game);
	return success;
}

int test_long_game_round_trip(){
	static const char* move_text[] = {
		"e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6",
		"O-O", "Be7", "Re1", "b5", "Bb3", "d6", "c3", "O-O",
		"h3", "Nb8", "d4", "Nbd7"
	};
	Game* game = create_game();
	if(game == NULL){
		return 0;
	}
	initialize_game(game);

	BoardState state_before = game->state;
	MoveList legal_moves_before = game->legal_moves;
	Move moves[20];
	int success = 1;
	int made_moves = 0;

	for(int i = 0; i < 20; i++){
		moves[i] = parse_algebraic_move((char*)move_text[i], game);
		if(moves[i] == 0 || !is_legal_player_move(game, moves[i])){
			fprintf(stderr, "long round-trip rejected move %d: %s\n", i, move_text[i]);
			success = 0;
			break;
		}
		make_move(game, moves[i]);
		made_moves++;
	}

	for(int i = made_moves - 1; i >= 0; i--){
		unmake_move(game, moves[i]);
	}

	success = success && game->game_ply == 0;
	success = success && memcmp(&state_before, &game->state, sizeof(state_before)) == 0;
	success = success && legal_moves_before.size == game->legal_moves.size;
	if(success){
		for(int i = 0; i < legal_moves_before.size; i++){
			success = success && legal_moves_before.moves[i] == game->legal_moves.moves[i];
		}
	}

	destroy_game(game);
	return success;
}

int test_special_move_round_trip(){
	const char* castling_fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
	const char* en_passant_fen = "4k3/8/3p4/4P3/8/8/8/4K3 w - d6 0 1";
	Game* game = create_game();
	if(game == NULL){
		return 0;
	}

	int success = load_fen(game, (char*)castling_fen);
	BoardState castling_state = game->state;
	MoveList castling_moves = game->legal_moves;
	Move castling = parse_algebraic_move("O-O", game);
	success = success && castling != 0 && is_legal_player_move(game, castling);
	make_move(game, castling);
	unmake_move(game, castling);
	success = success && memcmp(&castling_state, &game->state, sizeof(castling_state)) == 0;
	success = success && castling_moves.size == game->legal_moves.size;
	if(success){
		for(int i = 0; i < castling_moves.size; i++){
			success = success && castling_moves.moves[i] == game->legal_moves.moves[i];
		}
	}

	success = success && load_fen(game, (char*)en_passant_fen);
	BoardState en_passant_state = game->state;
	MoveList en_passant_moves = game->legal_moves;
	Move en_passant = parse_algebraic_move("exd6", game);
	success = success && en_passant != 0 && is_legal_player_move(game, en_passant);
	make_move(game, en_passant);
	unmake_move(game, en_passant);
	success = success && memcmp(&en_passant_state, &game->state, sizeof(en_passant_state)) == 0;
	success = success && en_passant_moves.size == game->legal_moves.size;
	if(success){
		for(int i = 0; i < en_passant_moves.size; i++){
			success = success && en_passant_moves.moves[i] == game->legal_moves.moves[i];
		}
	}

	destroy_game(game);
	return success;
}

static int play_repetition_cycle(Game* game, Move moves[4]){
	static const char* move_text[] = {"Na3", "Nh6", "Nb1", "Ng8"};
	for(int i = 0; i < 4; i++){
		moves[i] = parse_algebraic_move((char*)move_text[i], game);
		if(moves[i] == 0 || !is_legal_player_move(game, moves[i])){
			return 0;
		}
		make_move(game, moves[i]);
	}
	return 1;
}

int test_threefold_repetition(){
	Game* game = create_game();
	if(game == NULL || !load_fen(game, "6nk/8/8/8/8/8/8/KN6 w - - 0 1")){
		destroy_game(game);
		return 0;
	}

	Move first_cycle[4];
	Move second_cycle[4];
	int success = !is_threefold_repetition(game);
	success = success && play_repetition_cycle(game, first_cycle);
	success = success && !is_threefold_repetition(game);
	success = success && play_repetition_cycle(game, second_cycle);
	success = success && is_threefold_repetition(game);
	update_game_status(game);
	success = success && game->game_status == DRAW_THREEFOLD_REPETITION;

	destroy_game(game);
	return success;
}

int test_unmake_discards_repetition_history(){
	Game* game = create_game();
	if(game == NULL || !load_fen(game, "6nk/8/8/8/8/8/8/KN6 w - - 0 1")){
		destroy_game(game);
		return 0;
	}

	uint64_t initial_hash = game->state.zobrist_hash;
	Move first_cycle[4];
	Move second_cycle[4];
	int success = play_repetition_cycle(game, first_cycle);
	success = success && play_repetition_cycle(game, second_cycle);
	success = success && is_threefold_repetition(game);

	for(int i = 3; i >= 0; i--){
		unmake_move(game, second_cycle[i]);
	}
	for(int i = 3; i >= 0; i--){
		unmake_move(game, first_cycle[i]);
	}

	success = success && game->game_ply == 0;
	success = success && game->state.zobrist_hash == initial_hash;
	success = success && !is_threefold_repetition(game);

	destroy_game(game);
	return success;
}

int test_dummy(){
	return 1;
}

int run_tests(int (*test_cases[])(), char** test_case_names, int num_cases){
	int result = 1;
	for(int i=0;i<num_cases;i++){
		if(strcmp(test_case_names[i], "test_dummy") == 0){ continue; }
		if(!test_cases[i]()){
			fprintf(stderr, "[x] FAIL: %s\n", test_case_names[i]);
			result = 0;
		} else {
			printf("[ ] PASS: %s\n", test_case_names[i]);
		}
	}
	return result;
}

int main(){
	int (*test_cases[15])(); // array of function pointers
	char* test_case_names[15];

	initialize_attack_data();

	test_cases[0] = test_load_fen;
	test_cases[1] = test_make_move;
	test_cases[2] = test_game_init;
	test_cases[3] = test_zobrist_hash_after_move;
	test_cases[4] = test_unmake_move_round_trip;
	test_cases[5] = test_load_fen_invalid;
	test_cases[6] = test_load_fen_rejects_kingless_by_default;
	test_cases[7] = test_load_fen_ex_allows_kingless;
	test_cases[8] = test_save_fen_initial_position;
	test_cases[9] = test_save_fen_round_trip_state;
	test_cases[10] = test_promotion_round_trip;
	test_cases[11] = test_long_game_round_trip;
	test_cases[12] = test_special_move_round_trip;
	test_cases[13] = test_threefold_repetition;
	test_cases[14] = test_unmake_discards_repetition_history;

	test_case_names[0] = "test_load_fen";
	test_case_names[1] = "test_make_move";
	test_case_names[2] = "test_game_init";
	test_case_names[3] = "test_zobrist_hash_after_move";
	test_case_names[4] = "test_unmake_move_round_trip";
	test_case_names[5] = "test_load_fen_invalid";
	test_case_names[6] = "test_load_fen_rejects_kingless_by_default";
	test_case_names[7] = "test_load_fen_ex_allows_kingless";
	test_case_names[8] = "test_save_fen_initial_position";
	test_case_names[9] = "test_save_fen_round_trip_state";
	test_case_names[10] = "test_promotion_round_trip";
	test_case_names[11] = "test_long_game_round_trip";
	test_case_names[12] = "test_special_move_round_trip";
	test_case_names[13] = "test_threefold_repetition";
	test_case_names[14] = "test_unmake_discards_repetition_history";

	
	printf("====== GAME TESTS ======\n");
	int success = run_tests(test_cases, test_case_names, 15);

	printf("====== MOVE TESTS ======\n");
	success = move_tests() && success;

	printf("====== ATTACK TESTS ======\n");
	success = attack_tests() && success;

	printf("====== MOVE GEN TESTS ======\n");
	success = move_gen_tests() && success;

	printf("====== SEARCH TESTS ======\n");
	success = search_tests() && success;

	printf("====== ENDGAME TESTS ======\n");
	success = endgame_tests() && success;

	printf("====== TRANSPOSITION TABLE TESTS ======\n");
	success = transposition_table_tests() && success;

	printf("====== PERFT TESTS ======\n");
	success = perft_tests() && success;

	printf("====== QUIESCENCE TESTS ======\n");
	success = quiescence_tests() && success;

	printf("====== UCI TESTS ======\n");
	success = uci_tests() && success;

	printf("======\n");
	return success ? 0 : 1;
}
