#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include "../src/transposition_table.h"


void tt_add(TranspositionTable* table, uint64_t key, Move move, int score, int depth, int flag){
    if(table == NULL || table->entries == NULL || table->size <= 0){
        return;
    }

    int index = key & (table->size - 1);

    TranspositionTableEntry* entry = &table->entries[index];
    if(!entry->occupied || (entry->depth < depth) || (entry->age < table->age)){
        entry->key = key;
        entry->score = score;
        entry->depth = depth;
        entry->best_move = move;
        entry->flag = flag;
        entry->age = table->age;
        entry->occupied = 1;
    }
}

TranspositionTableEntry* table_get(TranspositionTable* table, uint64_t key){
    if(table == NULL || table->entries == NULL || table->size <= 0){
        return NULL;
    }

    int index = key & (table->size - 1);
    TranspositionTableEntry* entry = &table->entries[index];
    
    if(entry->occupied && entry->key == key){
        return entry;
    }
    return NULL;
}

void tt_clear(TranspositionTable* table){
    if(table == NULL || table->entries == NULL || table->size <= 0){
        return;
    }

    memset(table->entries, 0, (table->size * sizeof(TranspositionTableEntry)));
    table->age = 0;
}

void tt_new_generation(TranspositionTable* table){
    if(table == NULL || table->entries == NULL || table->size <= 0){
        return;
    }

    if(table->age == UINT8_MAX){
        tt_clear(table);
        table->age = 1;
        return;
    }

    table->age++;
}

void tt_init(TranspositionTable* table){
    if(table == NULL){
        return;
    }

    table->entries = (TranspositionTableEntry*) calloc(TT_ENTRIES, sizeof(TranspositionTableEntry));
    if(table->entries == NULL){
        table->size = 0;
        return;
    }
    table->size = TT_ENTRIES;
    table->age = 0;
}

void tt_free(TranspositionTable* table){
    if(table == NULL){
        return;
    }

    if(table->entries){
        free(table->entries);
        table->entries = NULL;
    }
    table->size = 0;
}
