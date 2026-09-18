#ifndef SEARCH_H
#define SEARCH_H

#include "transposition_table.h"

#define MAX_SEARCH_PLY 128
#define MAX_QUIESCENCE_PLY 10
#define INF 50000
#define MATE_SCORE 49000

typedef struct SearchState SearchState;

struct SearchState {
    TranspositionTable* tt;
    int nodes;
    int alpha_beta_nodes;
    int quiescence_nodes;
    int tt_probes;
    int tt_hits;
    int tt_exact_cutoffs;
    int tt_bound_cutoffs;
    int root_depth;
    int max_depth;
    int* stop;
    Move pv_table[MAX_SEARCH_PLY][MAX_SEARCH_PLY];
    int pv_lengths[MAX_SEARCH_PLY];
};

Move search_best_move(Game* game, SearchState* search_state);
Move search_root(Game* game, SearchState* search_state, int depth);
int evaluate(Game* game);
int alpha_beta(SearchState* search_state, Game* game, int alpha, int beta, int depth_remaining, int ply);
int quiesce(SearchState* search_state, Game* game, int alpha, int beta, int ply, int qply);
int score_to_tt(int score, int ply);
int score_from_tt(int score, int ply);

int initialize_searchstate(SearchState* search_state, TranspositionTable* tt, int max_depth, int* stop);
void destroy_searchstate(SearchState* search_state);
void reset_searchstate(SearchState* search_state);
void request_search_stop(SearchState* search_state);
void clear_search_stop(SearchState* search_state);
int is_search_stop_requested(SearchState* search_state);

#endif