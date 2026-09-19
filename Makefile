CC = gcc
CFLAGS = -g -Wall -Wextra
OBJ_DIR = obj
SRC_DIR = src
TEST_DIR = tests
BENCHMARK_DIR = benchmarks
BENCHMARK_OBJ_DIR = $(OBJ_DIR)/benchmarks
BENCHMARK_CFLAGS = $(CFLAGS) -DBENCHMARK_STATS

SRC_FILES = $(SRC_DIR)/board.c $(SRC_DIR)/game.c $(SRC_DIR)/move.c $(SRC_DIR)/attack_data.c $(SRC_DIR)/move_gen.c $(SRC_DIR)/search.c $(SRC_DIR)/transposition_table.c $(SRC_DIR)/uci_adapter.c
TEST_FILES = $(TEST_DIR)/attack_and_move_tests.c $(TEST_DIR)/move_gen_tests.c $(TEST_DIR)/tests.c $(TEST_DIR)/endgame_tests.c $(TEST_DIR)/search_tests.c $(TEST_DIR)/transposition_table_tests.c $(TEST_DIR)/perft_tests.c $(TEST_DIR)/quiescence_tests.c $(TEST_DIR)/uci_tests.c
BENCHMARK_FILES = $(BENCHMARK_DIR)/search_benchmarks.c

# notdir removes directory prefix, patsubst adds obj file directory and replaces .c with .o
SRC_OBJ_FILES = $(patsubst %.c, $(OBJ_DIR)/%.o, $(notdir $(SRC_FILES)))
TEST_OBJ_FILES = $(patsubst %.c, $(OBJ_DIR)/%.o, $(notdir $(TEST_FILES)))
BENCHMARK_SRC_OBJ_FILES = $(patsubst %.c, $(BENCHMARK_OBJ_DIR)/%.o, $(notdir $(SRC_FILES)))
BENCHMARK_OBJ_FILES = $(patsubst %.c, $(BENCHMARK_OBJ_DIR)/%.o, $(notdir $(BENCHMARK_FILES)))
GAME_OBJ_FILES = $(SRC_OBJ_FILES) obj/springer.o

# Default target
game: springer
tests: springer_tests
benchmarks: search_benchmarks

# Compile
$(OBJ_DIR)/%.o: $(SRC_DIR)/%.c | $(OBJ_DIR)
	$(CC) $(CFLAGS) -c $< -o $@

$(OBJ_DIR)/%.o: $(TEST_DIR)/%.c | $(OBJ_DIR)
	$(CC) $(CFLAGS) -c $< -o $@

$(OBJ_DIR)/%.o: $(BENCHMARK_DIR)/%.c | $(OBJ_DIR)
	$(CC) $(CFLAGS) -c $< -o $@

$(BENCHMARK_OBJ_DIR)/%.o: $(SRC_DIR)/%.c | $(BENCHMARK_OBJ_DIR)
	$(CC) $(BENCHMARK_CFLAGS) -c $< -o $@

$(BENCHMARK_OBJ_DIR)/%.o: $(BENCHMARK_DIR)/%.c | $(BENCHMARK_OBJ_DIR)
	$(CC) $(BENCHMARK_CFLAGS) -c $< -o $@

$(OBJ_DIR):
	mkdir -p $(OBJ_DIR)

$(BENCHMARK_OBJ_DIR):
	mkdir -p $(BENCHMARK_OBJ_DIR)

# Link
springer_tests: $(TEST_OBJ_FILES) $(SRC_OBJ_FILES)
	$(CC) -o $@ $^

springer: $(GAME_OBJ_FILES)
	$(CC) -o $@ $^

search_benchmarks: $(BENCHMARK_OBJ_FILES) $(BENCHMARK_SRC_OBJ_FILES)
	$(CC) -o $@ $^

# Clean
clean:
	rm -rf $(OBJ_DIR) springer_tests springer search_benchmarks springer_tests.exe springer.exe search_benchmarks.exe