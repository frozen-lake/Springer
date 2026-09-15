#include "game.h"
#include <stdlib.h>
#include <stdio.h>

AttackData attack_data = {}; // globally declared, statically allocated
static int attack_data_initialized = 0;


static void compute_knight_attacks(){
    for(int i=0;i<64;i++){
        if(i%8 > 0 && i/8 < 6) attack_data.knight[i] |= U64_MASK(15+i);
        if(i%8 < 7 && i/8 < 6) attack_data.knight[i] |= U64_MASK(17+i);
        if(i%8 > 1 && i/8 < 7) attack_data.knight[i] |= U64_MASK(6+i);
        if(i%8 < 6 && i/8 < 7) attack_data.knight[i] |= U64_MASK(10+i);
        if(i%8 > 1 && i/8 > 0) attack_data.knight[i] |= U64_MASK(i-10);
        if(i%8 < 6 && i/8 > 0) attack_data.knight[i] |= U64_MASK(i-6);
        if(i%8 > 0 && i/8 > 1) attack_data.knight[i] |= U64_MASK(i-17);
        if(i%8 < 7 && i/8 > 1) attack_data.knight[i] |= U64_MASK(i-15);
    }
}

static void compute_king_attacks(){
    for(int i=0;i<64;i++){
        if(i/8 < 7) attack_data.king[i] |= U64_MASK(i+8);
        if(i/8 > 0) attack_data.king[i] |= U64_MASK(i-8);
        if(i%8 > 0) attack_data.king[i] |= U64_MASK(i-1);
        if(i%8 < 7) attack_data.king[i] |= U64_MASK(i+1);
        if(i/8 < 7 && i%8 > 0) attack_data.king[i] |= U64_MASK(i+7);
        if(i/8 < 7 && i%8 < 7) attack_data.king[i] |= U64_MASK(i+9);
        if(i/8 > 0 && i%8 > 0) attack_data.king[i] |= U64_MASK(i-9);
        if(i/8 > 0 && i%8 < 7) attack_data.king[i] |= U64_MASK(i-7); 
    }
}

static void compute_pawn_attacks(){
    for(int i=0;i<64;i++){
        if(i%8 > 0){ // move diagonally left
            if(i/8 < 7) attack_data.pawn_white[i] |= U64_MASK(i+7);
            if(i/8 > 0) attack_data.pawn_black[i] |= U64_MASK(i-9);
        }
        if(i%8 < 7){ // move diagonally right
            if(i/8 < 7) attack_data.pawn_white[i] |= U64_MASK(i+9);
            if(i/8 > 0) attack_data.pawn_black[i] |= U64_MASK(i-7);
        }
    }
}

uint64_t occupancy_table_lookup(int index, int occupancy_key){
    return attack_data.occupancy_table[index][occupancy_key];
}

uint64_t generate_row_occupancy_key(int row, uint64_t occupancy){
    uint64_t result = occupancy & attack_data.row[row]; // Extract occupancy
    return (result >> ((8*row)+1)) & 0b111111; // Isolate the relevant 6 bits
}

uint64_t generate_col_occupancy_key(int col, uint64_t occupancy){
    uint64_t result = (occupancy & attack_data.col[col]) >> col; // Extract occupancy, move to A file
    return ((result * attack_data.lurd[LURD_INDEX(7)]) >> 57) & 0b111111; // Multiply to convert col to row, shift to first rank and isolate the 6 relevant bits
}

uint64_t generate_ruld_occupancy_key(int origin, uint64_t occupancy){
	uint64_t occupancy_key = attack_data.ruld[RULD_INDEX(origin)] & occupancy;
	occupancy_key = (occupancy_key * attack_data.col[1]) >> 58;
    return occupancy_key;
}
uint64_t generate_lurd_occupancy_key(int origin, uint64_t occupancy){
	uint64_t occupancy_key = attack_data.lurd[LURD_INDEX(origin)] & occupancy;
	occupancy_key = (occupancy_key * attack_data.col[1]) >> 58;
    return occupancy_key;
}


