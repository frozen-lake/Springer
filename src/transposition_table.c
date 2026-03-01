#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include "../src/transposition_table.h"


void table_add(TranspositionTable* table, uint64_t key, Move move, int score, int depth, int flag){
    int index = key & (table->size - 1);

    TranspositionTableEntry* entry = &table->entries[index];
    if((entry->key == 0) || (entry->depth < depth) || (entry->age < table->age)){
        entry->key = key;
        entry->score = score;
        entry->depth = depth;
        entry->best_move = move;
        entry->flag = flag;
        entry->age = table->age;
    }
}

TranspositionTableEntry* table_get(TranspositionTable* table, uint64_t key){
    int index = key & (table->size - 1);
    TranspositionTableEntry* entry = &table->entries[index];
    
    if(entry->key == key){
        return entry;
    }
    return NULL;
}

void table_clear(TranspositionTable* table){
    memset(table->entries, 0, (table->size * sizeof(TranspositionTableEntry)));
    table->age = 0;
}

void table_init(TranspositionTable* table){
    table->entries = (TranspositionTableEntry*) calloc(TT_ENTRIES, sizeof(TranspositionTableEntry));
    if(table->entries == NULL){
        table->size = 0;
        return;
    }
    table->size = TT_ENTRIES;
    table->age = 0;
}

void table_free(TranspositionTable* table){
    if(table->entries){
        free(table->entries);
        table->entries = NULL;
    }
    table->size = 0;
}
