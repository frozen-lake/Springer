#include "move_gen.h"
#include "game.h"


void add_promotions(MoveList* move_list, int src, int dest, Board* state){
    move_list_add(move_list, encode_promotion(src, dest, state, KNIGHT_PROMOTION));
    move_list_add(move_list, encode_promotion(src, dest, state, BISHOP_PROMOTION));
    move_list_add(move_list, encode_promotion(src, dest, state, ROOK_PROMOTION));
    move_list_add(move_list, encode_promotion(src, dest, state, QUEEN_PROMOTION)); 
}

void generate_moves_from_bitboard(int src, uint64_t attack, MoveList* move_list, Game* game, int color){
    Board* board = &game->state;
    while(attack){
        int dest = get_lsb_index(attack);
        attack &= attack - 1;

        if(board->pieces[color] & U64_MASK(dest)){
            continue;
        }

        Move move = encode_move(src, dest, &game->state);
        move_list_add(move_list, move);
    }
}

void generate_knight_moves(MoveList* move_list, Game* game, int color){
    Board* board = &game->state;
    uint64_t knights = board->pieces[Knight] & board->pieces[color];
    while(knights){
        int src = get_lsb_index(knights);
        knights &= knights - 1;
        uint64_t attack = get_knight_attacks(src);
        generate_moves_from_bitboard(src, attack, move_list, game, color);
    }
}
void generate_bishop_moves(MoveList* move_list, Game* game, int color){
    Board* board = &game->state;
    uint64_t occupancy = board->pieces[White] | board->pieces[Black];
    uint64_t bishops = board->pieces[Bishop] & board->pieces[color];
    while(bishops){
        int src = get_lsb_index(bishops);
        bishops &= bishops - 1;
        uint64_t attack = get_bishop_attacks(src, occupancy);
        generate_moves_from_bitboard(src, attack, move_list, game, color);
    }
}
void generate_rook_moves(MoveList* move_list, Game* game, int color){
    Board* board = &game->state;
    uint64_t occupancy = board->pieces[White] | board->pieces[Black];
    uint64_t rooks = board->pieces[Rook] & board->pieces[color];
    while(rooks){
        int src = get_lsb_index(rooks);
        rooks &= rooks - 1;
        uint64_t attack = get_rook_attacks(src, occupancy);
        generate_moves_from_bitboard(src, attack, move_list, game, color);
    }
}
void generate_queen_moves(MoveList* move_list, Game* game, int color){
    Board* board = &game->state;
    uint64_t occupancy = board->pieces[White] | board->pieces[Black];
    uint64_t queens = board->pieces[Queen] & board->pieces[color];
    while(queens){
        int src = get_lsb_index(queens);
        queens &= queens - 1;
        uint64_t attack = get_queen_attacks(src, occupancy);
        generate_moves_from_bitboard(src, attack, move_list, game, color);
    }
}

