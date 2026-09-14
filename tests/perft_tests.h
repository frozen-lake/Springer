#ifndef PERFT_TESTS_H
#define PERFT_TESTS_H

#include <stdint.h>
#include "../src/game.h"

uint64_t perft(Game* game, int depth);
int perft_tests(void);

#endif
