package ru.ifmo.genetics.tools.olc.overlaps;

import org.junit.Test;

import static org.junit.Assert.*;

public class OverlapsListTest {
    @Test
    public void testFind() throws Exception {
        OverlapsList list = new OverlapsList(false);
        list.add(10, 20);
        list.add(3, -1);
        list.add(11, 17);
        assertEquals(0, list.find(10, 20));
        assertEquals(1, list.find(3, -1));
        assertEquals(2, list.find(11, 17));
        list.sort(new OverlapsList.OverlapsSortTraits());
        assertEquals(2, list.find(10, 20));
        assertEquals(0, list.find(3, -1));
        assertEquals(1, list.find(11, 17));
    }
}
