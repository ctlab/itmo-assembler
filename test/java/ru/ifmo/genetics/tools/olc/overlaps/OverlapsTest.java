package ru.ifmo.genetics.tools.olc.overlaps;

import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.LightDna;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OverlapsTest {
    List<Dna> plainReads = Arrays.asList(new Dna("GTTCATATG"), new Dna("ACTGAA"));
    ArrayList<LightDna> reads = new ArrayList<LightDna>();
    {
        for (Dna dna: plainReads) {
            reads.add(dna);
            reads.add(dna.reverseComplement());
        }
    }
    

    @Test
    public void testIsWellOriented() throws Exception {
        Overlaps overlaps = new Overlaps<LightDna>(reads, false);

        /**
         *  GTTCATATG
         * ACTGAA
         */
        assertTrue(Overlaps.isWellOriented(2, 0, 4));
        assertTrue(!Overlaps.isWellOriented(0, 2, -4));

        /**
         * GTTCATATG
         * ACTGAA
         */
        assertTrue(Overlaps.isWellOriented(2, 0, 3));
        assertTrue(!Overlaps.isWellOriented(0, 2, -3));

        /**
         * GTTCATATG
         *  ACTGAA
         */
        assertTrue(Overlaps.isWellOriented(2, 0, 1));
        assertTrue(!Overlaps.isWellOriented(0, 2, -1));

        /**
         * GTTCATATG
         *   ACTGAA
         */
        assertTrue(Overlaps.isWellOriented(0, 2, 1));
        assertTrue(!Overlaps.isWellOriented(2, 0, -1));

        /**
         * GTTCATATG
         *    ACTGAA
         */
        assertTrue(Overlaps.isWellOriented(0, 2, 3));
        assertTrue(!Overlaps.isWellOriented(2, 0, -3));

        /**
         * GTTCATATG
         *     ACTGAA
         */
        assertTrue(Overlaps.isWellOriented(0, 2, 5));
        assertTrue(!Overlaps.isWellOriented(2, 0, -5));
    }

    @Test
    public void testAddOverlap() throws Exception {
        Overlaps overlaps = new Overlaps<LightDna>(reads, false);
        
        assertEquals(2, overlaps.addOverlap(2, 0, 1, 0));
        assertEquals(0, overlaps.addOverlap(0, 2, -1, 0));
        assertEquals(1, overlaps.addOverlap(0, 1, 0, 0));
    }

    @Test
    public void testGetOverlaps() throws Exception {
        Overlaps overlaps = new Overlaps<LightDna>(reads, false);

        overlaps.addOverlap(2, 0, 1, 0);

        OverlapsList expectedForwardOverlaps = new OverlapsList(false);
        expectedForwardOverlaps.add(0, 1);

        OverlapsList expectedBackwardOverlaps = new OverlapsList(false);
        expectedBackwardOverlaps.add(2, -1);

        assertEquals(expectedForwardOverlaps, overlaps.getForwardOverlaps(2));
        assertEquals(expectedBackwardOverlaps, overlaps.getBackwardOverlaps(0));
    }
}
