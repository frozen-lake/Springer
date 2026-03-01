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
    char piece = 'P'; // Default to pawn
    char file_hint = '\0';
    int rank_hint = -1;
    char destination_square[3] = {0};

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
    if(strlen(input) == 4
        && strchr("abcdefgh", input[0])
        && input[1] == 'x'
        && strchr("abcdefgh", input[2])
        && (input[3] >= '1' && input[3] <= '8')){
        int destination = parse_square(&input[2]);
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

                if(is_legal_player_move(game, pawn_move)){
                    return pawn_move;
                }
            }
        }
    }

    /* Determine the piece and destination square */
    int i = 0;
    if (isalpha(input[0]) && strchr("PNBRQKpnbrqk", input[0])) {
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

    return encode_move(src, destination, board); // Return the encoded move
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


