#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <string.h>
#include <stddef.h>
#include <limits.h>
#include "game.h"
#include "board.h"
#include "move.h"
#include "move_gen.h"
#include "attack_data.h"

static int ensure_undo_capacity(Game* game, int required){
	if(required <= game->undo_capacity){
		return 1;
	}

	int new_capacity = game->undo_capacity > 0 ? game->undo_capacity : 512;
	while(new_capacity < required){
		new_capacity *= 2;
	}

	UndoInfo* new_stack = (UndoInfo*)realloc(game->undo_stack, new_capacity * sizeof(UndoInfo));
	if(new_stack == NULL){
		return 0;
	}

	game->undo_stack = new_stack;
	game->undo_capacity = new_capacity;
	return 1;
}


/* Create, initialize and return a Game. Also creates a Board and sets up the AttackData for this game. */
Game* create_game(){
	Game* game = calloc(1, sizeof(Game));
	game->state.side_to_move = 1;
	game->state.en_passant = -1;
	game->state.halfmove_clock = 0;

	game->state.castling_rights = 0b1111;
	game->move_history_capacity = 512;
	game->move_history = (Move*) calloc(game->move_history_capacity, sizeof(Move));
	game->undo_capacity = 512;
	game->undo_stack = (UndoInfo*) calloc(game->undo_capacity, sizeof(UndoInfo));
	game->game_ply = 0;

	move_list_init(&game->legal_moves);

	return game;
}

/* Destroy a Game. */
void destroy_game(Game* game){
	if(!game) return;
	free(game->move_history);
	free(game->undo_stack);
	free(game);
}

/* Set the Board. */
void initialize_game(Game* game){
	if(game == NULL){
		return;
	}

	initialize_board(&game->state);
	game->state.side_to_move = 1;
	game->state.castling_rights = 0b1111;
	game->state.en_passant = -1;
	game->state.halfmove_clock = 0;
	game->game_ply = 0;
	game->game_status = ACTIVE;
	initialize_zobrist_keys();
	compute_zobrist_hash(&game->state);
	move_list_init(&game->legal_moves);
	generate_legal_moves(game, game->state.side_to_move);
}

static int parse_unsigned_field(const char* field, unsigned long maximum, unsigned long* value){
	if(field == NULL || field[0] == '\0' || value == NULL){
		return 0;
	}

	unsigned long parsed = 0;
	for(size_t i = 0; field[i] != '\0'; i++){
		if(!isdigit((unsigned char)field[i])){
			return 0;
		}
		unsigned int digit = (unsigned int)(field[i] - '0');
		if(parsed > (maximum - digit) / 10){
			return 0;
		}
		parsed = parsed * 10 + digit;
	}

	*value = parsed;
	return 1;
}

/* Load a position from a FEN String (Forsyth-Edwards Notation.) */
int load_fen(Game* game, char* str){
	if(game == NULL || str == NULL){
		return 0;
	}

	char fen[128];
	if(snprintf(fen, sizeof(fen), "%s", str) >= (int)sizeof(fen)){
		return 0;
	}

	char* fields[7] = {NULL};
	int field_count = 0;
	char* field = strtok(fen, " \t\r\n");
	while(field != NULL && field_count < 7){
		fields[field_count++] = field;
		field = strtok(NULL, " \t\r\n");
	}
	if(field != NULL || field_count != 6){
		return 0;
	}

	BoardState parsed_state;
	empty_board(&parsed_state);
	Board* board = &parsed_state;

	if(strcmp(fields[1], "w") == 0){
		board->side_to_move = White;
	} else if(strcmp(fields[1], "b") == 0){
		board->side_to_move = Black;
	} else {
		return 0;
	}

	const char* placement = fields[0];
	int rank = 7;
	int file = 0;
	int white_king_count = 0;
	int black_king_count = 0;
	for(size_t i = 0; ; i++){
		char c = placement[i];
		if(c == '/' || c == '\0'){
			if(file != 8 || rank == 0 && c == '/'){
				return 0;
			}
			if(c == '\0'){
				break;
			}
			rank--;
			file = 0;
			continue;
		}

		if(rank < 0 || file >= 8){
			return 0;
		}

		if(c >= '1' && c <= '8'){
			file += c - '0';
			if(file > 8){
				return 0;
			}
			continue;
		}

		char piece = (char)toupper((unsigned char)c);
		int piece_type;
		switch(piece){
			case 'P': piece_type = Pawn; break;
			case 'N': piece_type = Knight; break;
			case 'B': piece_type = Bishop; break;
			case 'R': piece_type = Rook; break;
			case 'Q': piece_type = Queen; break;
			case 'K': piece_type = King; break;
			default: return 0;
		}

		if(file >= 8){
			return 0;
		}
		uint64_t piece_mask = U64_MASK(rank * 8 + file);
		board->pieces[piece_type] |= piece_mask;
		int color = isupper((unsigned char)c) ? White : Black;
		board->pieces[color] |= piece_mask;
		if(piece_type == King){
			board->king_sq[color] = (int8_t)(rank * 8 + file);
			if(color == White){
				white_king_count++;
			} else {
				black_king_count++;
			}
		}
		file++;
	}

	if(rank != 0 || white_king_count > 1 || black_king_count > 1){
		return 0;
	}

	if(strcmp(fields[2], "-") != 0){
		if(fields[2][0] == '\0'){
			return 0;
		}
		for(size_t i = 0; fields[2][i] != '\0'; i++){
			uint8_t right;
			switch(fields[2][i]){
				case 'Q': right = 1 << 3; break;
				case 'K': right = 1 << 2; break;
				case 'q': right = 1 << 1; break;
				case 'k': right = 1 << 0; break;
				default: return 0;
			}
			if(board->castling_rights & right){
				return 0;
			}
			board->castling_rights |= right;
		}
	}

	if(strcmp(fields[3], "-") != 0){
		board->en_passant = (int8_t)parse_square(fields[3]);
		if(board->en_passant < 0
			|| (board->side_to_move == White && board->en_passant / 8 != 5)
			|| (board->side_to_move == Black && board->en_passant / 8 != 2)){
			return 0;
		}
	}

	unsigned long halfmove_clock;
	unsigned long fullmove_number;
	if(!parse_unsigned_field(fields[4], UINT8_MAX, &halfmove_clock)
		|| !parse_unsigned_field(fields[5], ULONG_MAX, &fullmove_number)
		|| fullmove_number == 0){
		return 0;
	}
	board->halfmove_clock = (uint8_t)halfmove_clock;

	initialize_zobrist_keys();
	compute_zobrist_hash(board);
	game->state = parsed_state;
	game->game_ply = 0;
	game->game_status = ACTIVE;
	move_list_init(&game->legal_moves);
	generate_legal_moves(game, game->state.side_to_move);

	return 1;
}

