import java.util.*;

public class SpringerAI {

    public static final int depth = 3;
    public Board analysisBoard; // Analysis board
    public final Board gameBoard;
    public SpringerAI(Board board)    {
        this.gameBoard = board;
        this.analysisBoard = new Board(board);
    }
    public void takeTurn(){
        analysisBoard = new Board(gameBoard);
        Set<Move> s = analysisBoard.sideToMove ? analysisBoard.getWhiteMoves() : analysisBoard.getBlackMoves();
        Move bestMove = null;
        int eval = 0;

        for(Move move : s){
            analysisBoard.makeMove(move);
            int n = negaMax(depth);
            if(bestMove == null || n > eval){ bestMove = move; eval = n;}

            analysisBoard.undoMove(move);
        }
        gameBoard.makeMove(bestMove);
        System.out.println("Chosen move: " + bestMove);
    }

    private Set<Move> moves(){
        return analysisBoard.sideToMove ? analysisBoard.getWhiteMoves() : analysisBoard.getBlackMoves();
    }

    private int negaMax(int depth){
        if(depth==0){
            //System.out.println("Evaluating... score=" + evaluate() + " | " + analysisBoard.blackArmy.size());
            return evaluate();
        }
        int max = 0;
        for(Move move: moves()){
            analysisBoard.makeMove(move);
            int score = negaMax(depth - 1);
            analysisBoard.undoMove(move);
            // System.out.println("Depth " + depth + ", move " + move + ", score " + score + " complete.");
            if(score > max){
                max = score;
            }
        }
        return max;
    }
    public int materialBalance(){
        int eval = 0;
        for(Piece p: analysisBoard.pieces){
            int k = (p.color)?1:-1;
            switch (p.type) {
                case "Pawn" -> eval += k * 100;
                case "Rook" -> eval += k * 500;
                case "Queen" -> eval += k * 1000;
                case "Knight" -> eval += k * 295;
                case "Bishop" -> eval += k * 310;
            }
        }
        return eval;
    }


    // Returns centipawn evaluation of the current analysisBoard state from the perspective
    // of the side who just made a move, so the opponent has the next move.
    public int evaluate(){
        int eval = 0;

        // Piece value
        eval += analysisBoard.sideToMove?materialBalance():-materialBalance();

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
