#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include <string.h>
#include "game.h"
#include "move.h"
#include "board.h"

int get_move_src(Move move){ return move & 0b111111; }
int get_move_dest(Move move){ return (move >> 6) & 0b111111; }
int get_move_piece(Move move){ return (move >> 12) & 0b111; }
int get_move_capture(Move move){ return (move >> 15) & 0b111; }
int get_move_promotion(Move move){ return (move >> 18) & 0b111; }
int get_move_special(Move move){ return (move >> 21) & 0b11; }

int move_to_simplified_algebraic(Move move, char* out, size_t out_size){
    if(out == NULL || out_size < 8 || move == 0){
        return 0;
    }

    int src = get_move_src(move);
    int dest = get_move_dest(move);
    int piece = get_move_piece(move);
    int special = get_move_special(move);
    int capture = (get_move_capture(move) != 0) || (special == EnPassant);
    int promotion = get_move_promotion(move);
    char dest_file = (char)('a' + (dest % 8));
    char dest_rank = (char)('1' + (dest / 8));
    char src_file = (char)('a' + (src % 8));
    char piece_letter = '\0';
    char promo = '\0';

    if(special == Kingside){
        return snprintf(out, out_size, "O-O") > 0;
    }
    if(special == Queenside){
        return snprintf(out, out_size, "O-O-O") > 0;
    }

    switch(piece){
        case Knight: piece_letter = 'N'; break;
        case Bishop: piece_letter = 'B'; break;
        case Rook: piece_letter = 'R'; break;
        case Queen: piece_letter = 'Q'; break;
        case King: piece_letter = 'K'; break;
        case Pawn: piece_letter = '\0'; break;
        default: return 0;
    }

    if(promotion != NO_PROMOTION){
        switch(promotion){
            case KNIGHT_PROMOTION: promo = 'N'; break;
            case BISHOP_PROMOTION: promo = 'B'; break;
            case ROOK_PROMOTION: promo = 'R'; break;
            case QUEEN_PROMOTION: promo = 'Q'; break;
            default: return 0;
        }
    }

    if(piece == Pawn){
        if(capture){
            if(promo != '\0'){
                return snprintf(out, out_size, "%cx%c%c=%c", src_file, dest_file, dest_rank, promo) > 0;
            }
            return snprintf(out, out_size, "%cx%c%c", src_file, dest_file, dest_rank) > 0;
        }

        if(promo != '\0'){
            return snprintf(out, out_size, "%c%c=%c", dest_file, dest_rank, promo) > 0;
        }
        return snprintf(out, out_size, "%c%c", dest_file, dest_rank) > 0;
    }

    if(capture){
        return snprintf(out, out_size, "%cx%c%c", piece_letter, dest_file, dest_rank) > 0;
    }
    return snprintf(out, out_size, "%c%c%c", piece_letter, dest_file, dest_rank) > 0;
}

