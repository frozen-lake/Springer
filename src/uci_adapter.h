#ifndef UCI_ADAPTER_H
#define UCI_ADAPTER_H

#include <stddef.h>
#include "game.h"
#include "move.h"

#define UCI_MOVE_STR_LEN 6

int move_to_uci(Move move, char* out, size_t out_size);
int parse_uci_move(const char* uci, Game* game, Move* out_move);

int algebraic_to_uci(const char* algebraic, Game* game, char* out, size_t out_size);
int uci_to_simplified_algebraic(const char* uci, Game* game, char* out, size_t out_size);

#endif
