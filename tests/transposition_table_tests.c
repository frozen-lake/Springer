#include <stddef.h>
#include "tests.h"
#include "../src/transposition_table.h"

int test_create_table(){
	TranspositionTable table = (TranspositionTable){0};

	table_init(&table);

	int success = (table.size == TT_ENTRIES) && (table.entries != NULL);
	if(table.entries != NULL){
		table_free(&table);
	}
	return success;
}  

int test_add_get_entry(){
	TranspositionTable table = (TranspositionTable){0};
	table_init(&table);

	uint64_t key = 0x1234ULL;
	Move move = (Move)(A2 | (A4 << 6) | (Pawn << 12));
	int score = 42;
	int depth = 5;
	int flag = TT_EXACT;

	table_add(&table, key, move, score, depth, flag);
	TranspositionTableEntry* entry = table_get(&table, key);

	int success = (entry != NULL)
		&& (entry->key == key)
		&& (entry->best_move == move)
		&& (entry->score == score)
		&& (entry->depth == depth)
		&& (entry->flag == flag);

	table_free(&table);
	return success;
}

int test_clear_table(){
	TranspositionTable table = (TranspositionTable){0};
	table_init(&table);

	uint64_t key = 0xABCDEFULL;
	table_add(&table, key, 0, 1, 1, TT_LOWER);
	table_clear(&table);

	TranspositionTableEntry* entry = table_get(&table, key);
	int success = (entry == NULL);

	table_free(&table);
	return success;
}

int transposition_table_tests(){
	int num_tests = 3;

	int (*test_cases[num_tests])();
	char* test_case_names[num_tests];


	test_cases[0] = test_create_table;
	test_cases[1] = test_add_get_entry;
	test_cases[2] = test_clear_table;

	test_case_names[0] = "test_create_table";
	test_case_names[1] = "test_add_get_entry";
	test_case_names[2] = "test_clear_table";

    return run_tests(test_cases, test_case_names, num_tests);
}