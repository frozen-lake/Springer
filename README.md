# Springer
A chess engine.

Uses an object-oriented architecture as opposed to bitboards.
Alpha-beta pruning depth-first search is used with move ordering and a quiescence search to find the best move.

Moves are made using the source and destination squares, as follows. 
<img width="579" alt="Screenshot_2" src="https://github.com/user-attachments/assets/3f0aa2f7-2eb6-4881-97d9-e8a647a5cea2">

Pawn promotion invokes a prompt to choose which piece to promote to. Castling is implemented, en passant is not as of yet.
