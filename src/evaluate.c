#include "evaluate.h"

static int PIECE_VALUES[8] = {0, 0, 100, 300, 300, 500, 900, 10000};

static int clamp_int(int x, int lo, int hi){
	if(x < lo) return lo;
	if(x > hi) return hi;
	return x;
}

static int in_bounds(int file, int rank){
	return file >= 0 && file < 8 && rank >= 0 && rank < 8;
}

static int distance_from_center(int sq){
	int file = sq % 8;
	int rank = sq / 8;

	int df = (file < 4) ? (3 - file) : (file - 4);
	int dr = (rank < 4) ? (3 - rank) : (rank - 4);

	return (df + dr);
}

static int count_knight_mobility(const Board* board, int sq, int side){
	static const int dfile[8] = { 1, 2, 2, 1, -1, -2, -2, -1 };
	static const int drank[8] = { 2, 1, -1, -2, -2, -1, 1, 2 };

	int file = sq & 7;
	int rank = sq >> 3;
	uint64_t own = board->pieces[side];
	int count = 0;

	for(int i = 0; i < 8; i++){
		int nf = file + dfile[i];
		int nr = rank + drank[i];
		if(!in_bounds(nf, nr)){
			continue;
		}
		int nsq = (nr << 3) | nf;
		if((own & U64_MASK(nsq)) == 0){
			count++;
		}
	}
	return count;
}

static int count_one_step_slider_mobility(const Board* board, int sq, int side, int piece){
	int file = sq & 7;
	int rank = sq >> 3;
	uint64_t own = board->pieces[side];
	int count = 0;

	static const int dirs[8][2] = {
		{ 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 },
		{ 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }
	};

	int start = 0;
	int end = 0;
	if(piece == Bishop){
		start = 0; end = 4;
	} else if(piece == Rook){
		start = 4; end = 8;
	} else {
		start = 0; end = 8;
	}

	for(int i = start; i < end; i++){
		int nf = file + dirs[i][0];
		int nr = rank + dirs[i][1];
		if(!in_bounds(nf, nr)){
			continue;
		}
		int nsq = (nr << 3) | nf;
		if((own & U64_MASK(nsq)) == 0){
			count++;
		}
	}

	return count;
}

int evaluate(Game* game){
	Board* board = &game->state;

	// Multiplied with piece centralization score
	const int CENTRAL_PAWN   = 2;
	const int CENTRAL_KNIGHT = 3;
	const int CENTRAL_BISHOP = 3;
	const int CENTRAL_ROOK   = 2;
	const int CENTRAL_QUEEN  = 1;
	const int CENTRAL_KING   = 0;

	// Multiplied with piece mobility score
	const int MOB_KNIGHT = 3;
	const int MOB_BISHOP = 3;
	const int MOB_ROOK   = 2;
	const int MOB_QUEEN  = 1;

	const int DOUBLED_PAWN_PENALTY = 12;

	int material_score = 0;
	int centralization_score = 0;
	int mobility_score = 0;

	int white_pawns_on_file[8] = {0};
	int black_pawns_on_file[8] = {0};

	for(int sq = 0; sq < 64; sq++){
		uint64_t mask = U64_MASK(sq);
		int side = -1;
		if(board->pieces[White] & mask){
			side = White;
		} else if(board->pieces[Black] & mask){
			side = Black;
		} else {
			continue;
		}

		int piece = position_to_piece_number(board, sq);
		int sign = (side == White) ? 1 : -1;

		material_score += sign * PIECE_VALUES[piece];

		int oriented_sq = (side == White) ? sq : (sq ^ 56);
		int base = 6 - (2 * distance_from_center(oriented_sq));

		int piece_central_scale = 0;
		switch(piece){
			case Pawn:   piece_central_scale = CENTRAL_PAWN; break;
			case Knight: piece_central_scale = CENTRAL_KNIGHT; break;
			case Bishop: piece_central_scale = CENTRAL_BISHOP; break;
			case Rook:   piece_central_scale = CENTRAL_ROOK; break;
			case Queen:  piece_central_scale = CENTRAL_QUEEN; break;
			case King:   piece_central_scale = CENTRAL_KING; break;
			default: break;
		}
		centralization_score += sign * (base * piece_central_scale);

		if(piece == Knight){
			mobility_score += sign * (MOB_KNIGHT * count_knight_mobility(board, sq, side));
		} else if(piece == Bishop){
			mobility_score += sign * (MOB_BISHOP * count_one_step_slider_mobility(board, sq, side, Bishop));
		} else if(piece == Rook){
			mobility_score += sign * (MOB_ROOK * count_one_step_slider_mobility(board, sq, side, Rook));
		} else if(piece == Queen){
			mobility_score += sign * (MOB_QUEEN * count_one_step_slider_mobility(board, sq, side, Queen));
		}

		if(piece == Pawn){
			int file = sq & 7;
			if(side == White){
				white_pawns_on_file[file]++;
			} else {
				black_pawns_on_file[file]++;
			}
		}
	}

	// Basic doubled pawn penalties.
	int doubled_pawn_score = 0;
	for(int f = 0; f < 8; f++){
		if(white_pawns_on_file[f] > 1){
			doubled_pawn_score -= DOUBLED_PAWN_PENALTY * (white_pawns_on_file[f] - 1);
		}
		if(black_pawns_on_file[f] > 1){
			doubled_pawn_score += DOUBLED_PAWN_PENALTY * (black_pawns_on_file[f] - 1);
		}
	}

	// Centralization fades out linearly by ply 80 (40 full moves).
	int fade_ply = clamp_int(game->game_ply, 0, 80);
	int centralization_weight_num = 80 - fade_ply;
	int faded_centralization_score = (centralization_score * centralization_weight_num) / 80;

	int score_white_pov = material_score + faded_centralization_score + mobility_score + doubled_pawn_score;

	return (game->state.side_to_move == White) ? score_white_pov : -score_white_pov;
}
