#include <stdint.h>

#include "game.h"

#ifndef TTABLE_H
#define TTABLE_H
#define TT_ENTRIES 1048576 // 2^20

// Transposition table entry flags
#define TT_EXACT 0
#define TT_LOWER 1
#define TT_UPPER 2

typedef struct {
    uint64_t key;
    Move best_move;
    int32_t score;
    uint8_t depth;
    uint8_t flag;
    uint8_t age;
    uint8_t occupied;
} TranspositionTableEntry;

typedef struct {
    TranspositionTableEntry* entries;
    int size;
    uint8_t age;
} TranspositionTable;

#endif

void tt_add(TranspositionTable* table, uint64_t key, Move move, int score, int depth, int flag);
void tt_clear(TranspositionTable* table);
void tt_new_generation(TranspositionTable* table);
TranspositionTableEntry* table_get(TranspositionTable* table, uint64_t key);
void tt_init(TranspositionTable* table);
void tt_free(TranspositionTable* table);
