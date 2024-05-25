import org.junit.Test;

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
}
