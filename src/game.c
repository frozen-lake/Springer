#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <string.h>
#include <stddef.h>
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
	initialize_board(&game->state);
	game->state.side_to_move = 1;
	game->state.castling_rights = 0b1111;
	game->state.en_passant = -1;
	game->state.halfmove_clock = 0;
	game->game_ply = 0;
	initialize_zobrist_keys();
	compute_zobrist_hash(&game->state);
	move_list_init(&game->legal_moves);
	generate_legal_moves(game, game->state.side_to_move);
}

/* Load a position from a FEN String (Forsyth-Edwards Notation.) */
int load_fen(Game* game, char* str){
	Board* board = &game->state;
	empty_board(board);

	// Split up FEN by spaces
	char fen[128];
	if(str == NULL){
		return 0;
	}
	if(snprintf(fen, sizeof(fen), "%s", str) >= (int)sizeof(fen)){
		return 0;
	}
	char* delim = " ";

	char* fen_field[6] = {NULL};
	fen_field[0] = strtok(fen, delim);
	
	for(int i=1; i<6 && fen_field[i-1]!=NULL; i++){
		fen_field[i] = strtok(NULL, delim);
	}

	if(fen_field[1]){
		game->state.side_to_move = (fen_field[1][0] == 'b') ? 0 : 1;
	}
	
	// Split field 0 into ranks by '/' 
	if(fen_field[0] == NULL){ return 0; }
	
	char* ranks[8];
	delim = "/";
	ranks[0] = strtok(fen, delim);
    if(ranks[0] == NULL){ return 0; }	
	for(int i=1;i<8;i++){
		ranks[i] = strtok(NULL, delim);
		if(ranks[i] == NULL){ return 0; };
	}		
	
	// Process ranks
	for(int rank=7;rank>=0;rank--){
		int len = strlen(ranks[7-rank]);
		int file = 0;
		for(int j=0;j<len;j++){
			if(file > 7){ return 0; } // Additional space or pieces after capacity is reached
			char c = ranks[7 - rank][j];

			if(isdigit(c)){
				int p = (int) (c - '0');
				if(file + p > 8){
					return 0;
				}
				file += p;
			} else if(isalpha(c)){
				uint64_t piece = 1ULL << (rank*8 + file);
				char u = toupper(c);
				switch(u){
					case 'P':
						board->pieces[Pawn] |= piece;
						break;
					case 'N':
						board->pieces[Knight] |= piece;
						break;
					case 'B':
						board->pieces[Bishop] |= piece;
						break;
					case 'R':
						board->pieces[Rook] |= piece;
						break;
					case 'Q':
						board->pieces[Queen] |= piece;
						break;
					case 'K':
						board->pieces[King] |= piece;
						board->king_sq[isupper(c) ? White : Black] = (rank*8 + file);
						break;
					default:
						return 0;
				}

				if(isupper(c)){
					board->pieces[White] |= piece;
				} else {
					board->pieces[Black] |= piece;
				}
				file++;
			} else {
				return 0;
			}
		}
		
	}	
	
	game->state.castling_rights = 0;
	if(fen_field[2] && fen_field[2][0] != '-'){
		int k = 0;
		while(fen_field[2][k] != '\0'){
			switch(fen_field[2][k]){
				case 'Q':
					game->state.castling_rights |= (1 << 3);
					break;
				case 'K':
					game->state.castling_rights |= (1 << 2);
					break;
				case 'q':
					game->state.castling_rights |= (1 << 1);
					break;
				case 'k':
					game->state.castling_rights |= (1 << 0);
					break;
			}
			k++;
		}
	}

	if(fen_field[3] && fen_field[3][0] != '-'){
		game->state.en_passant = parse_square(fen_field[3]);
	}

	game->state.halfmove_clock = 0;
	if(fen_field[4]){
		game->state.halfmove_clock = atoi(fen_field[4]);
	}
	game->game_ply = 0;

	initialize_zobrist(game);

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