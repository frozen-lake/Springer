import java.util.Set;
import java.util.HashSet;

public class King extends Piece {
    public String castled;
    public King(boolean color, int position, Board board){
        super(color, position, board, "King");
        castled = null;
    }
    public King(King p, Board board){
        super(p, board);
        castled = null;
    }
    public void updateMoves() {
        Projection p = new Projection(position, color, board);
        p.projectKing(true);
        legalMoves = p.moves();

        if(canCastleShort()) legalMoves.add(board.createMove(position, position + 2));
        if(canCastleLong()) legalMoves.add(board.createMove(position, position - 2));

        board.filterLegalMoves(legalMoves);

        defenders = p.getDefenders();
        attackers = p.getAttackers();

        p.clear();
        attacks = new HashSet<Piece>();
        for(Move move: legalMoves){
            if(move.capture() != null) attacks.add(move.capture());
        }
        p.projectKing(false);
        defends = p.getDefends();
        p.clear();
    }

    public boolean inCheckmate(){
        return inCheck() && (color?board.getWhiteMoves().size()==0:board.getBlackMoves().size()==0);
    }
    public boolean inCheck(){
        // Check hostile pawn diagonals, bishops and queens on diagonals, rooks and queen on horizontals,
        // enemy king on all surrounding squares, and all 8 knight squares.
        return terminalProjections(false, position) != 0;
    }


    // Returns true if the King is legally able to castle short.
    public boolean canCastleShort(){
        // Legal piece arrangement for short castling, clear line and unmoved king and rook
        if(castled!=null || hasMoved || inCheck()) return false; //  || position != (color?4:60)
        if(board.get(position+1) != null || board.get(position+2) != null || board.get(position+3) == null) return false;
        if(!board.get(position+3).type.equals("Rook") || board.get(position+3).hasMoved) return false;
        // Can't castle through check
        boolean checked;
        Move m1 = new Move(this, position, position+1, null, null, null);
        Move m2 = new  Move(this, position+1, position+2, null, null, null);
        board.primitiveMove(m1, false);
        checked = inCheck();
        board.primitiveMove(m2, false);
        checked = checked || inCheck();

        // Return the king
        board.undoPrimitiveMove(m2, false);
        board.undoPrimitiveMove(m1, false);

        return !checked;
    }

    // Received command from Board.makeMove() to castle short, now simply move the pieces and update properties.
    protected void castleShort(){
        if(!canCastleShort()) throw new IllegalStateException();

        board.set(position + 2, this); // Move king
        board.set(position, null);
        setPosition(position + 2);

        board.set(position - 1, board.get(position + 1)); // Move rook
        board.set(position+1, null);
        board.get(position - 1).setPosition(position - 1);

        castled = "K";
    }

    public boolean canCastleLong(){
        // Legal piece arrangement for long castling, clear line and unmoved king and rook
        if(castled!=null|| hasMoved || inCheck()) return false;
        if(board.get(position-1) != null || board.get(position-2) != null || board.get(position-3) != null || board.get(position - 4) == null) return false;
        if(!board.get(position-4).type.equals("Rook") || board.get(position-4).hasMoved) return false;
        // Can't castle through check
        boolean checked;
        Move m1 = new Move(this, position, position-1, null, null, null);
        Move m2 = new Move(this, position-1, position-2, null, null, null);
        Move m3 = new Move(this, position-2, position-3, null, null, null);
        board.primitiveMove(m1, false);
        checked = inCheck();
        board.primitiveMove(m2, false);
        checked = checked || inCheck();
        board.primitiveMove(m3, false);
        checked = checked || inCheck();
        // Return the king
        board.undoPrimitiveMove(m3, false);
        board.undoPrimitiveMove(m2, false);
        board.undoPrimitiveMove(m1, false);

        return !checked;
    }
    protected void castleLong(){
        if(!canCastleLong()) throw new IllegalStateException();

        board.set(position - 2, this); // Move king
        board.set(position, null);
        setPosition(position - 2);

        board.set(position + 1, board.get(position - 2)); // Move rook
        board.set(position-2, null);
        board.get(position + 1).setPosition(position + 1);
        castled = "Q";
    }

    public String toString(){
        return this.color ? "K" : "k";
    }
}
