import java.util.Set;

public class Rook extends Piece {

    public Rook otherRook; // Other rook of same color

    public Rook(boolean color, int position, Board board){
        super(color, position, board, "Rook");
    }
    public Rook(Rook p, Board board){super(p, board);}

    public void updateMoves(){
        Projection p = new Projection(position, color, board);
        p.projectStraight(true);
        Set<Move> moves = p.moves();
        board.filterLegalMoves(moves);
        this.legalMoves = moves;
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

    // Returns true if there is no pawn of the same color as the rook, on the file the rook is on.
    public boolean onSemiOpenFile(){
        for(int i=position%8;i<position;i+=8){
            if(!board.validPosition(i)) throw new IllegalStateException();
            if(board.get(i) != null && board.get(i).type.equals("Pawn") && board.get(i).color==color) return false;
        }
        return true;
    }
    public boolean onOpenFile(){
        for(int i=position%8;i<position;i+=8){
            if(!board.validPosition(i)) throw new IllegalStateException();
            if(board.get(i) != null && board.get(i).type.equals("Pawn")) return false;
        }
        return true;
    }
    public boolean connected(){

        if(position/8 == otherRook.position/8){
            if(position%8 < otherRook.position%8){
                for(int i = position+1;i%8<otherRook.position%8;i++){
                    if(board.get(i)!=null)return false;
                    if(!board.validPosition(i)) break;
                }
            } else {
                for(int i=otherRook.position-1;i%8>position%8;i--){
                    if(!board.validPosition(i)) break;
                    if(board.get(i)!=null)return false;
                }
            }
        } else if(position%8 == otherRook.position%8){
            if(position/8 < otherRook.position/8){
                for(int i = position+1;i/8<otherRook.position/8;i+=8){
                    if(!board.validPosition(i)) break;
                    if(board.get(i)!=null)return false;
                }
            } else {
                for(int i=otherRook.position-1;i/8>position/8;i-=8){
                    if(!board.validPosition(i)) break;
                    if(board.get(i)!=null)return false;
                }
            }
        }
        return false;
    }
    public String toString(){
        return this.color ? "R" : "r";
    }
}
