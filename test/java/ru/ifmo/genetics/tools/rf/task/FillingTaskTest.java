package ru.ifmo.genetics.tools.rf.task;

import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.structures.debriujn.CompactDeBruijnGraph;

import java.util.Random;

import static org.junit.Assert.*;

public class FillingTaskTest {
    @Test
    public void testFillRead() throws Exception {
        final int k = 7;
        CompactDeBruijnGraph g = new CompactDeBruijnGraph(k, 100000);

        String seq = "CGTCGCCGCTCTGAACCCGGAAAAGTTGCACAGCCAAAATTATGGCATAA";

        Dna dna1 = new Dna(seq);
        
        g.addEdges(dna1);
        
        DnaQ read1 = new DnaQ("A" + seq.substring(2, 13) + "GC", 30);
        DnaQ read2 = new DnaQ("GA" + seq.substring(29, 48) + "GG", 30);
        read2.inplaceReverseComplement();
        
        GlobalContext context1 = new GlobalContext(
                null,
                null,
                k,
                2,
                100,
                g);
        
        FillingTask ft1 = new FillingTask(context1, null);
        
        FillingTask.FillingResult fr1 = ft1.fillRead(read1, read2, true);
        ft1.printStat();

        assertNotNull(fr1);
        assertEquals(FillingTask.ResultType.OK, fr1.type);
        assertEquals(1, fr1.leftSkip);
        assertEquals(2, fr1.rightSkip);
        assertTrue(DnaTools.equals(new Dna(seq.substring(2, 48)), fr1.dnaq));

        read1.inplaceReverseComplement();
        read2.inplaceReverseComplement();

        FillingTask.FillingResult fr2 = ft1.fillRead(read1, read2, false);
        ft1.printStat();

        assertNotNull(fr2);
        assertEquals(1, fr2.leftSkip);
        assertEquals(2, fr2.rightSkip);
        assertTrue(DnaTools.equals(new Dna(seq.substring(2, 48)), fr1.dnaq));

        String seq2 = "CGTCGCCGCTCTGAACCCGGAAAAAGTTGCACAGCCAAAATTATGGCATAA";
        g.addEdges(new Dna(seq2));

        FillingTask.FillingResult fr3 = ft1.fillRead(read1, read2, false);
        ft1.printStat();

        assertEquals(FillingTask.ResultType.AMBIGUOUS, fr3.type);
    }

    @Test
    public void testFillReadWithLargeK() {
        final int k = 97;
        CompactDeBruijnGraph g = new CompactDeBruijnGraph(k, 100000);

        Random r = new Random(42);
        int l = 1000;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l; ++i) {
            sb.append(DnaTools.NUCLEOTIDES[r.nextInt(4)]);

        }
        String seq = sb.toString();

        Dna dna1 = new Dna(seq);

        g.addEdges(dna1);

        DnaQ read1 = new DnaQ(seq.substring(0, 3 * k), 30);
        DnaQ read2 = new DnaQ(seq.substring(l - 3 * k, l), 30);
        read2.inplaceReverseComplement();

        GlobalContext context1 = new GlobalContext(
                null,
                null,
                k,
                2,
                2000,
                g);

        FillingTask ft1 = new FillingTask(context1, null);

        FillingTask.FillingResult fr1 = ft1.fillRead(read1, read2, true);
        ft1.printStat();

        assertNotNull(fr1);
        assertEquals(FillingTask.ResultType.OK, fr1.type);
        assertEquals(seq, DnaTools.toString(fr1.dnaq));
    }

}
