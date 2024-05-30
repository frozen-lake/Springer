import java.util.HashSet;
import java.util.Set;

public class SearchNode {
    public int score; // Search score
    public int eval; // Concrete evaluation
    public SearchNode next; // Next in series of best moves
    public Set<SearchNode> children; // All nodes explored at the next depth level
    public Move move;
    public SearchNode(int score){
        children = new HashSet<SearchNode>();
        this.score = score;
        this.next = null;
        this.eval = 0;
    }
    public SearchNode(Move move){
        children = new HashSet<SearchNode>();
        this.score = 0;
        this.next = null;
        this.move = move;
        this.eval = 0;
    }
    public SearchNode(int score, Move move){
        children = new HashSet<SearchNode>();
        this.score = score;
        this.next = null;
        this.move = move;
        this.eval = score; // Base case node; end of recursive depth
    }

    public void addChild(SearchNode child){
        children.add(child);
    }

    public String toString(){
        return "[" + score + "/"+eval+", " + move + "], "+"{"+next+"}";
    }
}
