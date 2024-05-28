import java.util.*;

public class SpringerAI {

    // Springer chess engine.


    public static final int depth = 2; // Default search depth
    public static final int depthSpecial=3;
    public Board analysisBoard; // Analysis board
    public final Board gameBoard; // Real board
    public boolean colorPlaying; // Side the AI is playing for

    public final Map<String, Integer> pieceValues;
    public SpringerAI(Board board)    {
        this.pieceValues = new HashMap<String, Integer>();
        pieceValues.put("Pawn", 100);
        pieceValues.put("Knight", 295);
        pieceValues.put("Bishop", 305);
        pieceValues.put("Rook", 495);
        pieceValues.put("Queen", 900);
        pieceValues.put("King", 9999);

        this.gameBoard = board;
        this.analysisBoard = new Board(board);
        this.colorPlaying = false;
    }

    // takeTurn(): Creates an analysis board of the current board state. There it searches for the best move,
    // which it then executes on the game board.
    public void takeTurn(){
        analysisBoard = new Board(gameBoard);
        Set<Move> s = analysisBoard.sideToMove ? analysisBoard.getWhiteMoves() : analysisBoard.getBlackMoves();
        SearchNode bestNode = null;
        Move bestMove = null;
        Integer bestEval = null; // search eval of best move

        for(Move move : s){
            analysisBoard.makeMove(move);
            SearchNode node = minimax(depth, move, false);
            int n = node.score;
            analysisBoard.undoMove(move);
            if(bestMove == null || (n < bestEval)){
                bestMove = move; bestNode = node; bestEval = n;
            }

            //if(move.to() == 36)System.out.println("e5 node: " + node);
            if(move.piece().type.equals("Queen") && move.to() == 35 && move.capture() != null) System.out.println("d1 d5 qxP node children: " + node.children);
        }
        if(bestMove == null) throw new IllegalStateException();
        Move move;
        if(bestMove.promotion()==null){
            move = gameBoard.createMove(bestMove.from(), bestMove.to());
        } else {move = gameBoard.createMove(bestMove.from(), bestMove.to(), bestMove.promotion());}
        gameBoard.makeMove(move);


        System.out.println("Chosen move: " + move + ", score " + bestEval + ", new eval state " + evaluate(bestMove, false));
        System.out.println("bestNode: " + bestNode);
    }

    private Set<Move> moves(){
        return analysisBoard.sideToMove ? analysisBoard.getWhiteMoves() : analysisBoard.getBlackMoves();
    }


    private SearchNode minimax(int depth, Move m, boolean special){
        return minimaxMoveset(depth, m, special, special?getActiveMoves():moves());
    }
    private SearchNode minimaxActiveMoves(int depth, Move m, boolean special){
        return minimaxMoveset(depth, m, special, getActiveMoves());
    }

    private SearchNode minimaxMove(int depth, Move m, boolean special){
        return null;
    }

    private SearchNode minimaxMoveset(int depth, Move m, boolean special, Set<Move> moveSet){
        if(depth==0){
            //System.out.println("Evaluating... score=" + evaluate() + " | " + analysisBoard.blackArmy.size());

            // Special base case: Their king is in check, or
            if(!special && (analysisBoard.kingW.inCheck() || analysisBoard.kingB.inCheck())){
                return minimaxActiveMoves(depthSpecial-1, m, true);
            } else if(!special){ // 1 or more enemy threats

            }

            // Standard base case
            return new SearchNode(-evaluate(m, special), m);
        };

        // If we are in an active move search and there are none, reset to depth 2 normal move search.
        if(special && moveSet.size() == 0) {
            moveSet = moves();
            if(depth > 2) depth = 2;
        }

        SearchNode current = new SearchNode(m);

        Integer max = null;
        SearchNode maxNode = null;
        for(Move move: moveSet){

            analysisBoard.makeMove(move);
            SearchNode node = minimax(depth - 1, move, special);

            if(node != null) {
                int score = node.score;
                if (max == null || score > max) {
                    max = score;
                    maxNode = node;
                }
                current.addChild(node);
            }
            analysisBoard.undoMove(move);
            //System.out.println("Depth " + depth + ", move " + move + ", score " + score + " complete.");
        }
        if(maxNode == null) {
            current.score = Integer.MIN_VALUE;
            current.eval = Integer.MIN_VALUE;
            return current;
        }//return null;
        current.score = max;
        current.eval = evaluate(m, special) * (analysisBoard.sideToMove == colorPlaying ? 1 : -1);
        current.next = maxNode;
        //System.out.println("=== === === depth " + depth + " best move " + m);
        return current;
    }

    public Set<Move> getActiveMoves(){
        Map<String, Set<Move>> map = sortMoves(moves());
        Set<Move> moves = map.get("Checks"); moves.addAll(map.get("Captures")); moves.addAll(map.get("Threats"));
        return moves;
    }

