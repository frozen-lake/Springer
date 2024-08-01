import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class SpringerAI {

    // Springer chess engine.

    public HashMap<Integer, HashMap<String, Integer[]>> squareValues;
    public static final int depth = 1; // Default search depth
    public static final int depthSpecial=2;
    public Board analysisBoard; // Analysis board
    public final Board gameBoard; // Real board
    public boolean colorPlaying; // Side the AI is playing for

    public SpringerAI(Board board){
//        this.pieceValues = new HashMap<String, Integer>();
//        pieceValues.put("Pawn", 100);
//        pieceValues.put("Knight", 295);
//        pieceValues.put("Bishop", 305);
//        pieceValues.put("Rook", 495);
//        pieceValues.put("Queen", 900);
//        pieceValues.put("King", 9999);

        squareValues = new HashMap<Integer, HashMap<String, Integer[]>>();
        squareValues.put(20, new HashMap<String, Integer[]>());
        squareValues.get(20).put("P", new Integer[]{
                0,0,0,0,0,0,0,0,8,8,8,2,2,16,16,16,6,6,4,10,10,0,6,6,-2,-4,8,35,35,12,-6,-4,0,0,10,28,28,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
        squareValues.get(20).put("p", new Integer[]{
                0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,10,28,28,10,0,0,-4,-6,12,35,35,8,-4,-2,6,6,0,10,10,4,6,6,16,16,16,2,2,8,8,8,0,0,0,0,0,0,0,0});

        this.gameBoard = board;
        this.analysisBoard = new Board(board);
        this.colorPlaying = false;

    }
    public int getEval(){
        analysisBoard = new Board(gameBoard);
        analysisBoard.updateProperties();
        return evaluate(null);
    }

    // takeTurn(): Creates an analysis board of the current board state. There it searches for the best move,
    // which it then executes on the game board.
    public void takeTurn(){
        analysisBoard = new Board(gameBoard);
        SpringerSearch search = new SpringerSearch(this);
        search.updateNode();
        SearchNode mainNode = search.getNode();
        Move move = search.getMove();
        gameBoard.makeMove(move);


        System.out.println("Chosen move: " + move + ", score " + mainNode.score + ", new eval state " + evaluate(move) + ", new piece a/d: " + move.piece().attackers + "/" + move.piece().defenders);
        System.out.println("mainNode's children: " + mainNode.children);
        System.out.println("mainNode: " + mainNode + "\nmoves " + analysisBoard.moves.size());

        mainNode.printDebugDown();
    }

    protected Set<Move> moves(){
        return analysisBoard.sideToMove ? analysisBoard.getWhiteMoves() : analysisBoard.getBlackMoves();
    }

    public ArrayList<Move> quiescenceMoves(){
        // Assume side to move is not in check

        Map<String, Set<Move>> activeMoves = activeMoves(moves());
        Set<Move> captureSet = activeMoves.get("Captures");

        ArrayList<Move> captures = new ArrayList<Move>(captureSet);
//        moves.sort(Collections.reverseOrder()); // Sort by MVV-LVA
        sortCaptures(captures);
        return captures;
    }

    // Sorts captures according to MVV-LVA
    public void sortCaptures(ArrayList<Move> captures){
        captures.sort(Collections.reverseOrder());
    }
    public ArrayList<Move> orderMoves(Set<Move> moves){
        ArrayList<Move> orderedMoves = new ArrayList<Move>();
        Map<String, Set<Move>> activeMoves = activeMoves(moves);
        ArrayList<Move> captures = new ArrayList<Move>(activeMoves.get("Captures"));
        sortCaptures(captures);
        orderedMoves.addAll(captures);
        orderedMoves.addAll(activeMoves.get("Checks"));
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

    // Returns regular evaluation after a null move is made.
    public int fullStandpat(){

        Move nullMove = analysisBoard.generateNullMove();
        analysisBoard.makeMove(nullMove);

        int k = evaluate(nullMove);

        analysisBoard.undoMove(nullMove);
        return k;
    }
    // Returns static piece-only evaluation after a null move is made.
    public int standpat(){
        Move nullMove = analysisBoard.generateNullMove();
        analysisBoard.makeMove(nullMove);

        int k = 0;
        for(Piece p : analysisBoard.pieces()){
            k += p.value * (p.color?1:-1);
        }

        analysisBoard.undoMove(nullMove);
        return k;
    }

    // Returns centipawn evaluation of the current analysisBoard state from the perspective
    // of the side who just made a move, so the opponent has the next move.
    public int evaluate(Move m){

        int eval = 0;

        //eval += 80 * (!scoringFor?1:-1);

        // Checkmate
        if((analysisBoard.getBlackMoves().size()==0)) return Integer.MAX_VALUE;
        if((analysisBoard.getWhiteMoves().size()==0)) return Integer.MIN_VALUE;

//        for(Piece p : analysisBoard.pieces()){
//            eval += p.value * (p.color?1:-1);
//        }
//        return eval;

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
            int pieceMultiplier = p.color?1:-1;
            Set<Move> moves = p.getMoves();
            int pieceValue = p.value;

            // Enemy and friendly projections
            switch(p.type){
                case "Pawn":
                    eval += p.getMoves().size() * pieceMultiplier;

                    // Apply early game pawn-square value matrix
                    if(analysisBoard.moves.size() <= 20){
                        eval += squareValues.get(20).get(p.toString())[p.position] * pieceMultiplier;
                    }
                    // Extra center pawn bonus
                    if(analysisBoard.moves.size() <= 20 && p.position % 8 > 2 && p.position % 8 < 5 && p.position / 8 > 2 && p.position / 8 < 5){
                        eval += 20 * pieceMultiplier;
                    }

                    // Bad mobility penalty
                    if(((Pawn) p).doubled()){ eval -= 20 * pieceMultiplier; }

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
                    eval += 10*pieceMultiplier;
                    if(analysisBoard.moves.size()<=12 && p.position == p.originalPosition){eval-=8*pieceMultiplier;}
                    break;

                case "Rook":
                    eval += p.getMoves().size()*pieceMultiplier;
                    if(analysisBoard.moves.size() <= 10 && p.position != p.originalPosition) eval -= 8 * pieceMultiplier;

//                    Rook r = (Rook) p;
//                    if(r.connected()){
//                        eval += 7 * pieceMultiplier;
//                    } else if(r.orthogonalNoKing()){
//                        eval += 4 * pieceMultiplier;
//                    }
//
//                    if(r.onOpenFile()){
//                        eval += 20 * pieceMultiplier;
//                    } else if(r.onSemiOpenFile()){
//                        eval += 13 * pieceMultiplier;
//                    }


                    break;
                case "Queen":
                    eval += 1*p.getMoves().size()*pieceMultiplier;
                    if(analysisBoard.moves.size()<=12 && p.position != p.originalPosition){
                        eval-=15 *pieceMultiplier;
                    }

                    break;
                case "King":
                    King k = (King) p;

                    // Subtract points for King above home rank
                    if(analysisBoard.moves.size() < 16 && ((k.color&&k.position/8!=0)||(!k.color&&k.position/8!=7))) {
                        eval -= 66 * pieceMultiplier;
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

            eval += pieceValue*pieceMultiplier;

        }

        eval += (analysisBoard.sideToMove?analysisBoard.pieces.size():-analysisBoard.pieces.size());

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

        int i=0;
        for(String s: l){
            if(!s.equals("")){
                System.out.println(s);
            } else {
                System.out.println("piece gone: " + (i<=15?i:i+32));
            }
            i++;
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
