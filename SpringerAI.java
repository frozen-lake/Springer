import java.util.*;

public class SpringerAI {

    public static final int depth = 3;
    public Board analysisBoard; // Analysis board
    public final Board gameBoard;
    public boolean colorPlaying; // Side the AI is playing for
    public SpringerAI(Board board)    {
        this.gameBoard = board;
        this.analysisBoard = new Board(board);
        this.colorPlaying = false;
    }
    public void takeTurn(){
        analysisBoard = new Board(gameBoard);
        Set<Move> s = analysisBoard.sideToMove ? analysisBoard.getWhiteMoves() : analysisBoard.getBlackMoves();
        SearchNode bestNode = null;
        Move bestMove = null;
        int eval = 0;

        for(Move move : s){
            analysisBoard.makeMove(move);
            SearchNode node = negaMax(depth, move);
            int n = node.score;
            if(bestMove == null || (colorPlaying?n > eval: n < eval)){ bestMove = move; bestNode = node; eval = n;}

            analysisBoard.undoMove(move);

            if(move.to() == 36)System.out.println("e5 node: " + node);
        }
        gameBoard.makeMove(gameBoard.createMove(bestMove.from(), bestMove.to()));
        System.out.println("Chosen move: " + bestMove + ", score " + eval + ", new eval " + evaluate());
        System.out.println("bestNode: " + bestNode);
    }

    private Set<Move> moves(){
        return analysisBoard.sideToMove ? analysisBoard.getWhiteMoves() : analysisBoard.getBlackMoves();
    }

    private SearchNode negaMax(int depth, Move m){

        if(depth==0){
            //System.out.println("Evaluating... score=" + evaluate() + " | " + analysisBoard.blackArmy.size());
            return new SearchNode(evaluate(), m);
        }
        SearchNode current = new SearchNode(m);

        Integer max = null;
        SearchNode maxNode = null;
        for(Move move: moves()){

            analysisBoard.makeMove(move);
            SearchNode node = negaMax(depth - 1, move);

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
        if(maxNode == null) return null;
        current.score = max;
        current.next = maxNode;
        //System.out.println("=== === === depth " + depth + " best move " + m);
        return current;
    }
//    private int negaMax(int depth){
//        if(depth==0){
//            //System.out.println("Evaluating... score=" + evaluate() + " | " + analysisBoard.blackArmy.size());
//            return evaluate();
//        }
//        Integer max = null;
//        Move m = null;
//        for(Move move: moves()){
//            analysisBoard.makeMove(move);
//            int score = -negaMax(depth - 1);
//            if(max == null) {max = score; m = move;}
//            analysisBoard.undoMove(move);
//            System.out.println("Depth " + depth + ", move " + move + ", score " + score + " complete.");
//            if(score > max){
//                max = score;
//
//            }
//        }
//        System.out.println("=== === === depth " + depth + " best move " + m);
//        return max;
//    }


    // Returns centipawn evaluation of the current analysisBoard state from the perspective
    // of the side who just made a move, so the opponent has the next move.
    public int evaluate(){
        boolean scoringFor = !analysisBoard.sideToMove;
        int scoreMultiplier = !analysisBoard.sideToMove?1:-1;
        int eval = 0;

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

        for(Piece p : analysisBoard.pieces){
            //if(analysisBoard.pieces.size() != 32) System.out.println("BEEEP BEEEP BEEP\nWARNING===\n=======\n=== === ===\n");
            int pieceMultiplier = p.color != analysisBoard.sideToMove ? 1 : -1;
            Set<Move> moves = p.getMoves();
            switch(p.type){
                case "Pawn":
                    eval += 100 * pieceMultiplier; // Piece value

                    // Center pawn bonus
                    if(p.position % 8 > 2 && p.position % 8 < 5 && p.position / 8 > 2 && p.position / 8 < 5){
                        eval += 33 * pieceMultiplier;
                    }
                    break;
                case "Knight":
                    eval += 295 * pieceMultiplier; // Piece value
                    break;
                case "Bishop":
                    eval += 305 * pieceMultiplier; // Piece value
                    break;
                case "Rook":
                    eval += 495 * pieceMultiplier; // Piece value

                    if(((Rook) p).connected()){

                    } else if(((Rook) p).orthogonalNoKing()){
                        eval += 18 * pieceMultiplier;
                    }
                    break;
                case "Queen":
                    eval += 900 * pieceMultiplier; // Piece value
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
            // Move count mobility bonus
            eval += moves.size()*pieceMultiplier;
            //if(eval < -700 || eval > 700) System.out.println("Rare event");analysisBoard.printBoardW();
        }


        //Set<Move> m = analysisBoard.getWhiteMoves();
        //Set<Move> m2 = analysisBoard.getBlackMoves();


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
            for(Piece p: projectors){
                // N, B or P threatens Q or R
                if((p.type.equals("Queen") || p.type.equals("Rook"))
                        && (m.piece().type.equals("Knight") || m.piece().type.equals("Bishop") || m.piece().type.equals("Pawn"))){
                    map.get("Threats").add(m);
                    analysisBoard.undoPrimitiveMove(m);
                    continue;
                }

                // P threatens to promote
                if(m.promotion().equals("Q") || m.promotion().equals("N")){
                    map.get("Threats").add(m);
                    analysisBoard.undoPrimitiveMove(m);
                    continue;
                }
            }

            analysisBoard.undoPrimitiveMove(m);
        }
        return map;
    }

    private int defense(Piece p){
        Set<Piece> defenders = p.terminalProjectors(true, p.position);

        // Remove pinned defenders
        return 0;
    }
}