int move_to_algebraic(Game* game, Move move, char* out, int out_size){
    int i = 0;
    int src = get_move_src(move);
    int dest = get_move_dest(move);
    int piece = get_move_piece(move);
    int special = get_move_special(move);
    int capture = (get_move_capture(move) != 0) || (special == EnPassant);
    int promotion = get_move_promotion(move);
    char dest_file = (char)('a' + (dest % 8));
    char dest_rank = (char)('1' + (dest / 8));
    char piece_letter = '\0';
    char promo = '\0';
    int need_file_disambiguation = 0;
    int need_rank_disambiguation = 0;
    int found_same_file = 0;
    int found_same_rank = 0;

    if(out == NULL || out_size == 0){
        return 0;
    }

    out[0] = '\0';

    if(special == Kingside){
        if(out_size < 4){
            return 0;
        }
        out[0] = 'O';
        out[1] = '-';
        out[2] = 'O';
        out[3] = '\0';
        return 1;
    }

    if(special == Queenside){
        if(out_size < 6){
            return 0;
        }
        out[0] = 'O';
        out[1] = '-';
        out[2] = 'O';
        out[3] = '-';
        out[4] = 'O';
        out[5] = '\0';
        return 1;
    }

    switch(piece){
        case Knight:
            piece_letter = 'N';
            break;
        case Bishop:
            piece_letter = 'B';
            break;
        case Rook:
            piece_letter = 'R';
            break;
        case Queen:
            piece_letter = 'Q';
            break;
        case King:
            piece_letter = 'K';
            break;
        default:
            break;
    }

    if(promotion != NO_PROMOTION){
        switch(Pawn + promotion){
            case Knight:
                promo = 'N';
                break;
            case Bishop:
                promo = 'B';
                break;
            case Rook:
                promo = 'R';
                break;
            case Queen:
                promo = 'Q';
                break;
            default:
                return 0;
        }
    }

    if(piece != Pawn && piece_letter == '\0'){
        return 0;
    }

    if(game != NULL && piece != Pawn && piece != King){
        for(int j = 0; j < game->legal_moves.size; j++){
            Move other = game->legal_moves.moves[j];
            if(other == move){
                continue;
            }
            if(get_move_piece(other) != piece){
                continue;
            }
            if(get_move_dest(other) != dest){
                continue;
            }

            need_file_disambiguation = 1;
            need_rank_disambiguation = 1;

            if((get_move_src(other) % 8) == (src % 8)){
                found_same_file = 1;
            }
            if((get_move_src(other) / 8) == (src / 8)){
                found_same_rank = 1;
            }
        }

        if(need_file_disambiguation || need_rank_disambiguation){
            if(!found_same_file){
                need_rank_disambiguation = 0;
            } else if(!found_same_rank){
                need_file_disambiguation = 0;
            }
        }
    }

    if(piece == Pawn){
        if(capture){
            if(out_size < 5){
                return 0;
            }
            out[i++] = (char)('a' + (src % 8));
            out[i++] = 'x';
            out[i++] = dest_file;
            out[i++] = dest_rank;

            if(promotion != NO_PROMOTION){
                if(out_size < 7){
                    return 0;
                }
                out[i++] = '=';
                out[i++] = promo;
            }

            out[i] = '\0';
            return 1;
        }

        if(promotion != NO_PROMOTION){
            if(out_size < 5){
                return 0;
            }
            out[i++] = dest_file;
            out[i++] = dest_rank;
            out[i++] = '=';
            out[i++] = promo;
            out[i] = '\0';
            return 1;
        }

        if(out_size < 3){
            return 0;
        }
        out[i++] = dest_file;
        out[i++] = dest_rank;
        out[i] = '\0';
        return 1;
    }

    if(capture){
        if(out_size < 5 + need_file_disambiguation + need_rank_disambiguation){
            return 0;
        }
        out[i++] = piece_letter;
        if(need_file_disambiguation){
            out[i++] = (char)('a' + (src % 8));
        }
        if(need_rank_disambiguation){
            out[i++] = (char)('1' + (src / 8));
        }
        out[i++] = 'x';
        out[i++] = dest_file;
        out[i++] = dest_rank;
        out[i] = '\0';
        return 1;
    }

    if(out_size < 4 + need_file_disambiguation + need_rank_disambiguation){
        return 0;
    }
    out[i++] = piece_letter;
    if(need_file_disambiguation){
        out[i++] = (char)('a' + (src % 8));
    }
    if(need_rank_disambiguation){
        out[i++] = (char)('1' + (src / 8));
    }
    out[i++] = dest_file;
    out[i++] = dest_rank;
    out[i] = '\0';
    return 1;
}

int is_legal_player_move(Game* game, Move move){
    for(int i=0; i<game->legal_moves.size; i++){
        if(move == game->legal_moves.moves[i]){
            return 1;
        }
    }

    return 0;
}


