package ru.ifmo.genetics.tools.irf;

import org.junit.Test;
import ru.ifmo.genetics.dna.*;
import ru.ifmo.genetics.structures.debriujn.WeightedDeBruijnGraph;
import ru.ifmo.genetics.tools.rf.Orientation;

import java.io.File;

import static org.junit.Assert.*;

public class FillingTaskTest {
    @Test
    public void testFill() throws Exception {
        final int k = 7;
        WeightedDeBruijnGraph g = new WeightedDeBruijnGraph(k, 1000);

        String seq = "CGTCGCCGCTCTGAACCCGGAAAAGTTGCACAGCCAAAATTATGGCATAA";


        final Dna dna1 = new Dna(seq);
        final int l1 = dna1.length();
        
        g.addEdgesWithWeight(dna1, 20);
        final Dna dna2 = new Dna(
                seq.substring(0, 9) + seq.substring(10, 20) +
                seq.substring(21, 30) + seq.substring(31, l1));
        g.addEdgesWithWeight(dna2, 3);
        int l2 = dna2.length();

        g.addEdgesWithWeight(new Dna(
                seq.substring(0, 8) + "TCGTTGA"), 3);

        g.addEdgesWithWeight(new Dna(
                seq.substring(0, 10) + "GCCCCCGTTCTGATTTCGCA" + seq.substring(31, l1)), 3);

        g.addEdgesWithWeight(new Dna(
                seq.substring(0, 10) + "GCCCCGTTCTGATTTCGCA" + seq.substring(31, l1)), 3);

        g.addEdgesWithWeight(new Dna(
                seq.substring(0, 10) + "GCCCCCGTTCTGATTCGCA" + seq.substring(31, l1)), 3);

        g.addEdgesWithWeight(new Dna(
                seq.substring(0, 10) + "GCCCCGTTCTGATTCGCA" + seq.substring(31, l1)), 1);

        final int readLength = 15;
        final LightDnaQ read1 = new DnaQViewFromDna(dna2.substring(0, readLength), (byte) 30);
        final LightDnaQ read2 = DnaQView.rcView(new DnaQViewFromDna(dna2.substring(l2 - readLength, l2), (byte) 30));
//        DnaQ read1 = new DnaQ("A" + seq.substring(2, 13) + "GC", 30);
//        DnaQ read2 = new DnaQ("GA" + seq.substring(29, 48) + "GG", 30);
//        read2.inplaceReverseComplement();

        GlobalContext context = new GlobalContext(
                k,
                g,
                2,
                100,
                1000,
                new File("/tmp"), null, null, 3);

        FillingTask ft = new FillingTask(context);
        FillingResult res = ft.fill(read1, read2, Orientation.FR);

        assertEquals(FillingResult.ResultType.AMBIGUOUS, res.type);
    }
}
