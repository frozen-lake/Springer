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
    public Set<Move> legalMoves;

    public Piece(boolean color, int position, Board board, String type){
        this.legalMoves = new HashSet<Move>();
        this.color = color;
        this.position = position;
        this.hasMoved = false;
        this.board = board;
        this.type = type;
        this.originalPosition = position;

        board.addPiece(this);
    }
    public Piece(Piece piece, Board board){
        this.legalMoves = new HashSet<Move>();
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
    public abstract void updateMoves();
    public Set<Move> getMoves(){
        return legalMoves;
    }

    public void captured(){
//        board.pieces.removeIf(this::equals);
//        board.whiteArmy.removeIf(this::equals);
//        board.blackArmy.removeIf(this::equals);
    }

    public int pawnProjections(Set<Piece> friendlyProjectors){
        int n = 0;
        for(Piece p: friendlyProjectors){
            if(p.type.equals("Pawn")) n++;
        }
        return n;
    }
    public boolean protectedByPawn(){
        // White
        if(color) return position/8>0 && (
                (board.validPosition(position-7)&&board.get(position-7)!=null&&board.get(position-7).type.equals("Pawn")&&board.get(position-7).color)
                || (board.validPosition(position-9)&&board.get(position-9)!=null&&board.get(position-9).type.equals("Pawn")&&board.get(position-9).color)
        );
        // Black
        return position/8<7 && (
                (board.validPosition(position+7)&&board.get(position+7)!=null&&board.get(position+7).type.equals("Pawn")&&!board.get(position+7).color)
                        || (board.validPosition(position+9)&&board.get(position+9)!=null&&board.get(position+9).type.equals("Pawn")&&!board.get(position+9).color)
        );
    }

    public boolean defended(){
        return terminalProjections(true, position) - terminalProjections(false, position) >= 0;
    }
    public int defense(){
        return terminalProjections(true, position);
    }
    public boolean attackedByPawn() {
        // White piece attacked by black pawn
        if(color) return position/8<7 && (
                (board.validPosition(position+7)&&board.get(position+7)!=null&&board.get(position+7).type.equals("Pawn")&&!board.get(position+7).color)
                        || (board.validPosition(position+9)&&board.get(position+9)!=null&&board.get(position+9).type.equals("Pawn")&&!board.get(position+9).color)
        );
        // Black piece attacked by white pawn
        return position/8>0 && (
                (board.validPosition(position-7)&&board.get(position-7)!=null&&board.get(position-7).type.equals("Pawn")&&board.get(position-7).color)
                        || (board.validPosition(position-9)&&board.get(position-9)!=null&&board.get(position-9).type.equals("Pawn")&&board.get(position-9).color)
        );
    }
    public int terminalProjections(boolean friendly, int pos){
        return terminalProjectors(friendly, pos).size();
    }

    // Returns the set of friendly pieces which this piece protects.
    public Set<Piece> protects(){

        return null; //Projection
    }

    // Returns the number of defenders or attackers on this piece if it were at the given position.
    public Set<Piece> terminalProjectors(boolean friendly, int pos){
        Piece old = board.get(pos);
        int oldPos = position;
        board.set(pos, this);

        Set<Piece> projectors = new HashSet<Piece>();
        // Check relevant pawn diagonals, bishops and queens on diagonals, rooks and queen on horizontals,
        // enemy king on all surrounding squares, and all 8 knight squares.

//        Set<Move> straightProjections = board.straightProjection(pos, false);
//        Set<Move> diagonalProjections = board.diagonalProjection(pos, false);
        Projection p = new Projection(position, color, board);
        p.projectDiagonal(!friendly);
        Iterator<ProjectionNode> iter = p.iterator();
        while(iter.hasNext()){
            ProjectionNode node = iter.next();
            if(node.dest != null && node.dest.color == (friendly == color) && (node.dest.type.equals("Bishop")||node.dest.type.equals("Queen"))){
                projectors.add(node.dest);
            }
        }
        //
        p.clear(); p.projectStraight(!friendly);
        iter = p.iterator();
        while(iter.hasNext()){
            ProjectionNode node = iter.next();
            if(node.dest != null && node.dest.color == (friendly == color) && (node.dest.type.equals("Rook")||node.dest.type.equals("Queen"))){
                projectors.add(node.dest);
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
        return type.equals(p.type) && color == p.color && position == p.position && board == p.board;
    }

    public int hashCode(){
        return (color?344:-344) + (position*31) + (type.hashCode()) + 67;
    }

    public String toStringDebug(){
        return "{type: " + this.type + ", color: " + color + ", position: " + position + ", origin: " + originalPosition + " }";
    }



}