int is_legal_move(Game* game, Move move){
    if(move == (uint32_t)-1) return 0;

    int color = game->state.side_to_move;
    int special = get_move_special(move);
    BoardState* state = &game->state;

    /* Castling checks (pre-move) */
    if(special == Kingside || special == Queenside){
        uint8_t castling_bit = (color == White) ? 
            ((special == Kingside) ? (1 << 2) : (1 << 3)) : 
            ((special == Kingside) ? (1 << 0) : (1 << 1));
        
        if(!(state->castling_rights & castling_bit)) return 0;

        int src = get_move_src(move);
        int step = (special == Kingside) ? 1 : -1;
        
        // Cannot castle out of, through, or into check
        if(square_attacked(state, src, !color)) return 0;
        if(square_attacked(state, src + step, !color)) return 0;
        if(square_attacked(state, src + 2 * step, !color)) return 0;

        // Path must be clear
        uint64_t path_mask = U64_MASK(src + step) | U64_MASK(src + 2 * step);
        if(special == Queenside) path_mask |= U64_MASK(src - 3);
        if((state->pieces[White] | state->pieces[Black]) & path_mask) return 0;
    }

    // Trial make move
    UndoInfo undo;
    make_move_on_state(state, move, &undo);
    
    // Check if king is in check (from the perspective of the side that just moved)
    // side_to_move was flipped, so !state->side_to_move is the mover
    int in_check = square_attacked(state, state->king_sq[!state->side_to_move], state->side_to_move);
    
    unmake_move_on_state(state, move, &undo);

    return !in_check;
}

/* Convert algebraic notation to numeric square index: "e4" --> 28 */
int parse_square(char *square) {
    if (strlen(square) != 2 || square[0] < 'a' || square[0] > 'h' || square[1] < '1' || square[1] > '8') {
        return -1;
    }
    int file = square[0] - 'a';
    int rank = square[1] - '1';
    return rank * 8 + file;
}

/* Find the source square given the piece type and destination square */
int find_source_square(Board *board, char piece, int destination, char file_hint, int rank_hint) {
    uint64_t occupancy = board->pieces[White] | board->pieces[Black];
    int color = isupper(piece) ? White : Black;
    char p = toupper(piece);
    uint64_t potential_attackers = 0;

    switch(p) {
        case 'P': {
            // For pawns, file_hint indicates a diagonal capture (e.g., "exd5")
            // No file_hint means a forward move (e.g., "e4")
            if (file_hint) {
                // Capture move - only consider diagonal attacks from the specified file
                if (color == White) {
                    potential_attackers |= (get_pawn_attacks(destination, Black) & board->pieces[Pawn] & board->pieces[White]);
                } else {
                    potential_attackers |= (get_pawn_attacks(destination, White) & board->pieces[Pawn] & board->pieces[Black]);
                }
            } else {
                // Non-capture move - only consider forward moves
                if (color == White) {
                    if (destination >= 8) {
                        uint64_t single = U64_MASK(destination - 8);
                        if (single & board->pieces[Pawn] & board->pieces[White]) potential_attackers |= single;
                        if (destination >= 16 && (destination / 8 == 3) && !(occupancy & U64_MASK(destination - 8))) {
                            uint64_t double_push = U64_MASK(destination - 16);
                            if (double_push & board->pieces[Pawn] & board->pieces[White]) potential_attackers |= double_push;
                        }
                    }
                } else {
                    if (destination <= 55) {
                        uint64_t single = U64_MASK(destination + 8);
                        if (single & board->pieces[Pawn] & board->pieces[Black]) potential_attackers |= single;
                        if (destination <= 47 && (destination / 8 == 4) && !(occupancy & U64_MASK(destination + 8))) {
                            uint64_t double_push = U64_MASK(destination + 16);
                            if (double_push & board->pieces[Pawn] & board->pieces[Black]) potential_attackers |= double_push;
                        }
                    }
                }
            }
            break;
        }
        case 'N': potential_attackers = get_knight_attacks(destination) & board->pieces[Knight] & board->pieces[color]; break;
        case 'B': potential_attackers = get_bishop_attacks(destination, occupancy) & board->pieces[Bishop] & board->pieces[color]; break;
        case 'R': potential_attackers = get_rook_attacks(destination, occupancy) & board->pieces[Rook] & board->pieces[color]; break;
        case 'Q': potential_attackers = get_queen_attacks(destination, occupancy) & board->pieces[Queen] & board->pieces[color]; break;
        case 'K': potential_attackers = get_king_attacks(destination) & board->pieces[King] & board->pieces[color]; break;
    }

    while(potential_attackers) {
        int src = get_lsb_index(potential_attackers);
        potential_attackers &= potential_attackers - 1;

        if (file_hint && (src % 8) != (file_hint - 'a')) continue;
        if (rank_hint >= 0 && (src / 8) != rank_hint) continue;

        return src;
    }

    return -1;
}

