import java.util.*;

public abstract class Piece {

    public final boolean color;
    public final String type;
    protected int position;
    public int value;
    protected final int originalPosition;
    protected boolean hasMoved;
    public final Board board;
    public Set<Move> legalMoves;
    public Set<Piece> defenders;
    public Set<Piece> attackers;
    public Set<Piece> attacks;
    public Set<Piece> defends;
    public boolean pawnProtected;
    public int eval;
    public boolean captured;

    public Piece(boolean color, int position, Board board, String type){
        this.captured = false;
        this.legalMoves = new HashSet<Move>();
        this.color = color;
        this.position = position;
        this.hasMoved = false;
        this.board = board;
        this.type = type;
        this.originalPosition = position;
        this.attackers = new HashSet<Piece>();
        this.defenders = new HashSet<Piece>();
        this.attacks = new HashSet<Piece>();
        this.defends = new HashSet<Piece>();
        this.eval = 0;

        switch (type) {
            case "Pawn" -> value = 100;
            case "Bishop", "Knight" -> value = 300;
            case "Queen" -> value = 900;
            case "Rook" -> value = 500;
            case "King" -> value = 9999;
        }

        board.addPiece(this);
    }
    public Piece(Piece piece, Board board){
        this.captured = false;
        this.legalMoves = new HashSet<Move>();
        color=piece.color;
        position=piece.position;
        hasMoved=piece.hasMoved;
        type=piece.type;
        this.board = board;
        originalPosition = piece.originalPosition;
        this.attackers = new HashSet<Piece>();
        this.defenders = new HashSet<Piece>();
        this.attacks = new HashSet<Piece>();
        this.defends = new HashSet<Piece>();
        this.eval = 0;

        switch (type) {
            case "Pawn" -> value = 100;
            case "Bishop", "Knight" -> value = 300;
            case "Queen" -> value = 900;
            case "Rook" -> value = 500;
            case "King" -> value = 9999;
        }

        board.addPiece(this);
    }


    public void captured(){
        this.captured = true;
    }

    public boolean pawnProtected(){
        return pawnProtected;
    }

    public void setPosition(int p){ this.position = p; }

    public int getPosition(){ return position; }
    public abstract void updateMoves();
    public Set<Move> getMoves(){
        return legalMoves;
    }



    public int pawnProjections(Set<Piece> friendlyProjectors){
        int n = 0;
        for(Piece p: friendlyProjectors){
            if(p.type.equals("Pawn")) n++;
        }
        return n;
    }
    public boolean protectedByPawn(){
        for(Piece d: defenders){
            if(d.type.equals("Pawn")) return true;
        }
        return false;
    }

    public boolean defended(){
        return defenders.size() - attackers.size() >= 0;
    }
    public boolean attackedByPawn() {
        // White piece attacked by black pawn
//        if(color) return position/8<7 && (
//                (board.validPosition(position+7)&&board.get(position+7)!=null&&board.get(position+7).type.equals("Pawn")&&!board.get(position+7).color)
//                        || (board.validPosition(position+9)&&board.get(position+9)!=null&&board.get(position+9).type.equals("Pawn")&&!board.get(position+9).color)
//        );
//        // Black piece attacked by white pawn
//        return position/8>0 && (
//                (board.validPosition(position-7)&&board.get(position-7)!=null&&board.get(position-7).type.equals("Pawn")&&board.get(position-7).color)
//                        || (board.validPosition(position-9)&&board.get(position-9)!=null&&board.get(position-9).type.equals("Pawn")&&board.get(position-9).color)
//        );
        for(Piece a: attackers){
            if(a.type.equals("Pawn")) return true;
        }
        return false;
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
    public int compareTo(Piece other){
        if(type.equals(other.type)) return 0;
        if(type.equals("Pawn")) return -1;
        if(type.equals("King")) return 1;
        if(type.equals("Queen") && !other.type.equals("King")) return 1;
        if(type.equals("Rook") && !other.type.equals("Queen") && !other.type.equals("King")) return 1;
        if(type.equals("Knight") && !other.type.equals("Rook") && !other.type.equals("Queen") && !other.type.equals("King")) return 1;

        return -1;
    }
    public int hashCode(){
        return (color?344:-344) + (position*31) + (type.hashCode()) + 67;
    }

    public String toStringDebug(){
        return "{type: " + this.type + ", color: " + color + ", position: " + position + ", origin: " + originalPosition + ", defenders: " + defenders + ", attackers: " + attackers + ", eval: " + eval + "}";
    }

    public void printDebug(){
        System.out.println("===============");
        System.out.println("| type: " + this.type + "color: " + color + ", toString: " + toString());
        System.out.println("| position: " + this.position + ", originalPosition: " + this.originalPosition);
        System.out.println("| eval: " + this.eval);
        System.out.println("| defenders: " + this.defenders);
        System.out.println("| attackers: " + this.attackers);
        System.out.println("| defends: " + this.defends);
        System.out.println("| attacks: " + this.attacks);
    }



}