    // Returns centipawn evaluation of the current analysisBoard state from the perspective
    // of the side who just made a move, so the opponent has the next move.
    public int evaluate(Move m, boolean special){

        boolean scoringFor = !analysisBoard.sideToMove;
        int scoreMultiplier = !analysisBoard.sideToMove?1:-1;
        int eval = 0;
        int friendlyThreats = 0;
        int enemyThreats = 0;



        // Piece value

        // King safety
        // - Enemy pieces around a King create an "attack"
        // - Open lines around a king can create ideal conditions for the attack. If
        // a bishop, rook, or queen can land on those open lines, even more so.
        // - Friendly pieces and pawns around a King can soften the evaluation of the attack
        // - Pawn storm can start an attack on the King.
        // Strategy: Score for the existence of an attack on the other king.

        // Positional consideration
        // - If past move 8, score for having more or less pieces moved at least once.
        // - Score for knight on an outpost
        // - Score for pawn structure?

        // Tactical consideration
        // - Score for captures

        // Center control
        // - Large bonus for pawns controlling central 4 squares
        // - Medium bonus for pieces controlling central 4 squares
        // - Small bonus for pawns and pieces controlling central 16 squares

        // Mobility
        // - Score mobility piece by piece
        // - Weight the bonus by the quality of the square

        for(Piece p : analysisBoard.pieces()){
            int pieceMultiplier = p.color != analysisBoard.sideToMove ? 1 : -1;
            Set<Move> moves = p.getMoves();
            int pieceValue = pieceValues.get(p.type);
            switch(p.type){
                case "Pawn":
                    // Center pawn bonus
                    if(p.position % 8 > 2 && p.position % 8 < 5 && p.position / 8 > 2 && p.position / 8 < 5){
                        eval += 33 * pieceMultiplier;
                    }
                    // Threats
                    if(p.protectedByPawn() || p.defended()) { // Stable pawn
                        for(Move move: p.getMoves()){
                            if(move.capture() != null && !move.capture().type.equals("Pawn")){
                                eval += 33 * pieceMultiplier;
                                if(scoringFor == p.color) { friendlyThreats += 1; }
                                else { enemyThreats += 1; }
                            }
                        }
                    }
                    break;
                case "Knight":
                    break;
                case "Bishop":
                    break;
                case "Rook":

                    if(((Rook) p).connected()){

                    } else if(((Rook) p).orthogonalNoKing()){
                        eval += 18 * pieceMultiplier;
                    }
                    break;
                case "Queen":
                    break;
                case "King":
                    King k = (King) p;

                    // Subtract points for King above home rank
                    if(analysisBoard.moves.size() < 10 && p.color?(k.position/8!=0):(k.position/8!=7)) {
                        eval -= 40 * pieceMultiplier;
                    }

                    if(((King) p).canCastleShort() || ((King) p).canCastleLong()){
                        eval += 12 * pieceMultiplier;
                    }
                    break;

            }
            // If friendly piece is threatened by pawn, evaluate up to special depth and return that.

            // Move count mobility bonus
            eval += moves.size()*pieceMultiplier;
            eval += pieceValue*pieceMultiplier;
        }


        switch(enemyThreats){
            case 0:
                break;
            case 1:
                eval *= 1.05;
                break;
            case 2:
                eval *= 1.10;
                break;
            default:
                eval *= 1.15;

        }
        switch(friendlyThreats){
            case 0:
                break;
            case 1:
                eval *= 1.00;
                break;
            case 2:
                eval *= 1.033;
                break;
            default:
                eval *= 1.066;

        }

        return eval;
    }


    // Filters a set of moves into a map divided by checks, captures, and threats.
    public Map<String, Set<Move>> sortMoves(Set<Move> s){
        HashMap<String, Set<Move>> map = new HashMap<String, Set<Move>>();
        map.put("Checks", new HashSet<Move>());
        map.put("Captures", new HashSet<Move>());
        map.put("Threats", new HashSet<Move>());
        Iterator<Move> iter = s.iterator();
        Move m;
        while(iter.hasNext()){
            m = iter.next();
            analysisBoard.primitiveMove(m);

            // Checks
            if(m.piece().color?analysisBoard.kingB.inCheck():analysisBoard.kingW.inCheck()){
                map.get("Checks").add(m);
                analysisBoard.undoPrimitiveMove(m);
                continue;
            }

            // Captures
            if(m.capture() != null){
                map.get("Captures").add(m);
                analysisBoard.undoPrimitiveMove(m);
                continue;
            }

            // Threats
            Set<Piece> projectors = m.piece().terminalProjectors(false, m.to());
            boolean flag = false;
            for(Piece p: projectors){
                if(flag) break;
                // N, B or P threatens Q or R
                if((p.type.equals("Queen") || p.type.equals("Rook"))
                        && (m.piece().type.equals("Knight") || m.piece().type.equals("Bishop") || m.piece().type.equals("Pawn"))){
                    map.get("Threats").add(m);
                    analysisBoard.undoPrimitiveMove(m);
                    flag = true; continue;
                }

                // P threatens to promote
                if(m.promotion()!=null&& (m.promotion().equals("Q") || m.promotion().equals("N"))){
                    map.get("Threats").add(m);
                    analysisBoard.undoPrimitiveMove(m);
                    flag = true; continue;
                }
            }

            if(!flag) analysisBoard.undoPrimitiveMove(m);
        }
        return map;
    }

    private int defense(Piece p){
        Set<Piece> defenders = p.terminalProjectors(true, p.position);

        // Remove pinned defenders
        return 0;
    }
}
