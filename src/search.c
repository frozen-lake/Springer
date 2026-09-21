#include "search.h"
#include "move_gen.h"
#include "board.h"
#include "game.h"

static int PIECE_VALUES[8] = {0, 0, 100, 300, 300, 500, 900, 10000};
#define DELTA_PRUNING_MARGIN 100

typedef enum {
    DRAW_REASON_NONE = 0,
    DRAW_REASON_THREEFOLD,
    DRAW_REASON_FIFTY_MOVE,
    DRAW_REASON_INSUFFICIENT_MATERIAL,
} DrawReason;

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

static void prioritize_move(MoveList* moves, Move move);
static Move pick_fallback_legal_move(Game* game);
static Move search_root_internal(Game* game, SearchState* search_state, int depth,
    Move preferred_root_move, int* iteration_completed);
static void clear_draw_path_storage(SearchState* search_state);
static void draw_path_push(SearchState* search_state, uint64_t position_hash,
    int edge_irreversible);
static void draw_path_pop(SearchState* search_state);
static int count_occurrences_in_search_context(SearchState* search_state, Game* game);
static DrawReason get_search_draw_reason(SearchState* search_state, Game* game);
static int move_is_irreversible(Move move, const UndoInfo* undo, const BoardState* state_after);
static int quiesce_internal(SearchState* search_state, Game* game, int alpha, int beta,
    int ply, int qply, int entered_from_move, int edge_irreversible);
static int alpha_beta_internal(SearchState* search_state, Game* game, int alpha, int beta,
    int depth_remaining, int ply, int entered_from_move, int edge_irreversible);

static void clear_draw_path_storage(SearchState* search_state){
    if(search_state == NULL){
        return;
    }

    search_state->draw_path_length = 0;
}

static void draw_path_push(SearchState* search_state, uint64_t position_hash,
    int edge_irreversible){
    int index;

    if(search_state == NULL || search_state->draw_path_length >= MAX_SEARCH_PLY){
        return;
    }

    index = search_state->draw_path_length;
    search_state->draw_path_hashes[index] = position_hash;
    search_state->draw_path_edge_irreversible[index] = edge_irreversible != 0;
    search_state->draw_path_length += 1;
}

static void draw_path_pop(SearchState* search_state){
    if(search_state == NULL || search_state->draw_path_length <= 0){
        return;
    }

    search_state->draw_path_length -= 1;
}

static int count_occurrences_in_search_context(SearchState* search_state, Game* game){
    int occurrences = 0;
    uint64_t key;

    if(game == NULL){
        return 0;
    }

    key = game->state.zobrist_hash;

    if(game->position_history != NULL){
        int halfmove_clock = (int)game->state.halfmove_clock;
        int start;
        if(halfmove_clock < 0){
            halfmove_clock = 0;
        }
        if(halfmove_clock > game->game_ply){
            halfmove_clock = game->game_ply;
        }
        start = game->game_ply - halfmove_clock;

        for(int i = start; i <= game->game_ply; i++){
            if(game->position_history[i] == key){
                occurrences++;
            }
        }
    }

    if(search_state != NULL){
        int start = 0;
        if(search_state->draw_path_length > 0){
            for(int i = search_state->draw_path_length - 1; i >= 0; i--){
                if(search_state->draw_path_edge_irreversible[i]){
                    start = i;
                    break;
                }
            }
        }

        for(int i = start; i < search_state->draw_path_length; i++){
            if(search_state->draw_path_hashes[i] == key){
                occurrences++;
            }
        }
    }

    return occurrences;
}

