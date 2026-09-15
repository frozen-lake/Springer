# Build
Requires GCC.
- `make game` builds `springer.exe`.
- `make tests` builds `springer_tests.exe`
- `make benchmarks` builds `search_benchmarks.exe`
- `make clean` removes the `obj` directory and built executables.

# Features
- Full move generation (castling, en passant, promotion)
- FEN position loading
- Game status detection (checkmate, draw by fifty-move rule, draw by threefold repetition, draw by insufficient material, and stalemate)
- Alpha-beta search with bounded quiescence and material-only evaluation
- Transposition table with Zobrist hashing and forward/backward incremental updates (age/depth replacement policy)
- Search benchmarks with timing and node counts

# To-Do
- Iterative deepening depth-first search
- Improved move ordering
- Search-level draw detection
- Improved evaluation
- Multithreading

<img width="405" height="582" alt="image" src="https://github.com/user-attachments/assets/7468de5c-5c56-4273-abd0-cbc3d34231db" />
