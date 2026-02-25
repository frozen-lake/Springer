#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include "board.h"
#include "attack_data.h"
#include "game.h"

ZobristKeys zobrist_keys;
static int zobrist_keys_initialized = 0;

/* Initialize a Board with the starting position. */
void initialize_board(Board* board){
	board->pieces[Black] = 0xFFFFULL << 48;
	board->pieces[White] = 0xFFFFULL;

	board->pieces[Pawn] = 0x00ff00000000ff00ULL;
	board->pieces[Knight] = 66ULL + (66ULL << 56);
	board->pieces[Bishop] = 36ULL + (36ULL << 56);
	board->pieces[Rook] = 129ULL + (129ULL << 56);
	board->pieces[Queen] = 8ULL + (8ULL << 56);
	board->pieces[King] = 16ULL + (16ULL << 56);

	board->king_sq[White] = E1;
	board->king_sq[Black] = E8;
}

uint64_t random_u64(void){
	uint64_t result = 0;
	result = rand();
	for(int i=0;i<4;i++){
		result = (result << 15) | rand();
	}
	return result;
}

void initialize_zobrist_keys(void){
	if(zobrist_keys_initialized){
		return;
	}

	srand(1234567);
	for(int color=0; color<2; color++){
		for(int piece=0; piece<6; piece++){
			for(int square=0; square<64; square++){
				zobrist_keys.piece_square[color][piece][square] = random_u64();
			}
		}
	}

	for(int i=0; i<16; i++){
		zobrist_keys.castling_rights[i] = random_u64();
	}
	for(int i=0; i<8; i++){
		zobrist_keys.en_passant_file[i] = random_u64();
	}
	zobrist_keys.side_to_move = random_u64();
	zobrist_keys_initialized = 1;
}

uint64_t compute_zobrist_hash(BoardState* state){
	if(state == NULL){
		return 0;
	}

	uint64_t hash = 0;
	uint64_t occupied = state->pieces[White] | state->pieces[Black];
	while(occupied){
		int src = get_lsb_index(occupied);
		occupied &= occupied - 1;
		uint8_t color = !!(state->pieces[White] & U64_MASK(src));
		int piece = position_to_piece_number(state, src);
		if(piece >= Pawn && piece <= King){
			int piece_index = piece - Pawn;
			hash ^= zobrist_keys.piece_square[color][piece_index][src];
		}
	}

	hash ^= zobrist_keys.castling_rights[state->castling_rights];
	if(state->en_passant != -1) {
		hash ^= zobrist_keys.en_passant_file[state->en_passant % 8];
	}
	if(!state->side_to_move){
		hash ^= zobrist_keys.side_to_move;
	}

	state->zobrist_hash = hash;
	return hash;
}

void initialize_zobrist(Game* game){
	if(game == NULL){
		return;
	}

	initialize_zobrist_keys();
	compute_zobrist_hash(&game->state);
}


/* Prints the board. */
void print_board(Board* board){
	for(int i=7;i>=0;i--){
		printf("| %c", position_to_piece_char(board, i*8));
		for(int j=1;j<8;j++){
			printf(" | %c", position_to_piece_char(board, i*8 + j));
		}
		printf(" |\n");	
	}
	printf("=================================\n");
}

/* Zeroes out the pieces array in the given BoardState */
void empty_board(Board* board){
	board->pieces[Black] = board->pieces[White] = board->pieces[Pawn] = board->pieces[Knight] = board->pieces[Bishop] = board->pieces[Rook] = board->pieces[Queen] = board->pieces[King] = 0;
}

/* Prints the passed bitboard in an 8x8 format. */
void print_bitboard(uint64_t bb){
	for(int i=7;i>=0;i--){
		printf("| %llu", (bb >> (i*8))&1);
		for(int j=1;j<8;j++){
			printf(" | %llu", (bb>>(i*8 + j))&1);
		}
		printf(" |\n");	
	}
	printf("=================================\n");
	
}
/* Returns the character representing the piece at the given bit index. */
char position_to_piece_char(Board* board, int pos){
	char c = ' ';
	uint64_t mask = U64_MASK(pos);
	if(board->pieces[Pawn] & mask) c = 'p';
	if(board->pieces[Knight] & mask) c = 'n';
	if(board->pieces[Bishop] & mask) c = 'b';
	if(board->pieces[Rook] & mask) c = 'r';
	if(board->pieces[Queen] & mask) c = 'q';
	if(board->pieces[King] & mask) c = 'k';
	if(board->pieces[White] & mask) c = toupper(c);
	return c;
}

int position_to_piece_number(Board* board, int pos){
	uint64_t mask = U64_MASK(pos);
	if(board->pieces[Pawn] & mask) return Pawn;
	if(board->pieces[Knight] & mask) return Knight;
	if(board->pieces[Bishop] & mask) return Bishop;
	if(board->pieces[Rook] & mask) return Rook;
	if(board->pieces[Queen] & mask) return Queen;
	if(board->pieces[King] & mask) return King;
	return 0;
}

int square_attacked(Board* board, int square, int attacker_color){
    uint64_t attackers = board->pieces[attacker_color];
    uint64_t occupancy = board->pieces[White] | board->pieces[Black];

    /* Check pawns */
    if (get_pawn_attacks(square, !attacker_color) & (attackers & board->pieces[Pawn])) return 1;

    /* Check knights */
    if (get_knight_attacks(square) & (attackers & board->pieces[Knight])) return 1;

    /* Check king */
    if (get_king_attacks(square) & (attackers & board->pieces[King])) return 1;

    /* Check sliders */
    if (get_rook_attacks(square, occupancy) & (attackers & (board->pieces[Rook] | board->pieces[Queen]))) return 1;
    if (get_bishop_attacks(square, occupancy) & (attackers & (board->pieces[Bishop] | board->pieces[Queen]))) return 1;

    return 0;
}

char* piece_to_string(int piece){
	switch(piece){
		case Pawn:
			return "Pawn";
		case Knight:
			return "Knight";
		case Bishop:
			return "Bishop";
		case Rook:
			return "Rook";
		case Queen:
			return "Queen";
		case King:
			return "King";
		default:
			return "None";
	}
}

int board_validate(Board* board){
	uint64_t white = board->pieces[White];
	uint64_t black = board->pieces[Black];
	uint64_t occupancy = white | black;

	// Piece color bitboards are disjoint
	if(white & black){
		return 0;
	}

	uint64_t type_union = board->pieces[Pawn]
		| board->pieces[Knight]
		| board->pieces[Bishop]
		| board->pieces[Rook]
		| board->pieces[Queen]
		| board->pieces[King];

	if(type_union != occupancy){
		return 0;
	}

	// Piece type bitboards are disjoint
	for(int i=Pawn; i<=King; i++){
		for(int j=i+1; j<=King; j++){
			if(board->pieces[i] & board->pieces[j]){
				return 0;
			}
		}
	}

	// Exactly 1 king per side
	int white_kings = __builtin_popcountll(board->pieces[King] & white);
	int black_kings = __builtin_popcountll(board->pieces[King] & black);
	if(white_kings != 1 || black_kings != 1){
		return 0;
	}

	return 1;
}