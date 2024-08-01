import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SpringerSearch {

    public Board gameBoard;
    public Board analysisBoard;
    public SpringerAI sp;

    public SearchNode mainNode;
    public Move move;

    public SpringerSearch(SpringerAI sp){
        this.sp = sp;
        this.analysisBoard = sp.analysisBoard;
        this.gameBoard = sp.gameBoard;
        this.mainNode = null;
        this.move = null;
    }

    public SearchNode getNode(){
        return mainNode;
    }
    public Move getMove() {
        return this.move;
    }
    public void updateNode(){

        Set<Move> s = analysisBoard.sideToMove ? analysisBoard.getWhiteMoves() : analysisBoard.getBlackMoves();
        SearchNode parentNode = new SearchNode(analysisBoard.moves.get(analysisBoard.moves.size() - 1));
        parentNode.eval = sp.evaluate(parentNode.move);
        SearchNode bestNode = null;
        Move bestMove = null;
        Integer bestEval = null; // search eval of best move
        int i=1;
        for(Move move : s){
            analysisBoard.makeMove(move);
            SearchNode node = minimax(SpringerAI.depth, parentNode, move);
            int n = node.score;
            node.parent = parentNode;
            analysisBoard.undoMove(move);
            if(bestMove == null || (n < bestEval)){
                bestMove = move; bestEval = n; bestNode = node;
            }


            // Debug
            System.out.println("Discovered " + node + ", " + i + "/" + s.size());
            i++;
            SearchNode nrr = node;
            while(nrr.next!=null){nrr=nrr.next;}
//            System.out.println(nrr.children.size() + " children at leaf");
//            nrr.printChildren(0);
//
//            System.out.println("parent: " + nrr.parent);
//            nrr.parent.printChildren(0);
        }
        if(bestMove == null) throw new IllegalStateException();

        Move move;
        if(bestMove.promotion()==null){
            move = gameBoard.createMove(bestMove.from(), bestMove.to());
        } else {move = gameBoard.createMove(bestMove.from(), bestMove.to(), bestMove.promotion());}
        this.move = move;
        parentNode.next=bestNode;
        parentNode.score=bestNode.score;
        this.mainNode = parentNode;



    }

    // First call to quiesce
    public SearchNode quiesce(SearchNode parent, Move m){

        return quiesce(0, parent, m);
    }

    // Recursive call to quiesce
    private SearchNode quiesce(int depth, SearchNode parent, Move m){
        int standpat = sp.fullStandpat();
        SearchNode current = new SearchNode(m, parent); // The SearchNode we will be returning. Alpha/Beta are inherited from parent
        current.type = "Q";
        current.eval = sp.evaluate(m);
        int fullStandpat = sp.fullStandpat();
        // Upon entering quiescence, use standpat to establish a lower bound
        if(depth>=0) {
//            if (analysisBoard.sideToMove && standpat > current.alpha) current.alpha = standpat;
//            if (!analysisBoard.sideToMove && standpat < current.beta) current.beta = standpat;
            if(fullStandpat > current.alpha) current.alpha = standpat;
            if(fullStandpat < current.beta) current.beta = standpat;
        }

        // Move ordering
        ArrayList<Move> moves;
        if((analysisBoard.sideToMove?analysisBoard.kingW:analysisBoard.kingB).inCheck()){
            moves = sp.orderMoves(sp.moves());
            if(moves.size() == 0) { // In checkmate
                current.type = "CM";
                current.score = analysisBoard.sideToMove?Integer.MIN_VALUE:Integer.MAX_VALUE;
                current.eval = analysisBoard.sideToMove?Integer.MIN_VALUE:Integer.MAX_VALUE;
                current.next = null;
                return current;
            }
        } else {
            moves = sp.quiescenceMoves();
        }

        if(moves.size() == 0) { // No captures, quiescent position
            current.type = "QP";
            current.score = fullStandpat;
            current.eval = fullStandpat;
            current.next = null;
            return current;
        }

        SearchNode maxNode = null;
        for(Move move: moves){

            analysisBoard.makeMove(move);
            SearchNode node = quiesce(depth+1, current, move);

            if(!analysisBoard.sideToMove) {
                if (node.score >= current.beta) { // fail-hard beta cutoff
                    current.type = "QBC";
                    analysisBoard.undoMove(move);
                    current.score = node.score;//Integer.MIN_VALUE;
                    //current.eval = sp.evaluate(m);
                    current.next = node;
                    current.children.clear();
                    return current;
                }
                if (node.score > current.alpha) {
                    current.type = "A";
                    current.alpha = node.score;
                    current.score = node.score;
                    //current.eval = sp.evaluate(m);
                    current.next = node;
                    maxNode = node;
                }
            } else {
                if (node.score <= current.alpha) { // fail-hard alpha cutoff
                    current.type = "QAC";
                    analysisBoard.undoMove(move);
                    current.score = node.score;//Integer.MAX_VALUE;
                    current.next = node;
                    current.children.clear();
                    return current;
                }
                if (node.score < current.beta) {
                    current.type = "B";
                    current.beta = node.score;
                    current.score = node.score;
                    current.next = node;
                    maxNode = node;
                }
            }

            analysisBoard.undoMove(move);
        }
        Collections.sort((List) current.children, Collections.reverseOrder());

        if(maxNode == null){
            maxNode = analysisBoard.sideToMove?current.children.get(0):current.children.get(current.children.size()-1);
            current.score = maxNode.score;
            current.next = maxNode;
            current.type += "QJ";
        }
        // If standpat beats the best branch of the current node, opt for standpat score
        if(((!analysisBoard.sideToMove && (current.score > fullStandpat))||(analysisBoard.sideToMove && (current.score < fullStandpat)))) { // Option to take stand pat instead of executing capture
            current.score = fullStandpat;
            current.next = null;
            current.type = "S";
            return current;
        }
        return current;
    }


    public SearchNode minimax(int depthRemaining, SearchNode parent, Move m){
        if(depthRemaining==0){
            // Standard base case
            return quiesce(parent, m);
        }


        SearchNode current = new SearchNode(m, parent); // The SearchNode we will be returning. Alpha/Beta are inherited from parent
        current.eval = sp.evaluate(m);
        // Move ordering
        ArrayList<Move> moves = sp.orderMoves(sp.moves());
        if(moves.size() == 0) {
            current.score = analysisBoard.sideToMove?Integer.MIN_VALUE:Integer.MAX_VALUE;
            current.eval = analysisBoard.sideToMove?Integer.MIN_VALUE:Integer.MAX_VALUE;
            current.next = null;
            return current;
        }

        SearchNode maxNode = null;
        for(Move move: moves){

            analysisBoard.makeMove(move);
            //SearchNode node = minimax(depth - 1, move, alpha, beta);
            SearchNode node = minimax(depthRemaining-1, current, move);

            if(node != null) {
                int score = node.score;// * (analysisBoard.sideToMove ? 1 : -1);

                if(!analysisBoard.sideToMove) {
                    if (score >= current.beta) { // fail-hard beta cutoff
                        current.type = "BC";
                        analysisBoard.undoMove(move);
                        current.score = score;//Integer.MAX_VALUE;
                        current.next = node;
                        current.children.clear();
                        return current;
                    }
                    if (score > current.alpha) {
                        current.alpha = score;
//                        current.score = score;
//                        current.next = node;
//                        maxNode = node;
                    }
                } else {
                    if (score <= current.alpha) { // fail-hard alpha cutoff
                        current.type = "AC";
                        analysisBoard.undoMove(move);
                        current.score = score;//Integer.MIN_VALUE;
                        current.next = node;
                        current.children.clear();
                        return current;
                    }
                    if (score < current.beta) {
                        current.beta = score;
//                        current.score = score;
//                        current.next = node;
//                        maxNode = node;
                    }
                }

            }
            analysisBoard.undoMove(move);
        }

        Collections.sort((List) current.children, Collections.reverseOrder());

        maxNode = analysisBoard.sideToMove?current.children.get(0):current.children.get(current.children.size()-1);
        current.score = maxNode.score;
        current.next = maxNode;

        return current;
    }
}
