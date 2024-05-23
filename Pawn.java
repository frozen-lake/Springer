import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class Pawn extends Piece {

    public static final Set<String> promotions = new HashSet<String>(List.of(new String[]{"Q", "R", "N", "B"}));

    public Pawn(boolean color, int position, Board board){
        super(color, position, board, "Pawn");
    }
    public Set<Move> getMoves(){
        int forward = color ? position + 8 : position - 8;
        Set<Move> moves = new HashSet<Move>();

        int[] j = {forward + 1, forward - 1};
        for(int i: j){
            if(board.validPosition(i) && board.get(i) != null
                    && board.get(i).color != color){
                if(board.isPromotion(position, i)){
                    for(String p : promotions){moves.add(board.createMove(position, i, p));}
                } else {
                    moves.add(board.createMove(position, i));
                }
            }
        }
        if(board.validPosition(forward) && board.get(forward) == null) {

            if(board.isPromotion(position, forward)){
                for(String p : promotions){moves.add(board.createMove(position, forward, p));}

            } else {
                moves.add(board.createMove(position, forward));
            }

            int doubleForward = color ? forward + 8 : forward - 8;
            if(!hasMoved && board.validPosition(doubleForward) && board.get(doubleForward)==null){
                if(board.isPromotion(position, doubleForward)) {
                    for(String p : promotions){moves.add(board.createMove(position, doubleForward, p));}

                } else {
                    moves.add(board.createMove(position, doubleForward));
                }
            }
        }


        board.filterLegalMoves(moves);
        return moves;
    }

    //
    public void promote(String promotion){
        switch (promotion) {
            case "Q" -> board.set(position, new Queen(color, position, board));
            case "R" -> board.set(position, new Rook(color, position, board));
            case "N" -> board.set(position, new Knight(color, position, board));
            case "B" -> board.set(position, new Bishop(color, position, board));
        }
    }
    public String toString(){
        return this.color ? "P" : "p";
    }
}