void generate_pawn_moves(MoveList* move_list, Game* game, int color){
    /* Captures */
    Board* board = &game->state;
    uint64_t pawns = board->pieces[Pawn] & board->pieces[color];

    uint64_t occupancy = board->pieces[White] | board->pieces[Black];


    while(pawns) {
        int src = get_lsb_index(pawns);
        pawns &= pawns - 1;

        int left_capture = color?src+7:src-9;
        int right_capture = color?src+9:src-7;
        int forward = color?src+8:src-8;
        int double_forward = color?src+16:src-16;
        int is_start_rank = color ? ((src / 8) == 1) : ((src / 8) == 6);
        int promotion_rank = color ? 7 : 0;

        /* Captures */
        if((src % 8 > 0) && (U64_MASK(left_capture) & board->pieces[!color])){
            if((left_capture / 8) == promotion_rank){
                add_promotions(move_list, src, left_capture, &game->state);
            } else {
                Move move = encode_move(src, left_capture, &game->state);
                move_list_add(move_list, move);
            }
        }
        if((src % 8 < 7) && (U64_MASK(right_capture) & board->pieces[!color])){
            if((right_capture / 8) == promotion_rank){
                add_promotions(move_list, src, right_capture, &game->state);
            } else {
                Move move = encode_move(src, right_capture, &game->state);
                move_list_add(move_list, move);
            }
        }

        /* Forward moves */
        if(!(U64_MASK(forward) & occupancy)){
            if((forward / 8) == promotion_rank){
                add_promotions(move_list, src, forward, &game->state);
            } else {
                Move move = encode_move(src, forward, &game->state);
                move_list_add(move_list, move);
            }

            /* Double forward */
            if(is_start_rank && !(U64_MASK(double_forward) & occupancy)){
                Move move = encode_move(src, double_forward, &game->state);
                move_list_add(move_list, move);
            }
        }

        /* En passant */
        if(game->state.en_passant != -1){
            if((color && ((game->state.en_passant == src+7) || (game->state.en_passant == src+9)))
                || (!color && ((game->state.en_passant == src-7) || (game->state.en_passant == src-9)))){
                    Move move = src | (game->state.en_passant << 6) | (Pawn << 12) | (Pawn << 15) | (1 << 21);
                    move_list_add(move_list, move);
             }
        }
    }
}
void generate_king_moves(MoveList* move_list, Game* game, int color){
    Board* board = &game->state;
    uint64_t king = board->pieces[King] & board->pieces[color];

    int king_pos = get_lsb_index(king);
    uint64_t attack = get_king_attacks(king_pos);
    generate_moves_from_bitboard(king_pos, attack, move_list, game, color);

    int home_king_square = (color == White) ? E1 : E8;
    if(king_pos != home_king_square){
        return;
    }

    uint64_t occupied = board->pieces[White] | board->pieces[Black];
    uint64_t own_rooks = board->pieces[color] & board->pieces[Rook];
    int enemy = !color;

    uint8_t kingside_right = (color == White) ? (1 << 2) : (1 << 0);
    uint8_t queenside_right = (color == White) ? (1 << 3) : (1 << 1);
    int kingside_rook_square = home_king_square + 3;
    int queenside_rook_square = home_king_square - 4;

    if(square_attacked(board, king_pos, enemy)){
        return;
    }

    if((board->castling_rights & kingside_right)
        && (own_rooks & U64_MASK(kingside_rook_square))
        && !(occupied & (U64_MASK(king_pos + 1) | U64_MASK(king_pos + 2)))
        && !square_attacked(board, king_pos + 1, enemy)
        && !square_attacked(board, king_pos + 2, enemy)){
        Move move = king_pos | ((king_pos+2) << 6) | (King << 12) | (Kingside << 21);
        move_list_add(move_list, move);
    }

    if((board->castling_rights & queenside_right)
        && (own_rooks & U64_MASK(queenside_rook_square))
        && !(occupied & (U64_MASK(king_pos - 1) | U64_MASK(king_pos - 2) | U64_MASK(king_pos - 3)))
        && !square_attacked(board, king_pos - 1, enemy)
        && !square_attacked(board, king_pos - 2, enemy)){
        Move move = king_pos | ((king_pos-2) << 6) | (King << 12) | (Queenside << 21);
        move_list_add(move_list, move);
    }
}

void generate_all_moves(MoveList* move_list, Game* game, int color){
    move_list_init(move_list);

    generate_knight_moves(move_list, game, color);
    generate_bishop_moves(move_list, game, color);
    generate_rook_moves(move_list, game, color);
    generate_queen_moves(move_list, game, color);

    /* Need to follow special rules */
    generate_pawn_moves(move_list, game, color);
    generate_king_moves(move_list, game, color);

}

void filter_legal_moves(MoveList* move_list, Game* game){
    int num_legal_moves = 0;
    for(int i=0;i<move_list->size;i++){
        if(is_legal_move(game, move_list->moves[i])){
            if(num_legal_moves < i){
                Move tmp = move_list->moves[i];
                move_list->moves[i] = move_list->moves[num_legal_moves];
                move_list->moves[num_legal_moves] = tmp;
            }
            num_legal_moves += 1;
        }
    }
    move_list->size = num_legal_moves;
}

void order_moves(MoveList* move_list, Game* game){
    /* Indexed by victim and by attacker, value is (victim value - attacker value) + 99. */
    static const int MVV_LVA[8][8] = {
        {99, 99, 99, 99, 99, 99, 99, 99},
        {99, 99, 99, 99, 99, 99, 99, 99},
        {99, 99, 99, 97, 97, 95, 91, 0},
        {99, 99, 101, 99, 99, 97, 94, 0},
        {99, 99, 101, 99, 99, 97, 94, 0},
        {99, 99, 103, 101, 101, 99, 95, 0},
        {99, 99, 107, 104, 104, 103, 99, 0},
        {99, 99, 198, 198, 198, 198, 198, 99}
    };

    int captures_start = 0;
    int captures = 0;

    /* Move captures to front and sort by MVV-LVA */
    for(int i=0;i<move_list->size;i++){
        Move move = move_list->moves[i];
        int piece = get_move_piece(move);
        int capture = get_move_capture(move);
        if(capture){
            Move tmp = move_list->moves[i];
            move_list->moves[i] = move_list->moves[captures];
            move_list->moves[captures] = tmp;

            captures += 1;

            for(int j=captures_start + (captures - 2);j>=captures_start;j--){
                if(MVV_LVA[capture][piece] > MVV_LVA[get_move_capture(move_list->moves[j])][get_move_piece(move_list->moves[j])]){
                    Move tmp = move_list->moves[j];
                    move_list->moves[j] = move_list->moves[j+1];
                    move_list->moves[j+1] = tmp;
                }
            }
        }
    }
}

void generate_legal_moves(Game* game, int color){
    generate_all_moves(&game->legal_moves, game, color);
    filter_legal_moves(&game->legal_moves, game);
    order_moves(&game->legal_moves, game);
}