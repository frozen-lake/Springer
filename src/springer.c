#include "game.h"
#include "move.h"
#include "attack_data.h"
#include "springer.h"
#include "move_gen.h"
#include "search.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>

#define INPUT_MAX 16

char* get_input(){
    char* buffer = calloc(INPUT_MAX, sizeof(char));
    if(!buffer){
        return NULL;
    }
    if(fgets(buffer, INPUT_MAX, stdin) == NULL){
        printf("\nError: BAD INPUT");
        free(buffer);
        return NULL;
    }
    return buffer;
}

Move get_player_move(Game* game){
    printf("Enter move (%s): ", (game->state.side_to_move?"White":"Black"));
    char* buffer = calloc(INPUT_MAX, sizeof(char));
    if(!buffer){
        return 0;
    }

    if(fgets(buffer, INPUT_MAX, stdin) != NULL){
        buffer[strcspn(buffer, "\r\n")] = '\0'; // strip newline
        printf("\n");
        int move = parse_algebraic_move(buffer, game);
        free(buffer);
        return move;
    } else {
        printf("Error: BAD INPUT\n");
    }
    free(buffer);
    return 0;
}

void print_options(){
    printf("=== GAME OPTIONS ===\n");
    printf("1 - New game\n");
    printf("2 - Play from FEN\n");
}

int main(){
	initialize_attack_data();

    Game* game = create_game();
    TranspositionTable search_tt = (TranspositionTable){0};
    SearchState search_state = (SearchState){0};
    int search_stop = 0;

    tt_init(&search_tt);
    initialize_searchstate(&search_state, &search_tt, 4, &search_stop);


    print_options();
    printf("Enter choice: ");
    char* input = get_input();
    if(input == NULL){
        printf("Error: FAILED MEMORY ALLOCATION");
        return 1;
    }
    printf("input was %s\n", input);

    initialize_game(game);
    switch(input[0]){
        case '1':
            break;
        case '2':
            break;
        default:
            break;
    }

    free(input);

    /* MAIN GAME LOOP */
    char move_buffer[16];
    while(1){
        update_game_status(game);
        print_board(&game->state);
        
        if(game->game_status != ACTIVE){
            break;
        }

        /* PLAYER TURN */
        printf("Enter move (%s): ", (game->state.side_to_move?"White":"Black"));

        memset(move_buffer, 0, 16);
        if(fgets(move_buffer, sizeof(move_buffer), stdin) == NULL){
            printf("Error: BAD INPUT\n");
            continue;
        }

        for(int i=0;i<(int)strlen(move_buffer);i++){
            if(move_buffer[i] == '\n'){ move_buffer[i] = '\0'; }
        }

        Move move = parse_algebraic_move(move_buffer, game);

        if(is_legal_player_move(game, move)){
            printf("<--%s PLAYED: %s-->\n", (game->state.side_to_move?"WHITE":"BLACK"), move_buffer);
            make_move(game, move); 
        } else {
            printf("<--ILLEGAL MOVE: %s-->\n", move_buffer);
            continue;
        }


        /* AI TURN */
        move = search_best_move(game, &search_state);
        if(move != 0 && is_legal_player_move(game, move)){
            char algebraic[16];
            printf("<--%s PLAYED: ", (game->state.side_to_move?"WHITE":"BLACK"));
            if(move_to_algebraic(game, move, algebraic, sizeof(algebraic))){
                printf("%s", algebraic);
            } else {
                print_move(move);
            }
            printf("-->\n");
            make_move(game, move);
        } else {
            fprintf(stderr, "\nEXCEPTION: ILLEGAL OR EMPTY AI MOVE\n");
            destroy_searchstate(&search_state);
            tt_free(&search_tt);
            destroy_game(game);
            return -1;
        }

    }

    printf("GAME OVER -- ");
    switch(game->game_status){
        case WHITE_WINS:
            printf("WHITE WINS!\n");
            break;
        case BLACK_WINS:
            printf("BLACK WINS!\n");
            break;
        case DRAW_STALEMATE:
            printf("STALEMATE!\n");
            break;
        case DRAW_FIFTY_MOVE:
            printf("DRAW BY FIFTY MOVE RULE.\n");
            break;
        case DRAW_INSUFFICIENT_MATERIAL:
            printf("DRAW BY INSUFFICIENT MATERIAL.\n");
            break;
        case DRAW_THREEFOLD_REPETITION:
            printf("DRAW BY THREEFOLD REPETITION.\n");
            break;
    }

    destroy_searchstate(&search_state);
    tt_free(&search_tt);
    destroy_game(game);


    return 0;
}