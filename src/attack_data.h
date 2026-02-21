#ifndef ATTACK_DATA_H
#define ATTACK_DATA_H

typedef struct AttackData AttackData;

struct AttackData {
    uint64_t row[8];
    uint64_t col[8];

    uint64_t ruld[15]; // right-up, left-down diagonal / indexed by (row-col)+7
    uint64_t lurd[15]; // left-up, right-down diagonal \ indexed by row+col

    uint64_t knight[64];
    uint64_t king[64];
    uint64_t pawn_white[64];
    uint64_t pawn_black[64];

    /* 
     * Indexed by the position of a piece along the full 8-bit occupancy ray,
     * along with the compressed 6-bit occupancy ray.
     * Access returns a uint64_t with 8 bit sets.
     * The indexing logic is expected to move the returned 8-bit attack information into the correct position via multiplication and shifting.
     */
    uint64_t occupancy_table[8][64];
};

extern AttackData attack_data;


#define LURD_INDEX(pos) ((pos/8) + (pos%8))
#define RULD_INDEX(pos) (((pos/8) - (pos%8)) + 7)
#define COL_INDEX(pos) (pos % 8)
#define ROW_INDEX(pos) (pos / 8)

#endif


uint64_t occupancy_table_lookup(int index, int occupancy_key);

uint64_t generate_row_occupancy_key(int row, uint64_t occupancy);
uint64_t generate_col_occupancy_key(int col, uint64_t occupancy);
uint64_t generate_ruld_occupancy_key(int col, uint64_t occupancy);
uint64_t generate_lurd_occupancy_key(int col, uint64_t occupancy);

uint64_t get_rook_attacks(int square, uint64_t occupancy);
uint64_t get_bishop_attacks(int square, uint64_t occupancy);
uint64_t get_queen_attacks(int square, uint64_t occupancy);
uint64_t get_knight_attacks(int square);
uint64_t get_king_attacks(int square);
uint64_t get_pawn_attacks(int square, int color);

void initialize_attack_data();