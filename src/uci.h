#ifndef UCI_H
#define UCI_H

#include <stdio.h>
#include <stddef.h>
#include "game.h"
#include "move.h"

#define UCI_MOVE_STR_LEN 6

/* 8,850 moves = 17,700 plies at 5 bytes each ("e2e4 "), plus 1 byte per promotion (at most 16), plus a ~110-byte FEN prefix: at most 88,626 bytes. 96 KiB (98,304 bytes) leaves room. */
#define UCI_LINE_MAX (96 * 1024)

int move_to_uci(Move move, char* out, size_t out_size);
int parse_uci_move(const char* uci, Game* game, Move* out_move);

int handle_position_command(Game* game, const char* args);
int run_uci_loop(FILE* in, FILE* out, FILE* err);
int uci_clock_budget_ms(int remaining_ms, int increment_ms);

int algebraic_to_uci(const char* algebraic, Game* game, char* out, size_t out_size);
int uci_to_simplified_algebraic(const char* uci, Game* game, char* out, size_t out_size);

#endif
