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
            if(board.validPosition(j) && (board.get(j)==null || (board.get(j).color != color))){
                moves.add(board.createMove(position, j));
            }
        }
        if(canCastleShort()) moves.add(board.createMove(position, position + 2));
        if(canCastleLong()) moves.add(board.createMove(position, position - 2));

        board.filterLegalMoves(moves);
        return moves;
    }

    public boolean inCheckmate(){
        return inCheck() && getMoves().size() == 0;
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
            if(board.validPosition(j) && board.get(j) != null && board.get(j).type.equals("King") && board.get(j).color != color){
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


    // Returns true if the King is legally able to castle short.
    public boolean canCastleShort(){
        // Legal piece arrangement for short castling, clear line and unmoved king and rook
        if(hasMoved) return false;
        if(board.get(position+1) != null || board.get(position+2) != null || board.get(position+3) == null) return false;
        if(!board.get(position+3).type.equals("Rook") || board.get(position+3).hasMoved) return false;
        // Can't castle through check
        boolean checked;
        board.primitiveMove(new Move(this, position, position+1, null, null, null));
        checked = inCheck();
        board.primitiveMove(new Move(this, position, position+1, null, null, null));
        checked = checked || inCheck();

        // Return the king
        board.primitiveMove(new Move(this, position, position-2, null, null, null));

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

        hasMoved = true; board.get(position - 1).hasMoved = true;
    }

    public boolean canCastleLong(){
        // Legal piece arrangement for long castling, clear line and unmoved king and rook
        if(hasMoved) return false;
        if(board.get(position-1) != null || board.get(position-2) != null || board.get(position-3) != null || board.get(position - 4) == null) return false;
        if(!board.get(position-4).type.equals("Rook") || board.get(position-4).hasMoved) return false;
        // Can't castle through check
        boolean checked;
        board.primitiveMove(new Move(this, position, position-1, null, null, null));
        checked = inCheck();
        board.primitiveMove(new Move(this, position, position-1, null, null, null));
        checked = checked || inCheck();
        board.primitiveMove(new Move(this, position, position-1, null, null, null));
        checked = checked || inCheck();
        // Return the king
        board.primitiveMove(new Move(this, position, position+3, null, null, null));

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

        hasMoved = true; board.get(position + 1).hasMoved = true;
    }
    public String toString(){
        return this.color ? "K" : "k";
    }
}