int has_insufficient_material(BoardState* state){
	if(state->pieces[Pawn] || state->pieces[Rook] || state->pieces[Queen]){
		return 0;
	}

	uint64_t minor_pieces = state->pieces[Knight] | state->pieces[Bishop];
	int minor_count = __builtin_popcountll(minor_pieces);

	return minor_count <= 1;
}

void update_game_status(Game* game){
	game->game_status = ACTIVE;
	if(game->state.halfmove_clock > 99){
		game->game_status = DRAW_FIFTY_MOVE;
	} else if(game->legal_moves.size == 0){
		if(square_attacked(&game->state, game->state.king_sq[game->state.side_to_move], !game->state.side_to_move)){
			game->game_status = BLACK_WINS + (1 - game->state.side_to_move); // Winner is opposite of side to move
		} else {
			game->game_status = DRAW_STALEMATE;
		}
	} else if(has_insufficient_material(&game->state)){
		game->game_status = DRAW_INSUFFICIENT_MATERIAL;
	}

}


void make_move(Game* game, Move move){
	if(!ensure_undo_capacity(game, game->game_ply + 1)){
		return;
	}
	make_move_on_state(&game->state, move, &game->undo_stack[game->game_ply]);
	game->game_ply += 1;
	move_list_init(&game->legal_moves);
	generate_legal_moves(game, game->state.side_to_move);
}


void unmake_move(Game* game, Move move){
	if(game->game_ply <= 0){
		return;
	}
	game->game_ply -= 1;
	unmake_move_on_state(&game->state, move, &game->undo_stack[game->game_ply]);
	move_list_init(&game->legal_moves);
	generate_legal_moves(game, game->state.side_to_move);
}

