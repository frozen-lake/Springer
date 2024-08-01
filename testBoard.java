import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;
public class testBoard {

    @Test
    public void testValidPosition1(){
        Chess c = new Chess();
        Board b = c.board;
        assertTrue(b.validPosition("a1"));
    }

    @Test
    public void testPositionToInt(){
        Chess c = new Chess();
        Board b = c.board;
        assertEquals(1, b.positionToInt("b1"));
        assertEquals(8, b.positionToInt("a2"));
        assertEquals(63, b.positionToInt("h8"));
    }

    @Test
    public void testPopulateBoard(){
        Chess c = new Chess();
        Board b = c.board;
        assertEquals("P", b.get(13).toString());
        assertEquals("p", b.get(52).toString());
    }

    @Test
    public void testPrintBoard(){
        Chess c = new Chess();
        Board b = c.board;
        b.printBoardW();
        b.printBoardB();
    }

    @Test
    public void testBoardCopyConstructor(){
        Chess c = new Chess();
        Board b = c.board;
        // WIP
    }

    @Test
    public void testNullMove(){
        InputStream sysin = System.in;
        ByteArrayInputStream in = new ByteArrayInputStream("e2 e4\ne7 e5\nf1 c4\npass\n".getBytes());
        System.setIn(in);

        Chess c = new Chess();
        Board b = c.board;
        try{
            c.startGameW();

        } catch(NoSuchElementException ignored){
            assertEquals(true, b.sideToMove);
            assertNull(b.winner);
            b.printBoardW();
        }
        System.setIn(System.in);
    }
}