/* Convert algebraic notation move to integer encoded source/dest squares */
int parse_algebraic_move(char* input, Game* game) {
    Board* board = &game->state;
    int color = game->state.side_to_move;
    int input_len = (int)strlen(input);
    char piece = 'P'; // Default to pawn
    char file_hint = '\0';
    int rank_hint = -1;
    char destination_square[3] = {0};
    enum Promotion requested_promotion = NO_PROMOTION;
    int move_len = input_len;

    if(input_len >= 4 && input[input_len - 2] == '='){
        switch(toupper(input[input_len - 1])){
            case 'N':
                requested_promotion = KNIGHT_PROMOTION;
                break;
            case 'B':
                requested_promotion = BISHOP_PROMOTION;
                break;
            case 'R':
                requested_promotion = ROOK_PROMOTION;
                break;
            case 'Q':
                requested_promotion = QUEEN_PROMOTION;
                break;
            default:
                return -1;
        }
        move_len = input_len - 2;
    }

    if(input[0] == 'O' || input[0] == 'o'){
        uint64_t king = board->pieces[King] & game->state.pieces[game->state.side_to_move];
        int king_pos = get_lsb_index(king);
        if(strncmp(input, "O-O-O", 5) == 0 || strncmp(input, "o-o-o", 5) == 0){
            return king_pos | ((king_pos-2) << 6) | (King << 12) | (Queenside << 21);
        } else if(strncmp(input, "O-O", 3) == 0 || strncmp(input, "o-o", 3) == 0){
            return king_pos | ((king_pos+2) << 6) | (King << 12) | (Kingside << 21);
        }
    }

    /* Try pawn capture notation first (e.g. bxa3). If legal, use it; else fall through. */
    if(move_len == 4
        && strchr("abcdefgh", input[0])
        && input[1] == 'x'
        && strchr("abcdefgh", input[2])
        && (input[3] >= '1' && input[3] <= '8')){
        char capture_square[3] = { input[2], input[3], '\0' };
        int destination = parse_square(capture_square);
        if(destination >= 0){
            int src = input[0] - 97; // Column
            if(color){ // Row
                src += 8 * (input[3] - 50);
            } else {
                src += 8 * (input[3] - 48);
            }

            if(board->pieces[Pawn] & board->pieces[color] & U64_MASK(src)){
                Move pawn_move;
                if(game->state.en_passant != destination){
                    pawn_move = encode_move(src, destination, board);
                } else {
                    pawn_move = src | (destination << 6) | (Pawn << 12) | (Pawn << 15) | (EnPassant << 21);
                }

                if(requested_promotion != NO_PROMOTION){
                    int promotion_rank = color ? 7 : 0;
                    if((destination / 8) != promotion_rank || game->state.en_passant == destination){
                        return -1;
                    }
                    pawn_move = encode_promotion(src, destination, board, requested_promotion);
                }

                if(is_legal_player_move(game, pawn_move)){
                    return pawn_move;
                }
            }
        }
    }

    /* Determine the piece and destination square */
    int i = 0;
    if (move_len >= 3 && isalpha(input[0]) && strchr("NBRQK", toupper(input[0]))) {
        piece = toupper(input[0]);
        i++;
    }

    if(!(game->state.side_to_move)){ piece = tolower(piece); }

    /* Check for disambiguation */
    if(input[1] != 'x'){
        if (isalpha(input[i]) && input[i + 1] >= '1' && input[i + 1] <= '8') { // No disambiguation
            strncpy(destination_square, &input[i], 2);
        } else if (isalpha(input[i]) && isalpha(input[i + 1])) { // File disambiguation
            file_hint = input[i];
            strncpy(destination_square, &input[i + 1], 2);
        } else if (isdigit(input[i]) && isalpha(input[i + 1])) { // Rank disambiguation
            rank_hint = input[i] - '1';
            strncpy(destination_square, &input[i + 1], 2);
        }
    } else {
        if (piece == 'P' || piece == 'p') {
            file_hint = input[0];
        }
        strncpy(destination_square, &input[2], 2); // capture dest
    }

    /* Parse destination square */
    int destination = parse_square(destination_square);
    if (destination < 0) {
        if(DEBUG_ERR){ 
            fprintf(stderr, "Invalid destination square: %s\n", destination_square);
        }
        return -1;
    }

    if((input[1] == 'x' || input[2] == 'x')
        && !((board->pieces[White] | board->pieces[Black]) & U64_MASK(destination))
        && (game->state.en_passant != destination)){
        return -1;
    }

    /* Find the source square */
    int src = find_source_square(board, piece, destination, file_hint, rank_hint);
    if (src < 0) {
	    if(DEBUG_ERR) {
            fprintf(stderr, "No valid source square found for move: %spiece: %c, dest: %d, fhint: %d, rhint: %d, source: %d\n",
                    input, piece, destination, file_hint, rank_hint, src);
        }
        return -1;
    }

    Move move = encode_move(src, destination, board);
    if(requested_promotion != NO_PROMOTION){
        int promotion_rank = color ? 7 : 0;
        if(get_move_piece(move) != Pawn || (destination / 8) != promotion_rank){
            return -1;
        }
        move = encode_promotion(src, destination, board, requested_promotion);
    }

    return move; // Return the encoded move
}