void make_move_on_state(BoardState* state, Move move, UndoInfo* undo){
	int color = state->side_to_move;
	int dest = get_move_dest(move);
	int src = get_move_src(move);
	int piece_type = get_move_piece(move);
	int capture = get_move_capture(move);
	int special = get_move_special(move);
	int promotion = get_move_promotion(move);

	int promotion_piece = promotion != NO_PROMOTION ? Pawn + promotion : 0;
	int moving_piece = (piece_type == Pawn && promotion_piece) ? promotion_piece : piece_type; // Final piece on dest square

	undo->castling_rights = state->castling_rights;
	undo->en_passant = state->en_passant;
	undo->halfmove_clock = state->halfmove_clock;
	undo->zobrist_hash = state->zobrist_hash;
	int capture_square = dest;
	if(capture && special == EnPassant){
		capture_square = color ? dest - 8 : dest + 8;
	}

	int piece_index = piece_type - Pawn;
	int moving_piece_index = moving_piece - Pawn;
	state->zobrist_hash ^= zobrist_keys.piece_square[color][piece_index][src];
	state->zobrist_hash ^= zobrist_keys.piece_square[color][moving_piece_index][dest];
	if(capture){
		int capture_index = capture - Pawn;
		state->zobrist_hash ^= zobrist_keys.piece_square[!color][capture_index][capture_square];
	}
	if(special > EnPassant){
		int rook_src = (special == Kingside) ? src + 3 : src - 4;
		int rook_dest = (special == Kingside) ? src + 1 : src - 1;
		int rook_index = Rook - Pawn;
		state->zobrist_hash ^= zobrist_keys.piece_square[color][rook_index][rook_src];
		state->zobrist_hash ^= zobrist_keys.piece_square[color][rook_index][rook_dest];
	}

	if(capture != 0){
		state->pieces[capture] &= ~U64_MASK(capture_square);
		state->pieces[!color] &= ~U64_MASK(capture_square);

		if(!special && capture == Rook){
			switch(dest){
				case A1:
					state->castling_rights &= 0b0111;
					break;
				case H1:
					state->castling_rights &= 0b1011;
					break;
				case A8:
					state->castling_rights &= 0b1101;
					break;
				case H8:
					state->castling_rights &= 0b1110;
					break;
			}
		}
	}

	state->pieces[piece_type] &= ~U64_MASK(src);
	state->pieces[moving_piece] |= U64_MASK(dest);

	state->pieces[color] &= ~U64_MASK(src);
	state->pieces[color] |= U64_MASK(dest);

	if(special > EnPassant){
		int rook_src = (special == Kingside) ? src + 3 : src - 4;
		int rook_dest = (special == Kingside) ? src + 1 : src - 1;
		
		state->pieces[color] &= ~U64_MASK(rook_src);
		state->pieces[color] |= U64_MASK(rook_dest);
		state->pieces[Rook] &= ~U64_MASK(rook_src);
		state->pieces[Rook] |= U64_MASK(rook_dest);
	}

	state->en_passant = -1;
	if(piece_type == Pawn){
		if(color && (dest == src+16)){
			state->en_passant = src+8;
		} else if(!color && (dest == src-16)){
			state->en_passant = src-8;
		}
	}

	if(piece_type == Pawn || capture){
		state->halfmove_clock = 0;
	} else {
		state->halfmove_clock += 1;
	}

	if(piece_type == King){
		state->king_sq[color] = dest;
		if(src == E1) state->castling_rights &= 0b0011;
		if(src == E8) state->castling_rights &= 0b1100;
	} else if(piece_type == Rook){
		if(src == A1) state->castling_rights &= 0b0111;
		if(src == H1) state->castling_rights &= 0b1011;
		if(src == A8) state->castling_rights &= 0b1101;
		if(src == H8) state->castling_rights &= 0b1110;
	}

	state->zobrist_hash ^= zobrist_keys.castling_rights[undo->castling_rights];
	state->zobrist_hash ^= zobrist_keys.castling_rights[state->castling_rights];
	if(undo->en_passant != -1){
		state->zobrist_hash ^= zobrist_keys.en_passant_file[undo->en_passant % 8];
	}
	if(state->en_passant != -1){
		state->zobrist_hash ^= zobrist_keys.en_passant_file[state->en_passant % 8];
	}
	state->zobrist_hash ^= zobrist_keys.side_to_move;

	state->side_to_move = !state->side_to_move;
}

void unmake_move_on_state(BoardState* state, Move move, UndoInfo* undo){
	int dest = get_move_dest(move);
	int src = get_move_src(move);
	int piece_type = get_move_piece(move);
	int capture = get_move_capture(move);
	int special = get_move_special(move);
	int promotion = get_move_promotion(move);

	int promotion_piece = promotion != NO_PROMOTION ? Pawn + promotion : 0;
	int moving_piece = (piece_type == Pawn && promotion_piece) ? promotion_piece : piece_type;

	state->side_to_move = !state->side_to_move;
	int color = state->side_to_move;

	int capture_square = dest;
	if(capture && special == EnPassant){
		capture_square = color ? dest - 8 : dest + 8;
	}

	state->castling_rights = undo->castling_rights;
	state->en_passant = undo->en_passant;
	state->halfmove_clock = undo->halfmove_clock;
	state->zobrist_hash = undo->zobrist_hash;

	if(piece_type == Pawn && promotion_piece){
		state->pieces[moving_piece] &= ~U64_MASK(dest);
	} else {
		state->pieces[piece_type] &= ~U64_MASK(dest);
	}

	state->pieces[piece_type] |= U64_MASK(src);

	state->pieces[color] &= ~U64_MASK(dest);
	state->pieces[color] |= U64_MASK(src);

	if(special > EnPassant){
		int rook_src = (special == Kingside) ? src + 3 : src - 4;
		int rook_dest = (special == Kingside) ? src + 1 : src - 1;
		state->pieces[Rook] &= ~U64_MASK(rook_dest);
		state->pieces[Rook] |= U64_MASK(rook_src);
		state->pieces[color] &= ~U64_MASK(rook_dest);
		state->pieces[color] |= U64_MASK(rook_src);
	}

	if(piece_type == King){
		state->king_sq[color] = src;
	}

	if(capture){
		state->pieces[capture] |= U64_MASK(capture_square);
		state->pieces[!color] |= U64_MASK(capture_square);
	}
}

uint64_t swap_uint64(uint64_t num){
    return __builtin_bswap64(num);
}

int get_lsb_index(uint64_t num){
    return __builtin_ctzll(num);
}