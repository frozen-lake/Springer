public class ProjectionNode {
    Piece piece;
    int from;
    int to;
    Piece dest;

    public ProjectionNode(Piece piece, int from, int to, Piece dest){
        this.piece = piece;
        this.from = from;
        this.to = to;
        this.dest = dest;
    }

}
