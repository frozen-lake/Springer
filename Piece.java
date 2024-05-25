import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class Piece {

    public final boolean color;
    public final String type;
    protected int position;
    protected final int originalPosition;
    protected boolean hasMoved;
    public final Board board;

    public Piece(boolean color, int position, Board board, String type){
        this.color = color;
        this.position = position;
        this.hasMoved = false;
        this.board = board;
        this.type = type;
        this.originalPosition = position;

        board.addPiece(this);
    }
    public Piece(Piece piece, Board board){
        color=piece.color;
        position=piece.position;
        hasMoved=piece.hasMoved;
        type=piece.type;
        this.board = board;
        originalPosition = piece.originalPosition;
        board.addPiece(this);
    }

    public void setPosition(int p){ this.position = p; }

    public int getPosition(){ return position; }
    public abstract Set<Move> getMoves();

    public void captured(){
        board.pieces.removeIf(this::equals);
        board.whiteArmy.removeIf(this::equals);
        board.blackArmy.removeIf(this::equals);
    }

    public int terminalProjections(boolean friendly, int pos){
        return terminalProjectors(friendly, pos).size();
    }

    // Returns the number of defenders or attackers on this piece if it were at the given position.
    public Set<Piece> terminalProjectors(boolean friendly, int pos){
        Set<Piece> projectors = new HashSet<Piece>();
        // Check relevant pawn diagonals, bishops and queens on diagonals, rooks and queen on horizontals,
        // enemy king on all surrounding squares, and all 8 knight squares.

        Set<Move> straightProjections = board.straightProjection(pos, false);
        Set<Move> diagonalProjections = board.diagonalProjection(pos, false);

        // Rooks, queens, bishops
        for(Move proj: straightProjections){
            if(board.get(proj.to()) != null && board.get(proj.to()).color == (friendly == color)){
                if(board.get(proj.to()).type.equals("Rook")||board.get(proj.to()).type.equals("Queen")){
                    projectors.add(board.get(proj.to()));
                }
            }
        }

        for(Move proj: diagonalProjections){
            if(board.get(proj.to()) != null && board.get(proj.to()).color == (friendly == color)){
                if(board.get(proj.to()).type.equals("Bishop")||board.get(proj.to()).type.equals("Queen")){
                    projectors.add(board.get(proj.to()));
                }
            }
        }

        // King
        int[] around = {pos - 1, pos + 1, pos - 7, pos + 7,
                pos - 8, pos + 8, pos - 9, pos + 9};
        for(int j : around){
            if(board.validPosition(j) && board.get(j) != null && board.get(j).type.equals("King") && board.get(j).color == (friendly == color)){
                projectors.add(board.get(j));
            }
        }

        // Knights
        around = new int[]{pos - 6, pos + 6, pos - 10, pos + 10,
                pos - 15, pos + 15, pos - 17, pos + 17};
        for(int j : around){
            if(board.validPosition(j) && board.get(j) != null && board.get(j).type.equals("Knight") && board.get(j).color == (friendly == color)){
                projectors.add(board.get(j));
            }
        }

        // Pawns
        int forward = color ? pos + 8 : pos - 8;
        if(board.validPosition(forward-1) && board.get(forward-1) != null && board.get(forward-1).type.equals("Pawn") && board.get(forward-1).color == (friendly == color)){
            projectors.add(board.get(forward-1));
        }
        if(board.validPosition(forward+1) && board.get(forward+1) != null && board.get(forward+1).type.equals("Pawn") && board.get(forward+1).color == (friendly == color)){
            projectors.add(board.get(forward+1));
        }

        projectors.remove(this);

        return projectors;
    }

    public boolean equals(Object o){
        if(!(o instanceof Piece)) return false;
        Piece p = (Piece) o;
        return type.equals(p.type) && color == p.color && position == p.position;
    }

    public int hashCode(){
        return (color?344:-344) + (position*31) + (type.hashCode()) + 67;
    }
}