void print_move(Move move){
    int src = move & 0b111111;
    int dest = (move >> 6) & 0b111111;
    int piece = (move >> 12) & 0b111;
    int capture = (move >> 15) & 0b111;
    int promotion = (move >> 18) & 0b111;
    int special = (move >> 21) & 0b11;

    printf("{move: 0x%x, src: %d, dest: %d, piece: %s, capture: %s, special: %d, promotion: %d}\n",
        move, src, dest, piece_to_string(piece), piece_to_string(capture), special, promotion);
}

void move_list_init(MoveList* move_list){  
    move_list->size = 0;
}

void move_list_add(MoveList* move_list, Move move){
    if(move_list->size < MAX_MOVES){
        move_list->moves[move_list->size] = move;
        move_list->size += 1;
    }
}

void move_list_copy(MoveList* src, MoveList* dest){
    move_list_init(dest);
    for(int i=0;i<src->size;i++){
        move_list_add(dest, src->moves[i]);
    }
}


void print_moves(MoveList* move_list){
    printf("MoveList (%d moves):\n", move_list->size);
    int size = move_list->size;
    for(int i=0;i<size;i++){
        print_move(move_list->moves[i]);
    }
}


Move encode_move(int src, int dest, Board* board){
    Move move = 0;

    move |= (src & 0b111111);
    move |= (dest & 0b111111) << 6;

    int piece = position_to_piece_number(board, src);
    int capture = position_to_piece_number(board, dest);

    move |= piece << 12;
    move |= capture << 15;

    /* Promotion bits and special bits left for later */
    
    return move;
}

Move encode_promotion(int src, int dest, Board* board, enum Promotion promotion){
    return encode_move(src, dest, board) | (promotion << 18);
}


