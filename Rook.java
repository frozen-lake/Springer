import java.util.HashSet;
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
        legalMoves = p.moves();
        board.filterLegalMoves(legalMoves);
        
        defenders = p.getDefenders();
        attackers = p.getAttackers();

        p.clear();
        attacks = new HashSet<Piece>();
        for(Move move: legalMoves){
            if(move.capture() != null) attacks.add(move.capture());
        }
        p.projectStraight(false);
        defends = p.getDefends();
        p.clear();
    }

    // Returns true if there is a horizontal or vertical line from this rook to the other with no king between.

    public boolean orthogonalNoKing(){
        if(otherRook == null || otherRook.captured) return false;
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
        if(otherRook == null || otherRook.captured) return false;
        return position/8 != otherRook.position/8 && position%8 != otherRook.position%8;

    }

    // Returns true if there is no pawn of the same color as the rook, on the file the rook is on.
    public boolean onSemiOpenFile(){
        for(int i=position%8;i<=63;i+=8){
            if(!board.validPosition(i)) throw new IllegalStateException();
            if(board.get(i) != null && board.get(i).type.equals("Pawn") && board.get(i).color==color) return false;
        }
        return true;
    }
    public boolean onOpenFile(){
        for(int i=position%8;i>=0&&i<=63;i+=8){
            if(!board.validPosition(i)) throw new IllegalStateException();
            if(board.get(i) != null && board.get(i).type.equals("Pawn")) return false;
        }
        return true;
    }
    public boolean connected(){
        if(otherRook == null) return false;
        if(position/8 == otherRook.position/8){
            if(position%8 < otherRook.position%8){
                for(int i=position+1;i%8<otherRook.position%8;i++){
                    if(board.get(i)!=null && board.get(i) != otherRook)return false;
                    if(!board.validPosition(i)) break;
                }
            } else {
                for(int i=otherRook.position+1;i%8<position%8;i++){
                    if(!board.validPosition(i)) break;
                    if(board.get(i)!=null && board.get(i) != otherRook)return false;
                }
            }
        } else if(position%8 == otherRook.position%8){
            if(position/8 < otherRook.position/8){
                for(int i = position+1;i/8<otherRook.position/8;i+=8){
                    if(!board.validPosition(i)) break;
                    if(board.get(i)!=null && board.get(i) != otherRook)return false;
                }
            } else {
                for(int i=otherRook.position+1;i/8<position/8;i+=8){
                    if(!board.validPosition(i)) break;
                    if(board.get(i)!=null && board.get(i) != otherRook)return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }
    public String toString(){
        return this.color ? "R" : "r";
    }
}
