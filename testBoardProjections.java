import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;

public class testBoardProjections {
    @Test
    public void testStraightProjection1(){
        Chess c = new Chess();
        Board b = c.board;
        int[] arr = {17, 25, 33, 41, 49};
        ArrayList<Integer> l = new ArrayList<Integer>();
        for(Move move: b.straightProjection(9)){
            l.add(move.to());
        }
        l.sort(Comparator.naturalOrder());
        assertEquals(l.size(), arr.length);
        for(int i=0;i<l.size();i++){
            assertEquals(arr[i], (int) l.get(i));
        }
    }
    @Test

    public void testStraightProjection2(){
        Chess c = new Chess();
        Board b = c.board;
        b.set(34, new Rook(true, 34, b));
        int[] arr = {18, 26, 32, 33, 35, 36, 37, 38, 39, 42, 50};
        ArrayList<Integer> l = new ArrayList<Integer>();
        for(Move move: b.straightProjection(34)){
            l.add(move.to());
        }
        l.sort(Comparator.naturalOrder());
        assertEquals(l.size(), arr.length);
        for(int i=0;i<l.size();i++){
            assertEquals(arr[i], (int) l.get(i));
        }
    }

    @Test
    public void testDiagonalProjection1(){
        Chess c = new Chess();
        Board b = c.board;
        b.set(34, new Bishop(true, 34,b));

        int[] arr = {16, 20, 25, 27, 41, 43, 48, 52};
        ArrayList<Integer> l = new ArrayList<Integer>();
        for(Move move: b.diagonalProjection(34)){
            l.add(move.to());
        }
        l.sort(Comparator.naturalOrder());
        System.out.println(l);
        assertEquals(l.size(), arr.length);
        for(int i=0;i<l.size();i++){
            assertEquals(arr[i], (int) l.get(i));
        }
    }
}
