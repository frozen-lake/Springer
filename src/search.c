#include "search.h"
#include "move_gen.h"
#include "board.h"
#include "game.h"

static int PIECE_VALUES[8] = {0, 0, 100, 300, 300, 500, 900, 10000};
#define DELTA_PRUNING_MARGIN 100

#ifdef BENCHMARK_STATS
#define SEARCH_STAT_INC(search_state, field) ((search_state)->field++)
#else
#define SEARCH_STAT_INC(search_state, field) ((void)0)
#endif

static void clear_pv_storage(SearchState* search_state){
    if(search_state == NULL){
        return;
    }

    for(int ply = 0; ply < MAX_SEARCH_PLY; ply++){
        search_state->pv_lengths[ply] = 0;
        for(int j = 0; j < MAX_SEARCH_PLY; j++){
            search_state->pv_table[ply][j] = 0;
        }
    }
}

Move search_best_move(Game* game, SearchState* search_state){
    if(game == NULL || search_state == NULL){
        return 0;
    }

    tt_new_generation(search_state->tt);

    int depth = search_state->root_depth;
    if(depth <= 0){
        depth = search_state->max_depth;
    }
    if(depth <= 0){
        depth = 1;
    }
    if(depth > search_state->max_depth){
        depth = search_state->max_depth;
    }

    return search_root(game, search_state, depth);
}

Move search_root(Game* game, SearchState* search_state, int depth){
    if(game == NULL || search_state == NULL){
        return 0;
    }

    if(depth <= 0){
        depth = 1;
    }
    if(depth > search_state->max_depth){
        depth = search_state->max_depth;
    }

    search_state->root_depth = depth;
    search_state->nodes = 0;
#ifdef BENCHMARK_STATS
    search_state->alpha_beta_nodes = 0;
    search_state->quiescence_nodes = 0;
    search_state->tt_probes = 0;
    search_state->tt_hits = 0;
    search_state->tt_exact_cutoffs = 0;
    search_state->tt_bound_cutoffs = 0;
#endif
    clear_pv_storage(search_state);

    MoveList moves;
    generate_all_moves(&moves, game, game->state.side_to_move);
    filter_legal_moves(&moves, game);
    order_moves(&moves, game);

    if(moves.size == 0){
        return 0;
    }

    Move best_move = moves.moves[0];
    int best = -INF;

    for(int i = 0; i < moves.size; i++){
        Move move = moves.moves[i];

        UndoInfo undo;
        make_move_on_state(&game->state, move, &undo);

        int score = -alpha_beta(search_state, game, -INF, INF, depth - 1, 1);

        unmake_move_on_state(&game->state, move, &undo);

        if(score > best){
            best = score;
            best_move = move;

            search_state->pv_table[0][0] = move;
            for(int j = 0; j < search_state->pv_lengths[1] && (j + 1) < MAX_SEARCH_PLY; j++){
                search_state->pv_table[0][j + 1] = search_state->pv_table[1][j];
            }
            search_state->pv_lengths[0] = 1 + search_state->pv_lengths[1];
            if(search_state->pv_lengths[0] > MAX_SEARCH_PLY){
                search_state->pv_lengths[0] = MAX_SEARCH_PLY;
            }
        }

        if(is_search_stop_requested(search_state)){
            break;
        }
    }

    if(best_move == 0){
        search_state->pv_lengths[0] = 0;
    }

    return best_move;
}

int initialize_searchstate(SearchState* search_state, TranspositionTable* tt, int max_depth, int* stop){
    if(search_state == NULL){
        return 0;
    }

    search_state->tt = tt;
    search_state->nodes = 0;
    search_state->root_depth = 0;
    search_state->stop = stop;
    clear_pv_storage(search_state);

    if(max_depth <= 0){
        search_state->max_depth = MAX_SEARCH_PLY;
    } else if(max_depth > MAX_SEARCH_PLY){
        search_state->max_depth = MAX_SEARCH_PLY;
    } else {
        search_state->max_depth = max_depth;
    }

    return 1;
}

void destroy_searchstate(SearchState* search_state){
    (void)search_state;
}

