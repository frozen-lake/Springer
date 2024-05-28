import java.util.Set;

public class Rook extends Piece {

    public Rook otherRook; // Other rook of same color

    public Rook(boolean color, int position, Board board){
        super(color, position, board, "Rook");
    }
    public Rook(Rook p, Board board){super(p, board);}

    public Set<Move> getMoves(){
        Projection p = new Projection(position, true, board);
        p.projectStraight();
        Set<Move> moves = p.moves();
        board.filterLegalMoves(moves);
        return moves;
    }

    // Returns true if there is a horizontal or vertical line from this rook to the other with no king between.

    public boolean orthogonalNoKing(){
        if(otherRook == null) return false;
        King k = color?board.kingW:board.kingB;
        if(position/8 == otherRook.position/8){
            if(k.position/8 == position/8){
                return (position < k.position && otherRook.position < k.position)
                        || (position > k.position && otherRook.position > k.position);
            } else {return true;}
        } else if(position%8 == otherRook.position%8){
            if(k.position%8 == position%8){
                return (position/8 < k.position/8 && otherRook.position/8 < k.position/8)
                        || (position/8 > k.position/8 && otherRook.position/8 > k.position/8);
            } else {return true;}

        }
        // Not orthogonal
        return false;

    }

    public boolean orthogonal(){
        if(otherRook == null) return false;
        return position/8 != otherRook.position/8 && position%8 != otherRook.position%8;

    }

    public boolean connected(){
        return false;
//        if(position/8 == otherRook.position/8){
//            for(int i=(position%8 <= otherRook.position%8 ? position+1 : otherRook.position+1);i%8<otherRook.position%8;i++){
//                if(board.get(i) != null ) return false;
//            }
//        } else if(position%8 == otherRook.position%8){
//            for(int i=(position/8 <= otherRook.position/8 ? position+8 : otherRook.position+8);i){
//
//            }
//        }
//        return false;
    }
    public String toString(){
        return this.color ? "R" : "r";
    }
}
