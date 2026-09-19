#include "board.h"
#include "attack_data.h"
#include "move.h"

#ifndef GAME_H
#define GAME_H

#define DEBUG_ERR 1


enum GameStatus {
	ACTIVE,
	BLACK_WINS,
	WHITE_WINS,
	DRAW_STALEMATE,
	DRAW_FIFTY_MOVE,
	DRAW_INSUFFICIENT_MATERIAL,
	DRAW_THREEFOLD_REPETITION,
};

typedef struct Game Game;

struct Game {
	BoardState state;
	MoveList legal_moves;
	Move* move_history;
	int move_history_capacity;
	UndoInfo* undo_stack;
	int undo_capacity;
	uint64_t* position_history;
	int position_history_capacity;
	int game_ply;
	int game_status;
};


#endif


Game* create_game();
void destroy_game(Game* game);
void initialize_game(Game* game);
int load_fen(Game* game, char* str);
int save_fen(const Game* game, char* out, size_t out_size);
void update_game_status(Game* game);
int has_insufficient_material(BoardState* state);
int is_threefold_repetition(const Game* game);

void make_move(Game* game, Move move);
void unmake_move(Game* game, Move move);

void make_move_on_state(BoardState* state, Move move, UndoInfo* undo);
void unmake_move_on_state(BoardState* state, Move move, UndoInfo* undo);


/* Utilities */
int get_lsb_index(uint64_t num);
uint64_t swap_uint64(uint64_t num);

