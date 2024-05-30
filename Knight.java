
import java.util.HashSet;
import java.util.Set;
public class Knight extends Piece {

    public Knight(boolean color, int position, Board board){
        super(color, position, board, "Knight");
    }
    public Knight(Knight p, Board board){super(p, board);}

    public void updateMoves(){
        int[] around = {position - 6, position + 6, position - 10, position + 10,
                position - 15, position + 15, position - 17, position + 17};
        Set<Move> moves = new HashSet<Move>();
        for(int j : around){
            if(board.validPosition(j) && (board.get(j)==null || board.get(j).color != color) && !((position%8<=1&&j%8>=6) || (position%8>=6&&j%8<=1))){
                moves.add(board.createMove(position, j));
            }
        }
        board.filterLegalMoves(moves);
        this.legalMoves = moves;
    }
    public String toString(){
        return this.color ? "N" : "n";
    }
}