static DrawReason get_search_draw_reason(SearchState* search_state, Game* game){
    if(game == NULL){
        return DRAW_REASON_NONE;
    }

    if(game->state.halfmove_clock >= 100){
        return DRAW_REASON_FIFTY_MOVE;
    }

    if(has_insufficient_material(&game->state)){
        return DRAW_REASON_INSUFFICIENT_MATERIAL;
    }

    if(count_occurrences_in_search_context(search_state, game) >= 3){
        return DRAW_REASON_THREEFOLD;
    }

    return DRAW_REASON_NONE;
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

    search_state->completed_depth = 0;
    search_state->completed_iterations = 0;
    search_state->nodes = 0;
    search_state->alpha_beta_nodes = 0;
    search_state->quiescence_nodes = 0;
    search_state->tt_probes = 0;
    search_state->tt_hits = 0;
    search_state->tt_exact_cutoffs = 0;
    search_state->tt_bound_cutoffs = 0;

    int cumulative_nodes = 0;
    int cumulative_alpha_beta_nodes = 0;
    int cumulative_quiescence_nodes = 0;
    int cumulative_tt_probes = 0;
    int cumulative_tt_hits = 0;
    int cumulative_tt_exact_cutoffs = 0;
    int cumulative_tt_bound_cutoffs = 0;

    Move best_move = 0;
    Move preferred_root_move = 0;

    for(int current_depth = 1; current_depth <= depth; current_depth++){
        int iteration_completed = 0;

        Move iteration_best_move = search_root_internal(game, search_state,
            current_depth, preferred_root_move, &iteration_completed);
        if(iteration_best_move == 0){
            break;
        }

        if(!iteration_completed){
            break;
        }

        cumulative_nodes += search_state->nodes;
        cumulative_alpha_beta_nodes += search_state->alpha_beta_nodes;
        cumulative_quiescence_nodes += search_state->quiescence_nodes;
        cumulative_tt_probes += search_state->tt_probes;
        cumulative_tt_hits += search_state->tt_hits;
        cumulative_tt_exact_cutoffs += search_state->tt_exact_cutoffs;
        cumulative_tt_bound_cutoffs += search_state->tt_bound_cutoffs;

        best_move = iteration_best_move;
        if(search_state->pv_lengths[0] > 0){
            preferred_root_move = search_state->pv_table[0][0];
        } else {
            preferred_root_move = iteration_best_move;
        }
        search_state->completed_depth = current_depth;
        search_state->completed_iterations += 1;

        if(is_search_stop_requested(search_state)){
            break;
        }
    }

    search_state->nodes = cumulative_nodes;
    search_state->alpha_beta_nodes = cumulative_alpha_beta_nodes;
    search_state->quiescence_nodes = cumulative_quiescence_nodes;
    search_state->tt_probes = cumulative_tt_probes;
    search_state->tt_hits = cumulative_tt_hits;
    search_state->tt_exact_cutoffs = cumulative_tt_exact_cutoffs;
    search_state->tt_bound_cutoffs = cumulative_tt_bound_cutoffs;
    search_state->root_depth = search_state->completed_depth;

    if(best_move == 0){
        best_move = pick_fallback_legal_move(game);
        search_state->root_depth = 0;
    }

    return best_move;
}

static Move pick_fallback_legal_move(Game* game){
    if(game == NULL){
        return 0;
    }

    MoveList moves;
    generate_all_moves(&moves, game, game->state.side_to_move);
    filter_legal_moves(&moves, game);
    order_moves(&moves, game);

    if(moves.size == 0){
        return 0;
    }

    return moves.moves[0];
}

Move search_root(Game* game, SearchState* search_state, int depth){
    return search_root_internal(game, search_state, depth, 0, NULL);
}

