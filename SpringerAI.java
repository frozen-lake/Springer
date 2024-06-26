import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class SpringerAI {

    // Springer chess engine.

    public HashMap<Integer, HashMap<String, Integer[]>> squareValues;
    public static final int depth = 2; // Default search depth
    public static final int depthSpecial=2;
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

        squareValues = new HashMap<Integer, HashMap<String, Integer[]>>();
        squareValues.put(20, new HashMap<String, Integer[]>());
        squareValues.get(20).put("P", new Integer[]{0,0,0,0,0,0,0,0,2,2,2,0,0,1,3,1,1,1,3,1,1,0,1,0,0,0,4,25,25,12,0,0,0,0,6,22,22,6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
        squareValues.get(20).put("p", new Integer[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6,22,22,6,0,0,0,0,6,25,25,12,0,0,1,1,3,1,1,0,1,0,2,2,2,0,0,1,3,1,0,0,0,0,0,0,0,0});

        this.gameBoard = board;
        this.analysisBoard = new Board(board);
        this.colorPlaying = false;

    }
    public int getEval(){
        analysisBoard = new Board(gameBoard);
        analysisBoard.updateProperties();
        return evaluate(null, false) * (analysisBoard.sideToMove == colorPlaying ? 1 : -1);
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

            System.out.println("Discovered " + node);
            //if(move.piece().type.equals("Queen") && move.to() == 35 && move.capture() != null) System.out.println("d1 d5 qxP node children: " + node.children);
        }
        if(bestMove == null) throw new IllegalStateException();
        Move move;
        if(bestMove.promotion()==null){
            move = gameBoard.createMove(bestMove.from(), bestMove.to());
        } else {move = gameBoard.createMove(bestMove.from(), bestMove.to(), bestMove.promotion());}
        gameBoard.makeMove(move);


        System.out.println("Chosen move: " + move + ", score " + bestEval + ", new eval state " + evaluate(bestMove, false) + ", new piece a/d: " + move.piece().attackers + "/" + move.piece().defenders);
        System.out.println("bestNode's children: " + bestNode.children);
        System.out.println("bestNode: " + bestNode + "\nmoves " + analysisBoard.moves.size());
    }

    private Set<Move> moves(){
        return analysisBoard.sideToMove ? analysisBoard.getWhiteMoves() : analysisBoard.getBlackMoves();
    }


    private SearchNode minimax(int depth, Move m, boolean special){
        return minimaxMoveset(depth, m, special, moves());// special?getActiveMoves():moves());
    }

    private SearchNode minimaxMove(int depth, Move m, boolean special){
        return null;
    }

    private SearchNode minimaxMoveset(int depth, Move m, boolean special, Set<Move> moveSet){
        if(depth==0){
            //System.out.println("Evaluating... score=" + evaluate() + " | " + analysisBoard.blackArmy.size());

            // Special base case: Their king is in check, or
            if(!special && (analysisBoard.kingW.inCheck() || analysisBoard.kingB.inCheck())){
                return minimax(depthSpecial-1, m, true);
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
        if(moveSet.size() == 0) {
            current.score = analysisBoard.sideToMove?Integer.MIN_VALUE:Integer.MAX_VALUE;
            current.eval = analysisBoard.sideToMove?Integer.MIN_VALUE:Integer.MAX_VALUE;
            return current;
        }

        ArrayList<Move> moves = orderMoves(moveSet);

        Integer max = null;
        SearchNode maxNode = null;
        for(Move move: moves){

            analysisBoard.makeMove(move);
            SearchNode node = minimax(depth - 1, move, special);

            if(node != null) {
                int score = node.score;
                if (max == null ||  (!analysisBoard.sideToMove? score > max:score<max)){
                    max = score;
                    maxNode = node;
                }
                current.addChild(node);
            }
            analysisBoard.undoMove(move);
            //System.out.println("Depth " + depth + ", move " + move + ", score " + score + " complete.");
        }

        current.score = max;
        current.eval = evaluate(m, special) * (analysisBoard.sideToMove == colorPlaying ? 1 : -1);
        current.next = maxNode;
        //System.out.println("=== === === depth " + depth + " best move " + m);
        return current;
    }

    public ArrayList<Move> orderMoves(Set<Move> moves){
        ArrayList<Move> orderedMoves = new ArrayList<Move>();
        Map<String, Set<Move>> activeMoves = activeMoves(moves);
        Set<Move> passiveMoves = new HashSet<Move>();
        orderedMoves.addAll(activeMoves.get("Checks"));
        orderedMoves.addAll(activeMoves.get("Captures"));
        orderedMoves.addAll(activeMoves.get("Threats"));
        for(Move move : moves){
            if(!orderedMoves.contains(move)) orderedMoves.add(move);
        }


        if(moves.size() != orderedMoves.size()) throw new IllegalStateException("moves: "+moves.size()+", "+moves+"\nordered: " + orderedMoves.size() + ", " + orderedMoves);
        return orderedMoves;
    }

//    public Set<Move> getActiveMoves(Set<Move> moves){
//        Map<String, Set<Move>> map = activeMoves(moves);
//        Set<Move> activeMoves = map.get("Checks"); activeMoves.addAll(map.get("Captures")); moves.addAll(map.get("Threats"));
//        return activeMoves;
//    }

    // Returns centipawn evaluation of the current analysisBoard state from the perspective
    // of the side who just made a move, so the opponent has the next move.
    public int evaluate(Move m, boolean special){

        boolean scoringFor = !analysisBoard.sideToMove;
        int scoreMultiplier = !analysisBoard.sideToMove?1:-1;
        int eval = 0 * (scoringFor?1:-1);

        int threats = 0;
        String lowestThreatenedPiece = ""; // Lowest value piece threatened by the side we are evaluating from

        String highestThreatenedPiece = ""; // Highest value piece threatened by the enemy side
        int highestThreatLoss = 0; // Any material loss the enemy will suffer associated with their biggest threat

        //eval += 80 * (!scoringFor?1:-1);

        // Checkmate
        if((scoringFor&&analysisBoard.getBlackMoves().size()==0)||(!scoringFor&&analysisBoard.getWhiteMoves().size()== 0)) return Integer.MAX_VALUE;



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

        boolean enemyQueenThreatened = false;

        for(Piece p : analysisBoard.pieces()){
            int evalOld = eval;
            int pieceMultiplier = p.color != analysisBoard.sideToMove ? 1 : -1;
            Set<Move> moves = p.getMoves();
            int pieceValue = pieceValues.get(p.type);

            // Enemy and friendly projections
            Projection pieceProjection = new Projection(p.position, p.color, true, analysisBoard);
            switch(p.type){
                case "Pawn":
                    eval += p.getMoves().size() * pieceMultiplier;
                    King king = (p.color?analysisBoard.kingW:analysisBoard.kingB);
                    // Protecting the king
                    if(analysisBoard.moves.size() <= 60){
                        if(p.position%8 == king.position%8 || (king.position%8!=0&&p.position%8 == (king.position-1)%8) || (king.position%8!=7&&p.position%8==(king.position+1)%8)){
                            if((p.position/8 - king.position/8) < -2 || (p.position/8 - king.position/8) > 2){
                                // Not protecting
                                eval -= 6 * pieceMultiplier;
                            } else {
                                eval += 6 * pieceMultiplier;
                            }
                        }
                    }

                    // Apply early game pawn-square value matrix
                    if(analysisBoard.moves.size() <= 20){
                        eval += squareValues.get(20).get(p.toString())[p.position] * pieceMultiplier;
                    }
                    // Extra center pawn bonus
                    if(analysisBoard.moves.size() <= 20 && p.position % 8 > 2 && p.position % 8 < 5 && p.position / 8 > 2 && p.position / 8 < 5){
                        eval += 20 * pieceMultiplier;

                        // Defended center pawn bonus
                        for(Piece d: p.defenders){
                            if(d.type.equals("Pawn")){
                                eval += 10 * pieceMultiplier;
                            } else if(d.type.equals("Knight")){
                                eval += 8 * pieceMultiplier;
                            } else {
                                eval += 6 * pieceMultiplier;
                            }
                        }
                    }

                    // Bad mobility penalty
                    if(((Pawn) p).doubled()){ eval -= 20 * pieceMultiplier; }

                    // Blocking center pawn at original square penalty
                    int forward = p.color?p.position+8:p.position-8;
                    if((p.position % 8 == 3 || p.position % 8 == 4)
                            && ((p.color&&p.position/8==1)||(!p.color&&p.position/8==6))
                            && analysisBoard.validPosition(forward) && analysisBoard.get(forward) != null){
                        if(analysisBoard.get(forward).color == p.color){
                            eval -= 20 * pieceMultiplier;
                        }
                    }

                    pieceProjection.projectPawnAttack(false);
                    for(ProjectionNode node: pieceProjection.nodes){
                        if(node.dest != null && node.dest.color == p.color){
                            // Protecting a pawn or piece bonus
                            eval += 2 * pieceMultiplier;
                            // Pawn defend pawn bonus
                            if(node.dest.type.equals("Pawn")) eval += 2 * pieceMultiplier;
                        }
                    }
                    break;
                case "Knight":
                    eval += 2*p.getMoves().size()*pieceMultiplier;
                    if(analysisBoard.moves.size()<=12){
                        if(p.position == p.originalPosition){
                            eval-=8*pieceMultiplier;
                        }
                        if((p.color&&p.position/8>2)||(!p.color&&p.position/8<5)){
                            eval -= 12 * pieceMultiplier;
                        }
                    }

                    // Score for legal moves to a square not threatened by an enemy pawn
                    for(Move move: p.getMoves()){
                        analysisBoard.primitiveMove(move, false);
                        if(!p.attackedByPawn()) eval += 2 * pieceMultiplier;
                        analysisBoard.undoPrimitiveMove(move, false);

                    }
                    // Early game development
                    if(analysisBoard.moves.size() <= 16){
                        if((p.color&&p.position/8<7) || (!p.color&&p.position/8>0)){
                            eval += 10 * pieceMultiplier;
                        }
                    }

                    // Knight on the rim is dim
                    if(p.position%8==0 || p.position%8==7) eval -= 6 * pieceMultiplier;
                    break;

                case "Bishop":
                    eval += 2*p.getMoves().size()*pieceMultiplier;
                    if(analysisBoard.moves.size()<=12 && p.position == p.originalPosition){eval-=8*pieceMultiplier;}
                    break;

                case "Rook":
                    eval += p.getMoves().size()*pieceMultiplier;
                    Rook r = (Rook) p;
                    if(r.connected()){
                        eval += 7 * pieceMultiplier;
                    } else if(r.orthogonalNoKing()){
                        eval += 4 * pieceMultiplier;
                    }
                    if(analysisBoard.moves.size() <= 10 && p.position != p.originalPosition) eval -= 8 * pieceMultiplier;

                    if(r.onOpenFile()){
                        eval += 20 * pieceMultiplier;
                    } else if(r.onSemiOpenFile()){
                        eval += 13 * pieceMultiplier;
                    }

                    if(analysisBoard.moves.size() <= 40){
                        eval -= 50 * pieceMultiplier;
                    }


                    break;
                case "Queen":
                    eval += 1.5*p.getMoves().size()*pieceMultiplier;
                    if(analysisBoard.moves.size()<=24 && p.position != p.originalPosition){
                        eval-=22 *pieceMultiplier;
                        if(analysisBoard.moves.size() <= 12) eval -= 22 *pieceMultiplier;
                    }
                    break;
                case "King":
                    King k = (King) p;

                    // Subtract points for King above home rank
                    if(analysisBoard.moves.size() < 16 && ((k.color&&k.position/8!=0)||(!k.color&&k.position/8!=7))) {
                        eval -= 60 * pieceMultiplier;
                    }

                    // Add points if castled, add less points if can castle
                    if(((King) p).castled != null) eval += 16 * pieceMultiplier;
                    if(((King) p).canCastleShort() || ((King) p).canCastleLong()){
                        eval += 6 * pieceMultiplier;
                    }

                    // TO-DO: Subtract points for 3+ pieces near king,
                    // subtract points for too much luft or back-rank issues

                    break;
                }
                pieceProjection.clear();

                if(p.color==scoringFor){

                    // Undefended pieces at end of turn penalty
//                    if(!p.type.equals("King") && !p.type.equals("Queen")&&!p.type.equals("Pawn") && p.defenders.size() == 0){
//                        eval -= (pieceValue / 10) * pieceMultiplier;
//                        if(p.attackers.size() > 0) eval -= (20) * pieceMultiplier;
//                    }


                    // Calculate threats on friendly piece
                    if(!p.type.equals("King") && !p.type.equals("Pawn") && (p.attackedByPawn() || (!p.protectedByPawn() && !p.defended()))){

                        if(highestThreatenedPiece.equals("") || pieceValues.get(p.type) > pieceValues.get(highestThreatenedPiece)){
                            highestThreatenedPiece = p.type;
                            if(p.defended()) highestThreatLoss = pieceValues.get("Pawn");
                        }

                    } else if(p.type.equals("Pawn")){
                        if(!p.defended()){
                            if(p.protectedByPawn()){
                                boolean attackedByPawn = false;
                                for(Piece attacker: p.attackers){
                                    if(attacker.type.equals("Pawn")) {
                                        attackedByPawn = true;break;
                                    }
                                }
                                if(attackedByPawn){
                                    if(highestThreatenedPiece.equals("") || pieceValues.get(p.type) > pieceValues.get(highestThreatenedPiece)){
                                        highestThreatenedPiece = p.type;
                                    }

                                }
                            } else {
                                if(highestThreatenedPiece.equals("") || pieceValues.get(p.type) > pieceValues.get(highestThreatenedPiece)){
                                    highestThreatenedPiece = p.type;
                                }
                            }
                        }
                    } else if(!p.type.equals("King") && !p.protectedByPawn() && !p.defended()) {
                        // Piece attacking piece
                        String type = null;
                        if (p.attackers.size() != 0) {
                            for (Piece attacker : p.attackers) {
                                if (type == null) {
                                    type = attacker.type;
                                    continue;
                                }
                                if (attacker.type.equals("Pawn")) {
                                    type = "Pawn";
                                    break;
                                } else if (attacker.type.equals("Bishop")) {
                                    type = "Bishop";
                                } else if (attacker.type.equals("Knight")) {
                                    type = "Knight";
                                } else if (attacker.type.equals("Rook") && !type.equals("Knight") && !type.equals("Bishop")) {
                                    type = "Rook";
                                }
                            }
                        }
                        if (type != null) {
                            if(highestThreatenedPiece.equals("") || pieceValues.get(p.type) > pieceValues.get(highestThreatenedPiece)){
                                highestThreatenedPiece = p.type;
                            }
                            //eval -= pieceValue * pieceMultiplier;
                            //eval += pieceValues.get(type);
                        }
                    }
                } else { // we know p.color != scoringFor
                    // Calculate threats on enemy piece
                    if(!p.type.equals("Pawn") && p.attackedByPawn()){ // Piece threatened by pawn
                        switch(p.type){
                            case "Queen":
                                enemyQueenThreatened = true;
                                if(!lowestThreatenedPiece.equals("Pawn")&&!lowestThreatenedPiece.equals("Rook")&&!lowestThreatenedPiece.equals("Knight")&&!lowestThreatenedPiece.equals("Bishop")) lowestThreatenedPiece = "Queen";
                                break;
                            case "Rook":
                                if(!lowestThreatenedPiece.equals("Pawn")&&!lowestThreatenedPiece.equals("Knight")&&!lowestThreatenedPiece.equals("Bishop")) lowestThreatenedPiece = "Rook";
                                break;
                            case "Knight":
                                if(!lowestThreatenedPiece.equals("Pawn")) lowestThreatenedPiece = "Knight";
                                break;
                            case "Bishop":
                                if(!lowestThreatenedPiece.equals("Pawn") && !lowestThreatenedPiece.equals("Knight")) lowestThreatenedPiece = "Bishop";
                                break;
                            case "King":
                                break;
                        }
                        threats++;
                    } else if(!p.type.equals("Pawn") && !p.protectedByPawn() && !p.defended()){
                        // Piece that could be captured to win material
                        switch(p.type){
                            case "Queen":
                                enemyQueenThreatened = true;
                                if(!lowestThreatenedPiece.equals("Pawn")&&!lowestThreatenedPiece.equals("Rook")&&!lowestThreatenedPiece.equals("Knight")&&!lowestThreatenedPiece.equals("Bishop")) lowestThreatenedPiece = "Queen";

                                break;
                            case "Rook":
                                if(!lowestThreatenedPiece.equals("Pawn")&&!lowestThreatenedPiece.equals("Knight")&&!lowestThreatenedPiece.equals("Bishop")) lowestThreatenedPiece = "Rook";
                                break;
                            case "Knight":
                                if(!lowestThreatenedPiece.equals("Pawn")) lowestThreatenedPiece = "Knight";
                                break;
                            case "Bishop":
                                if(!lowestThreatenedPiece.equals("Pawn") && !lowestThreatenedPiece.equals("Knight")) lowestThreatenedPiece = "Bishop";
                                break;
                            case "King":
                                break;
                        }
                        threats++;
                    } else if(p.type.equals("Pawn") && !p.protectedByPawn() && !p.defended()){
                        //eval += 4 * pieceMultiplier;
                        threats++;
                        lowestThreatenedPiece = "Pawn";
                        // Pawn that could be captured
                    }
                }


                // If a piece is deep in enemy territory early game and undefended, it is a liability.
            if(analysisBoard.moves.size() <= 12 && !p.type.equals("Pawn")
                    &&!p.type.equals("Bishop") && p.defenders.size() == 0
                    &&((p.color&&p.position/8>2) || (!p.color&&p.position/8<5))
            ){
                eval -= 10 * pieceMultiplier;
                if(p.attackers.size() > 0) eval -= 10 * pieceMultiplier;
            }

            if(analysisBoard.moves.size() <= 12 && (p.type.equals("Bishop")||p.type.equals("Knight")) && p.position == p.originalPosition) eval -= 6 * pieceMultiplier;

            eval += pieceValue*pieceMultiplier;

            p.eval = (eval - evalOld) * pieceMultiplier;

            //System.out.println("Post piece eval: " + p.toStringDebug() + "\nEVAL: " +  eval * (analysisBoard.sideToMove == colorPlaying ? 1 : -1));

        }

        if(!highestThreatenedPiece.equals("")) {
            eval -= pieceValues.get(highestThreatenedPiece);// * (scoringFor?1:-1);
            eval += highestThreatLoss;// * (scoringFor?1:-1);
        }

        switch(threats){
            case 0:
                break;
            case 1:
                eval += 10;// * (scoringFor?1:-1);
                break;
            default:
                eval += pieceValues.get(lowestThreatenedPiece);//*(scoringFor?1:-1);
                //eval += 15 * threats ;//* (scoringFor?1:-1);
        }

        return eval;
        }

    public void debugPieces(){
        ArrayList<String> l = new ArrayList<String>(32);
        for(int i=0;i<32;i++){l.add("");}
        for(Piece p: analysisBoard.pieces()){
            if(p.originalPosition>=48){
                l.set(p.originalPosition - 32, p.toStringDebug());
            } else {
                l.set(p.originalPosition, p.toStringDebug());
            }
        }

        for(String s: l){
            System.out.println(s);
        }

    }


    // Filters a set of moves into a map divided by checks, captures, and threats.
    public Map<String, Set<Move>> activeMoves(Set<Move> s){
        HashMap<String, Set<Move>> map = new HashMap<String, Set<Move>>();
        map.put("Checks", new HashSet<Move>());
        map.put("Captures", new HashSet<Move>());
        map.put("Threats", new HashSet<Move>());
        Iterator<Move> iter = s.iterator();
        Move m;
        while(iter.hasNext()){
            m = iter.next();
            analysisBoard.primitiveMove(m, true);

            // Checks
            if(m.piece().color?analysisBoard.kingB.inCheck():analysisBoard.kingW.inCheck()){
                map.get("Checks").add(m);
                analysisBoard.undoPrimitiveMove(m, true);
                continue;
            }

            // Captures
            if(m.capture() != null){
                map.get("Captures").add(m);
                analysisBoard.undoPrimitiveMove(m, true);
                continue;
            }

            boolean flag = false;
            // Threats
            for(Move move: m.piece().getMoves()){
                if(move.capture() != null ){
                    if(!move.capture().type.equals("Pawn")){
                        if(!move.capture().defended()){
                            map.get("Threats").add(m);
                            analysisBoard.undoPrimitiveMove(m, true);
                            flag = true; break;
                        }
                    } else if(!move.capture().defended()){
                        map.get("Threats").add(m);
                        analysisBoard.undoPrimitiveMove(m, true);
                        flag = true; break;

                    }
                }
            }

            if(!flag) analysisBoard.undoPrimitiveMove(m, true);
        }
        return map;
    }

}
