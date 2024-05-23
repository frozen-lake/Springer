import java.util.Set;
import java.util.HashSet;

public class King extends Piece {

    public King(boolean color, int position, Board board){
        super(color, position, board, "King");
    }
    public Set<Move> getMoves() {
        int[] around = {position - 1, position + 1, position - 7, position + 7,
                position - 8, position + 8, position - 9, position + 9};
        Set<Move> moves = new HashSet<Move>();
        for(int j : around){
            if(board.validPosition(j)){
                moves.add(board.createMove(position, j));
            }
        }
        board.filterLegalMoves(moves);
        return moves;
    }

    public boolean inCheck(){
        // Check hostile pawn diagonals, bishops and queens on diagonals, rooks and queen on horizontals,
        // enemy king on all surrounding squares, and all 8 knight squares.

        Set<Move> straightProjections = board.straightProjection(position);
        Set<Move> diagonalProjections = board.diagonalProjection(position);

        // Rooks, queens, bishops
        for(Move proj: straightProjections){
            if(board.get(proj.to()) != null && board.get(proj.to()).color != color){
                if(board.get(proj.to()).type.equals("Rook")||board.get(proj.to()).type.equals("Queen")){
                    return true;
                }
            }
        }
        for(Move proj: diagonalProjections){
            if(board.get(proj.to()) != null && board.get(proj.to()).color != color){
                if(board.get(proj.to()).type.equals("Bishop")||board.get(proj.to()).type.equals("Queen")){
                    return true;
                }
            }
        }

        // Enemy king
        int[] around = {position - 1, position + 1, position - 7, position + 7,
                position - 8, position + 8, position - 9, position + 9};
        for(int j : around){
            if(board.validPosition(j) && board.get(j) != null && board.get(j).type.equals("King")){
                return true;
            }
        }

        // Knights
        around = new int[]{position - 6, position + 6, position - 10, position + 10,
                position - 15, position + 15, position - 17, position + 17};

        // Enemy pawn
        int forward = color ? position + 8 : position - 8;
        if(board.validPosition(forward-1) && board.get(forward-1) != null && board.get(forward-1).type.equals("Pawn") && board.get(forward-1).color != color){
            return true;
        }
        if(board.validPosition(forward+1) && board.get(forward+1) != null && board.get(forward+1).type.equals("Pawn") && board.get(forward+1).color != color){
            return true;
        }

        return false;
    }
    public String toString(){
        return this.color ? "K" : "k";
    }
}
