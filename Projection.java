import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

public class Projection {
    Set<ProjectionNode> nodes;
    Board board;
    int origin;
    boolean ignoreFriendly;
    public Projection(int origin, boolean ignoreFriendly, Board board){
        this.nodes = new HashSet<ProjectionNode>();
        this.board = board; this.origin = origin;
        this.ignoreFriendly = ignoreFriendly;
    }

    public void projectStraight(){
        if(!board.validPosition(origin)) throw new IllegalArgumentException();
        if (board.get(origin) == null) return;

        // Go left
        for(int i=origin-1;i/8==origin/8;i--){
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(origin, i, board.get(i)));
            if(board.get(i) != null) break;
        }
        // Go right
        for(int i=origin+1;i/8==origin/8;i++){
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(origin, i, board.get(i)));
            if(board.get(i) != null) break;
        }
        // Go up
        for(int i=origin+8;i<=63;i+=8){
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(origin, i, board.get(i)));
            if(board.get(i) != null) break;
        }
        // Go down
        for(int i=origin-8;i>=0;i-=8){
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(origin, i, board.get(i)));
            if(board.get(i) != null) break;
        }
    }
    public void projectDiagonal(){
        if(!board.validPosition(origin)) throw new IllegalArgumentException();
        if (board.get(origin) == null) return;

        // Down + left
        for(int i=origin-9;i>=0;i-=9){
            if(i%8 == 7 || (i%8)+1!=((i+9)%8)) break;
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(origin, i, board.get(i)));
            if(board.get(i) != null) break;
        }

        // Down + right
        for(int i=origin-7;i>=0;i-=7){
            if(i%8 == 0 || (i%8)-1!=((i+7) %8)) break;
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(origin, i, board.get(i)));
            if(board.get(i) != null) break;
        }

        // Up + left
        for(int i=origin+7;i<=63;i+=7){
            if(i%8==7 || (i%8)+1!=(i-7)%8) break;
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(origin, i, board.get(i)));
            if(board.get(i) != null) break;
        }

        // Up + right
        for(int i=origin+9;i<=63;i+=9){
            if(i%8==0 || (i%8)-1!=(i-9)%8) break;
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(origin, i, board.get(i)));
            if(board.get(i) != null) break;
        }
    }

    // Returns set of moves from projection. Note: will not work for promotion!
    public Set<Move> moves(){
        Set<Move> moveSet = new HashSet<Move>();
        for(ProjectionNode node : nodes){
            moveSet.add(board.createMove(node.from, node.to));
        }
        return moveSet;
    }
    public void clear(){
        this.nodes = new HashSet<ProjectionNode>();
    }

    public Iterator<ProjectionNode> iterator(){
        return nodes.iterator();
    }
}