void reset_searchstate(SearchState* search_state){
    if(search_state == NULL){
        return;
    }
    search_state->nodes = 0;
#ifdef BENCHMARK_STATS
    search_state->alpha_beta_nodes = 0;
    search_state->quiescence_nodes = 0;
    search_state->tt_probes = 0;
    search_state->tt_hits = 0;
    search_state->tt_exact_cutoffs = 0;
    search_state->tt_bound_cutoffs = 0;
#endif
    search_state->root_depth = 0;
    clear_pv_storage(search_state);
}

void request_search_stop(SearchState* search_state){
    if(search_state == NULL || search_state->stop == NULL){
        return;
    }
    *(search_state->stop) = 1;
}

void clear_search_stop(SearchState* search_state){
    if(search_state == NULL || search_state->stop == NULL){
        return;
    }
    *(search_state->stop) = 0;
}

int is_search_stop_requested(SearchState* search_state){
    if(search_state == NULL || search_state->stop == NULL){
        return 0;
    }
    return *(search_state->stop) != 0;
}

int evaluate(Game* game){
    Board* board = &game->state;
    int score = 0;
    for(int i=0;i<64;i++){
        uint64_t mask = U64_MASK(i);
        if(board->pieces[White] & mask){
            int piece = position_to_piece_number(board, i);
            score += PIECE_VALUES[piece];
        } else if(board->pieces[Black] & mask){
            int piece = position_to_piece_number(board, i);
            score -= PIECE_VALUES[piece];
        }
    }

    return game->state.side_to_move == White ? score : -score;
}

int score_to_tt(int score, int ply){
    if(score > MATE_SCORE - MAX_SEARCH_PLY){
        return score + ply;
    }
    if(score < -MATE_SCORE + MAX_SEARCH_PLY){
        return score - ply;
    }
    return score;
}

int score_from_tt(int score, int ply){
    if(score > MATE_SCORE - MAX_SEARCH_PLY){
        return score - ply;
    }
    if(score < -MATE_SCORE + MAX_SEARCH_PLY){
        return score + ply;
    }
    return score;
}

static void prioritize_move(MoveList* moves, Move move){
    if(moves == NULL || move == 0){
        return;
    }

    for(int i = 0; i < moves->size; i++){
        if(moves->moves[i] == move){
            Move first_move = moves->moves[0];
            moves->moves[0] = move;
            moves->moves[i] = first_move;
            return;
        }
    }
}

int quiesce(SearchState* search_state, Game* game, int alpha, int beta, int ply, int qply){
    if(game == NULL){
        return 0;
    }

    if(search_state != NULL){
        search_state->nodes += 1;
        SEARCH_STAT_INC(search_state, quiescence_nodes);
    }
    if(is_search_stop_requested(search_state)){
        return evaluate(game);
    }

    int color = game->state.side_to_move;
    int in_check = square_attacked(&game->state,
        game->state.king_sq[color], !color);

    MoveList moves;
    generate_all_moves(&moves, game, color);
    filter_legal_moves(&moves, game);
    order_moves(&moves, game);

    if(moves.size == 0){
        return in_check ? -MATE_SCORE + ply : 0;
    }

    if(qply >= MAX_QUIESCENCE_PLY){
        return evaluate(game);
    }

    int best = -INF;
    if(!in_check){
        best = evaluate(game);
        if(best >= beta){
            return best;
        }
        if(best > alpha){
            alpha = best;
        }
    }

    for(int i = 0; i < moves.size; i++){
        Move move = moves.moves[i];
        int capture = get_move_capture(move);
        int promotion = get_move_promotion(move);
        int is_tactical = capture != 0 || promotion != NO_PROMOTION;
        if(!in_check && !is_tactical && qply > 0){
            continue;
        }

        UndoInfo undo;
        make_move_on_state(&game->state, move, &undo);
        int gives_check = square_attacked(&game->state,
            game->state.king_sq[game->state.side_to_move],
            !game->state.side_to_move);
        if(!in_check && !is_tactical && !gives_check){
            unmake_move_on_state(&game->state, move, &undo);
            continue;
        }
        if(!in_check && capture != 0 && promotion == NO_PROMOTION
            && !gives_check && best + PIECE_VALUES[capture]
                + DELTA_PRUNING_MARGIN <= alpha){
            unmake_move_on_state(&game->state, move, &undo);
            continue;
        }
        int score = -quiesce(search_state, game, -beta, -alpha, ply + 1, qply + 1);
        unmake_move_on_state(&game->state, move, &undo);

        if(score > best){
            best = score;
        }
        if(score > alpha){
            alpha = score;
        }
        if(alpha >= beta){
            break;
        }
        if(is_search_stop_requested(search_state)){
            break;
        }
    }

    return best;
}

