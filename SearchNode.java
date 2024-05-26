import java.util.HashSet;
import java.util.Set;

public class SearchNode {
    public int score;
    public SearchNode next;
    public Set<SearchNode> children;
    public Move move;
    public SearchNode(int score){
        children = new HashSet<SearchNode>();
        this.score = score;
        this.next = null;
    }
    public SearchNode(Move move){
        children = new HashSet<SearchNode>();
        this.score = 0;
        this.next = null;
        this.move = move;
    }
    public SearchNode(int score, Move move){
        children = new HashSet<SearchNode>();
        this.score = score;
        this.next = null;
        this.move = move;
    }

    public void addChild(SearchNode child){
        children.add(child);
    }

    public String toString(){
        return "[" + score + ", " + move + "], "+"{"+next+"}";
    }
}