static Move search_root_internal(Game* game, SearchState* search_state, int depth,
    Move preferred_root_move, int* iteration_completed){
    if(game == NULL || search_state == NULL){
        if(iteration_completed != NULL){
            *iteration_completed = 0;
        }
        return 0;
    }

    if(iteration_completed != NULL){
        *iteration_completed = 0;
    }

    if(depth <= 0){
        depth = 1;
    }
    if(depth > search_state->max_depth){
        depth = search_state->max_depth;
    }

    search_state->root_depth = depth;
    search_state->nodes = 0;
    search_state->alpha_beta_nodes = 0;
    search_state->quiescence_nodes = 0;
    search_state->tt_probes = 0;
    search_state->tt_hits = 0;
    search_state->tt_exact_cutoffs = 0;
    search_state->tt_bound_cutoffs = 0;
    clear_draw_path_storage(search_state);
    clear_pv_storage(search_state);

    MoveList moves;
    generate_all_moves(&moves, game, game->state.side_to_move);
    filter_legal_moves(&moves, game);
    order_moves(&moves, game);
    prioritize_move(&moves, preferred_root_move);

    if(moves.size == 0){
        if(iteration_completed != NULL){
            *iteration_completed = 1;
        }
        return 0;
    }

    Move best_move = moves.moves[0];
    int best = -INF;
    int interrupted = 0;

    for(int i = 0; i < moves.size; i++){
        Move move = moves.moves[i];

        UndoInfo undo;
        make_move_on_state(&game->state, move, &undo);
        int edge_irreversible = move_is_irreversible(move, &undo, &game->state);

        int score = -alpha_beta_internal(search_state, game, -INF, INF,
            depth - 1, 1, 1, edge_irreversible);

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
            interrupted = 1;
            break;
        }
    }

    if(iteration_completed != NULL){
        *iteration_completed = !interrupted;
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
    search_state->completed_depth = 0;
    search_state->completed_iterations = 0;
    search_state->stop = stop;
    search_state->alpha_beta_nodes = 0;
    search_state->quiescence_nodes = 0;
    search_state->tt_probes = 0;
    search_state->tt_hits = 0;
    search_state->tt_exact_cutoffs = 0;
    search_state->tt_bound_cutoffs = 0;
    clear_draw_path_storage(search_state);
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
    search_state->alpha_beta_nodes = 0;
    search_state->quiescence_nodes = 0;
    search_state->tt_probes = 0;
    search_state->tt_hits = 0;
    search_state->tt_exact_cutoffs = 0;
    search_state->tt_bound_cutoffs = 0;
    search_state->root_depth = 0;
    search_state->completed_depth = 0;
    search_state->completed_iterations = 0;
    clear_draw_path_storage(search_state);
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

static int move_is_irreversible(Move move, const UndoInfo* undo, const BoardState* state_after){
    if(get_move_piece(move) == Pawn || get_move_capture(move) != 0){
        return 1;
    }

    if(undo != NULL && state_after != NULL
        && undo->castling_rights != state_after->castling_rights){
        return 1;
    }

    return 0;
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
    return quiesce_internal(search_state, game, alpha, beta, ply, qply, 0, 0);
}

static int quiesce_internal(SearchState* search_state, Game* game, int alpha, int beta,
    int ply, int qply, int entered_from_move, int edge_irreversible){
    int pushed_context = 0;
    int result = 0;
    DrawReason draw_reason;
    int done = 0;

    if(game == NULL){
        return 0;
    }

    if(search_state != NULL){
        search_state->nodes += 1;
        search_state->quiescence_nodes += 1;
        if(entered_from_move){
            draw_path_push(search_state, game->state.zobrist_hash, edge_irreversible);
            pushed_context = 1;
        }
    }

    if(is_search_stop_requested(search_state)){
        result = evaluate(game);
        done = 1;
    }

    if(!done){
        draw_reason = get_search_draw_reason(search_state, game);
        if(draw_reason != DRAW_REASON_NONE){
            result = 0;
            done = 1;
        }
    }

    if(!done){
        int color = game->state.side_to_move;
        int in_check = square_attacked(&game->state, game->state.king_sq[color], !color);

        MoveList moves;
        generate_all_moves(&moves, game, color);
        filter_legal_moves(&moves, game);
        order_moves(&moves, game);

        if(moves.size == 0){
            result = in_check ? -MATE_SCORE + ply : 0;
            done = 1;
        }

        if(!done && qply >= MAX_QUIESCENCE_PLY){
            result = evaluate(game);
            done = 1;
        }

        if(!done){
            int best = -INF;
            if(!in_check){
                best = evaluate(game);
                if(best >= beta){
                    result = best;
                    done = 1;
                }
                if(!done && best > alpha){
                    alpha = best;
                }
            }

            if(!done){
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
                    int child_edge_irreversible = move_is_irreversible(move, &undo, &game->state);
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
                    int score = -quiesce_internal(search_state, game, -beta, -alpha,
                        ply + 1, qply + 1, 1, child_edge_irreversible);
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

                result = best;
            }
        }
    }

    if(search_state != NULL && pushed_context){
        draw_path_pop(search_state);
    }

    return result;
}

int alpha_beta(SearchState* search_state, Game* game, int alpha, int beta, int depth_remaining, int ply){
    return alpha_beta_internal(search_state, game, alpha, beta, depth_remaining, ply, 0, 0);
}

static int alpha_beta_internal(SearchState* search_state, Game* game, int alpha, int beta,
    int depth_remaining, int ply, int entered_from_move, int edge_irreversible){
    uint64_t key = game->state.zobrist_hash;
    int pushed_context = 0;
    int result = 0;
    DrawReason draw_reason;
    int done = 0;

    if(search_state != NULL){
        search_state->nodes += 1;
        search_state->alpha_beta_nodes += 1;
        if(entered_from_move){
            draw_path_push(search_state, key, edge_irreversible);
            pushed_context = 1;
        }
        if(ply >= 0 && ply < MAX_SEARCH_PLY){
            search_state->pv_lengths[ply] = 0;
        }
    }

    if(is_search_stop_requested(search_state)){
        result = evaluate(game);
        done = 1;
    }

    if(!done){
        draw_reason = get_search_draw_reason(search_state, game);
        if(draw_reason != DRAW_REASON_NONE){
            result = 0;
            done = 1;
        }
    }

    if(!done && depth_remaining == 0){
        result = quiesce_internal(search_state, game, alpha, beta, ply, 0, 0, 0);
        done = 1;
    }

    if(!done){
        int alpha_original = alpha;
        int beta_original = beta;
        Move tt_move = 0;
        MoveList moves;
        TranspositionTableEntry* entry = NULL;
        if(search_state != NULL && search_state->tt != NULL){
            search_state->tt_probes += 1;
            entry = table_get(search_state->tt, key);
        }
        if(entry != NULL){
            search_state->tt_hits += 1;
            tt_move = entry->best_move;
            if(entry->depth >= depth_remaining){
                int score = score_from_tt(entry->score, ply);
                if(entry->flag == TT_EXACT){
                    search_state->tt_exact_cutoffs += 1;
                    result = score;
                    done = 1;
                } else {
                    if(entry->flag == TT_LOWER && score > alpha){
                        alpha = score;
                    } else if(entry->flag == TT_UPPER && score < beta){
                        beta = score;
                    }
                    if(alpha >= beta){
                        search_state->tt_bound_cutoffs += 1;
                        result = score;
                        done = 1;
                    }
                }
            }
        }

        if(!done){
            generate_all_moves(&moves, game, game->state.side_to_move);
            filter_legal_moves(&moves, game);
            order_moves(&moves, game);
            prioritize_move(&moves, tt_move);

            if(moves.size == 0){
                int in_check = square_attacked(&game->state,
                    game->state.king_sq[game->state.side_to_move], !game->state.side_to_move);
                int score = in_check ? (-MATE_SCORE + ply) : 0;
                if(search_state != NULL && search_state->tt != NULL){
                    tt_add(search_state->tt, key, 0, score_to_tt(score, ply),
                        depth_remaining, TT_EXACT);
                }
                result = score;
                done = 1;
            }
        }

        if(!done){
            int best = -INF;
            Move best_move = 0;

            for(int i = 0; i < moves.size; i++){
                Move move = moves.moves[i];
                UndoInfo undo;
                make_move_on_state(&game->state, move, &undo);
                int child_edge_irreversible = move_is_irreversible(move, &undo, &game->state);

                int score = -alpha_beta_internal(search_state, game, -beta, -alpha,
                    depth_remaining - 1, ply + 1, 1, child_edge_irreversible);

                unmake_move_on_state(&game->state, move, &undo);
                if(is_search_stop_requested(search_state)){
                    result = best;
                    done = 1;
                    break;
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
                if(score > alpha){
                    alpha = score;
                }
                if(alpha >= beta){
                    break;
                }
            }

            if(!done){
                if(search_state != NULL && search_state->tt != NULL){
                    int flag = best <= alpha_original ? TT_UPPER
                        : best >= beta_original ? TT_LOWER : TT_EXACT;
                    if(flag == TT_EXACT && best == 0){
                        flag = TT_UPPER;
                    }
                    tt_add(search_state->tt, key, best_move, score_to_tt(best, ply),
                        depth_remaining, flag);
                }
                result = best;
            }
        }
    }

    if(search_state != NULL && pushed_context){
        draw_path_pop(search_state);
    }

    return result;
}
