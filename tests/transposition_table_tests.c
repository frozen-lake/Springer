#include <stddef.h>
#include "tests.h"
#include "../src/transposition_table.h"

int test_create_table(){
	TranspositionTable table = (TranspositionTable){0};

	tt_init(&table);

	int success = (table.size == TT_ENTRIES) && (table.entries != NULL);
	if(table.entries != NULL){
		tt_free(&table);
	}
	return success;
}  

int test_add_get_entry(){
	TranspositionTable table = (TranspositionTable){0};
	tt_init(&table);

	uint64_t key = 0x1234ULL;
	Move move = (Move)(A2 | (A4 << 6) | (Pawn << 12));
	int score = 49000;
	int depth = 5;
	int flag = TT_EXACT;

	tt_add(&table, key, move, score, depth, flag);
	TranspositionTableEntry* entry = table_get(&table, key);

	int success = (entry != NULL)
		&& (entry->key == key)
		&& (entry->best_move == move)
		&& (entry->score == score)
		&& (entry->depth == depth)
		&& (entry->flag == flag)
		&& entry->occupied;

	tt_free(&table);
	return success;
}

int test_clear_table(){
	TranspositionTable table = (TranspositionTable){0};
	tt_init(&table);

	uint64_t key = 0xABCDEFULL;
	tt_add(&table, key, 0, 1, 1, TT_LOWER);
	tt_clear(&table);

	TranspositionTableEntry* entry = table_get(&table, key);
	int success = (entry == NULL);

	tt_free(&table);
	return success;
}

int test_zero_key_entry(){
	TranspositionTable table = (TranspositionTable){0};
	tt_init(&table);
	tt_add(&table, 0, 0, -49000, 3, TT_EXACT);

	TranspositionTableEntry* entry = table_get(&table, 0);
	int success = entry != NULL
		&& entry->occupied
		&& entry->key == 0
		&& entry->score == -49000;

	tt_free(&table);
	return success;
}

int test_unavailable_table_operations(){
	TranspositionTable table = (TranspositionTable){0};
	tt_add(NULL, 1, 0, 0, 0, TT_EXACT);
	tt_clear(NULL);
	tt_new_generation(NULL);
	tt_free(NULL);
	tt_add(&table, 1, 0, 0, 0, TT_EXACT);
	tt_clear(&table);
	tt_new_generation(&table);
	return table_get(NULL, 1) == NULL && table_get(&table, 1) == NULL;
}

int test_new_generation_stamps_entries(){
	TranspositionTable table = (TranspositionTable){0};
	tt_init(&table);
	tt_new_generation(&table);
	tt_new_generation(&table);
	tt_add(&table, 1, 0, 42, 4, TT_EXACT);

	TranspositionTableEntry* entry = table_get(&table, 1);
	int success = table.age == 2 && entry != NULL && entry->age == 2;

	tt_free(&table);
	return success;
}

int test_newer_generation_replaces_collision(){
	TranspositionTable table = (TranspositionTable){0};
	tt_init(&table);
	uint64_t first_key = 1;
	uint64_t second_key = UINT64_C(1) << 20 | 1;

	tt_new_generation(&table);
	tt_add(&table, first_key, 0, 100, 8, TT_EXACT);
	tt_new_generation(&table);
	tt_add(&table, second_key, 0, 200, 2, TT_EXACT);

	TranspositionTableEntry* entry = table_get(&table, second_key);
	int success = table_get(&table, first_key) == NULL
		&& entry != NULL
		&& entry->score == 200
		&& entry->depth == 2
		&& entry->age == 2;

	tt_free(&table);
	return success;
}

int test_generation_rollover_clears_table(){
	TranspositionTable table = (TranspositionTable){0};
	tt_init(&table);
	uint64_t old_key = 1;
	table.age = UINT8_MAX;
	tt_add(&table, old_key, 0, 42, 4, TT_EXACT);
	tt_new_generation(&table);

	int success = table.age == 1 && table_get(&table, old_key) == NULL;
	tt_add(&table, 2, 0, 84, 4, TT_EXACT);
	TranspositionTableEntry* entry = table_get(&table, 2);
	success = success && entry != NULL && entry->age == 1;

	tt_free(&table);
	return success;
}

int transposition_table_tests(){
	int num_tests = 8;

	int (*test_cases[num_tests])();
	char* test_case_names[num_tests];


	test_cases[0] = test_create_table;
	test_cases[1] = test_add_get_entry;
	test_cases[2] = test_clear_table;
	test_cases[3] = test_zero_key_entry;
	test_cases[4] = test_unavailable_table_operations;
	test_cases[5] = test_new_generation_stamps_entries;
	test_cases[6] = test_newer_generation_replaces_collision;
	test_cases[7] = test_generation_rollover_clears_table;

	test_case_names[0] = "test_create_table";
	test_case_names[1] = "test_add_get_entry";
	test_case_names[2] = "test_clear_table";
	test_case_names[3] = "test_zero_key_entry";
	test_case_names[4] = "test_unavailable_table_operations";
	test_case_names[5] = "test_new_generation_stamps_entries";
	test_case_names[6] = "test_newer_generation_replaces_collision";
	test_case_names[7] = "test_generation_rollover_clears_table";

    return run_tests(test_cases, test_case_names, num_tests);
}