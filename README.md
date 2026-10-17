# Springer

Springer is a chess engine written in C. It uses UCI, runs on AWS, and plays live on Lichess as [SpringerBot](https://lichess.org/@/SpringerBot), where you can watch it play or challenge it to a game.

## Engine

**Board representation and move generation**
- Bitboard board representation with precomputed attack tables
- Legal move generation, including castling, en passant and promotions
- Incremental make/unmake with Zobrist hashing
- FEN parsing, and detection of checkmate, stalemate, threefold repetition, the fifty-move rule and insufficient material

**Search**
- Iterative-deepening alpha-beta search with time management based on the game clock
- Quiescence search with delta pruning
- Transposition table: 2^20 entries (24 MB) with an age/depth-based replacement policy
- Move ordering: transposition table move first, then captures by MVV-LVA

**Evaluation**
- Material, piece centralization, mobility and pawn structure
- Game phase is used to scale evaluation terms: piece centralization is weighted higher early on, and passed pawns are weighted higher later in the game

## Correctness and testing

- Move generation is validated by perft on standard test positions, up to depth 5
- Unit tests cover move generation, search, the transposition table and the UCI interface
- GitHub Actions builds and runs tests on every push and pull request, along with a separate build for AddressSanitizer and UndefinedBehaviorSanitizer

## Deployment

- **Bridge to Lichess:** [lichess-bot](https://github.com/lichess-bot-devs/lichess-bot) connects
  the engine's UCI interface to the Lichess Bot API. Springer runs inside its Docker image
- **Infrastructure as code:** the AWS CDK stack in [`infra/`](infra) defines a VPC, an EC2 instance, its IAM role, and the CloudWatch log group. Expects a Lichess bot:play token under `/springer/lichess-bot-token` in SSM Parameter Store
- **Continuous deployment:** a systemd timer on the instance checks `main` periodically. When it
  finds a new commit, it builds a new image, waits for any game in progress to finish, and then
  restarts the bot on the new version. See [`deploy/`](deploy)
- Behavior such as accepted time controls and matchmaking is set in [`config.yml`](config.yml).

## Building and running

Requires GCC and Make.

| Command | Result |
|---|---|
| `make uci` | Builds `uci.exe`, the UCI engine |
| `make tests` | Builds the unit test runner, `springer_tests` |
| `make tests-sanitize` | Builds the tests with AddressSanitizer and UndefinedBehaviorSanitizer |
| `make benchmarks` | Builds the search benchmarks, `search_benchmarks` |
| `make game` | Builds `springer`, a command-line interface for playing against the engine |
| `make clean` | Removes build output |

## Roadmap

- Better move ordering (killer moves, history heuristic)
- Search improvements such as null-move pruning and late move reductions
- Evaluation tuning
- Multithreaded search
