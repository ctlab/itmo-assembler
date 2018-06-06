package ru.ifmo.genetics.structures.debriujn;

import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaView;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.utils.TestUtils;

import java.io.*;
import java.util.Random;

import static org.junit.Assert.*;

public class CompactDeBruijnGraphTest {
    Random r;
    {
        long seed = this.hashCode() + System.nanoTime();
        r = new Random(seed);
        System.err.println("Seed = " + seed);
    }

    @Test
    public void testAddEdges() throws IOException {
        int k = 15;
        CompactDeBruijnGraph g = new CompactDeBruijnGraph(k, 4096);
        LightDna testDna = new Dna("TATGTTCAGATAATGCCCGATGACTTTGTCATGCAGCTCCAC");
        LightDna testDnaRc = DnaView.rcView(testDna);
        LightDna testDna2 = new Dna("GATGCTGAAAAGAGTAGTAATTGCTGGTAATGACTCCAACTTA");
        g.addEdges(testDna);
        assertTrue(g.containsEdges(testDna));
        assertEquals(testDna.length() - k, g.edgesSize());

        g.addEdges(testDnaRc);
        assertEquals(testDna.length() - k, g.edgesSize());

        assertTrue(g.containsEdges(DnaView.rcView(testDna)));
        assertFalse(g.containsEdges(testDna2));
    }

    @Test
    public void testOkWritable() throws IOException {
        int k = 15;
        CompactDeBruijnGraph g = new CompactDeBruijnGraph(k, 4096);
        LightDna testDna = new Dna("TATGTTCAGATAATGCCCGATGACTTTGTCATGCAGCTCCAC");
        LightDna testDna2 = new Dna("GATGCTGAAAAGAGTAGTAATTGCTGGTAATGACTCCAACTTA");
        g.addEdges(testDna);

        CompactDeBruijnGraph g2 = new CompactDeBruijnGraph();
        TestUtils.copyWritable(g, g2);

        assertTrue(g2.containsEdges(testDna));
        assertFalse(g2.containsEdges(testDna2));
    }
}