int alpha_beta(SearchState* search_state, Game* game, int alpha, int beta, int depth_remaining, int ply){
    if(search_state != NULL){
        search_state->nodes += 1;
        SEARCH_STAT_INC(search_state, alpha_beta_nodes);
        if(ply >= 0 && ply < MAX_SEARCH_PLY){
            search_state->pv_lengths[ply] = 0;
        }
    }

    if(is_search_stop_requested(search_state)){
        return evaluate(game);
    }

    if(depth_remaining == 0){
        return quiesce(search_state, game, alpha, beta, ply, 0);
    }

    int alpha_original = alpha;
    int beta_original = beta;
    Move tt_move = 0;
    uint64_t key = game->state.zobrist_hash;
    TranspositionTableEntry* entry = NULL;
    if(search_state != NULL && search_state->tt != NULL){
        SEARCH_STAT_INC(search_state, tt_probes);
        entry = table_get(search_state->tt, key);
    }
    if(entry != NULL){
        SEARCH_STAT_INC(search_state, tt_hits);
        tt_move = entry->best_move;
        if(entry->depth >= depth_remaining){
            int score = score_from_tt(entry->score, ply);
            if(entry->flag == TT_EXACT){
                SEARCH_STAT_INC(search_state, tt_exact_cutoffs);
                return score;
            }
            if(entry->flag == TT_LOWER && score > alpha){
                alpha = score;
            } else if(entry->flag == TT_UPPER && score < beta){
                beta = score;
            }
            if(alpha >= beta){
                SEARCH_STAT_INC(search_state, tt_bound_cutoffs);
                return score;
            }
        }
    }

    MoveList moves;
    generate_all_moves(&moves, game, game->state.side_to_move);
    filter_legal_moves(&moves, game);
    order_moves(&moves, game);
	prioritize_move(&moves, tt_move);

    if(moves.size == 0){
        int in_check = square_attacked(&game->state, game->state.king_sq[game->state.side_to_move], !game->state.side_to_move);
		int score = in_check ? (-MATE_SCORE + ply) : 0;
		if(search_state != NULL){
			tt_add(search_state->tt, key, 0, score_to_tt(score, ply),
				depth_remaining, TT_EXACT);
		}
		return score;
    }

    int best = -INF;
	Move best_move = 0;

    for(int i=0; i<moves.size; i++){
        Move move = moves.moves[i];
        UndoInfo undo;
        make_move_on_state(&game->state, move, &undo);

        int score = -alpha_beta(search_state, game, -beta, -alpha, depth_remaining - 1, ply + 1);

        unmake_move_on_state(&game->state, move, &undo);
        if(is_search_stop_requested(search_state)){
            return best;
        }

        if(score > best){
            best = score;
            best_move = move;

            if(search_state != NULL && ply >= 0 && ply < MAX_SEARCH_PLY){
                int child_ply = ply + 1;
                int child_len = 0;
                search_state->pv_table[ply][0] = move;

                if(child_ply < MAX_SEARCH_PLY){
                    child_len = search_state->pv_lengths[child_ply];
                    for(int j = 0; j < child_len && (j + 1) < MAX_SEARCH_PLY; j++){
                        search_state->pv_table[ply][j + 1] = search_state->pv_table[child_ply][j];
                    }
                }

                search_state->pv_lengths[ply] = 1 + child_len;
                if(search_state->pv_lengths[ply] > MAX_SEARCH_PLY){
                    search_state->pv_lengths[ply] = MAX_SEARCH_PLY;
                }
            }
        }
        if(score > alpha) alpha = score;
        if(alpha >= beta) break; // prune
    }

    	if(search_state != NULL){
    		int flag = best <= alpha_original ? TT_UPPER
    			: best >= beta_original ? TT_LOWER : TT_EXACT;
    		tt_add(search_state->tt, key, best_move, score_to_tt(best, ply),
    			depth_remaining, flag);
    	}
    return best;
}