uint64_t get_row_attacks(int origin, uint64_t occupancy) {
    int col = origin % 8;
    int row = origin / 8;
    uint64_t occupancy_key = generate_row_occupancy_key(row, occupancy);
    return occupancy_table_lookup(col, occupancy_key) << (8*row);
}

uint64_t get_col_attacks(int origin, uint64_t occupancy) {
    int col = origin % 8;
    int row = origin / 8;
    uint64_t occupancy_key = generate_col_occupancy_key(col, occupancy);
    uint64_t attack = occupancy_table_lookup(row, occupancy_key);

    attack = attack * attack_data.ruld[RULD_INDEX(0)];
    attack = swap_uint64(attack);
    return (attack >> (7 - col)) & attack_data.col[col];
}

uint64_t get_ruld_attacks(int origin, uint64_t occupancy) {
    int col = origin % 8;
    uint64_t occupancy_key = generate_ruld_occupancy_key(origin, occupancy);
    uint64_t attack = occupancy_table_lookup(col, occupancy_key);
    return ((attack * attack_data.col[0]) & attack_data.ruld[RULD_INDEX(origin)]);
}

uint64_t get_lurd_attacks(int origin, uint64_t occupancy) {
    int col = origin % 8;
    uint64_t occupancy_key = generate_lurd_occupancy_key(origin, occupancy);
    uint64_t attack = occupancy_table_lookup(col, occupancy_key);
    return ((attack * attack_data.col[0]) & attack_data.lurd[LURD_INDEX(origin)]);
}

uint64_t get_rook_attacks(int square, uint64_t occupancy) {
    return get_row_attacks(square, occupancy) | get_col_attacks(square, occupancy);
}

uint64_t get_bishop_attacks(int square, uint64_t occupancy) {
    return get_ruld_attacks(square, occupancy) | get_lurd_attacks(square, occupancy);
}

uint64_t get_queen_attacks(int square, uint64_t occupancy) {
    return get_rook_attacks(square, occupancy) | get_bishop_attacks(square, occupancy);
}

uint64_t get_knight_attacks(int square) {
    return attack_data.knight[square];
}

uint64_t get_king_attacks(int square) {
    return attack_data.king[square];
}

uint64_t get_pawn_attacks(int square, int color) {
    return color ? attack_data.pawn_white[square] : attack_data.pawn_black[square];
}

static void generate_occupancy_table(){
    for(int i=0;i<8;i++){
        for(int j=0;j<64;j++){
            uint64_t result = 0;

            int l = i;
            while(l > 0){
                l--;
                result |= (1 << l);
                if((1 << l) & (j << 1)) break;
            }

            l = i;
            while(l < 7){
                l++;
                result |= (1 << l);
                if((1 << l) & (j << 1)) break;
            }

            attack_data.occupancy_table[i][j] = result;
        }
    }
}

static void compute_diagonal_ruld(int index) {
    attack_data.ruld[index] = 0;

    for (int col = 0; col < 8; col++) {
        int row = col + (index - 7);
        if (row >= 0 && row < 8) {  // Ensure row is valid
            int pos = row * 8 + col;
            attack_data.ruld[index] |= U64_MASK(pos);
        }
    }
}

static void compute_diagonal_lurd(int index) {
    attack_data.lurd[index] = 0;
    for (int row = 0; row < 8; row++) {
        int col = index - row;
        if (col >= 0 && col < 8) {  // Ensure col is valid
            int pos = row * 8 + col;
            attack_data.lurd[index] |= U64_MASK(pos);
        }
    }
}

void initialize_attack_data(){
    if(attack_data_initialized){
        return;
    }

    for(int i=0;i<8;i++){ // Row
        attack_data.row[i] = (uint64_t)0b11111111 << (8*i);
    } 
    for(int i=0; i<8; i++){ // Column
        attack_data.col[i] = (uint64_t)0b100000001000000010000000100000001000000010000000100000001 << i;
    }

    for(int i=0;i<15;i++){ // Right-Up Left-Down Diagonal
        compute_diagonal_ruld(i);
        compute_diagonal_lurd(i);
    }

    compute_king_attacks();
    compute_knight_attacks();
    compute_pawn_attacks();

    generate_occupancy_table();
	attack_data_initialized = 1;
}