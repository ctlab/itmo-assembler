package ru.ifmo.genetics.utils;

import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.DnaView;
import ru.ifmo.genetics.dna.LightDna;

import static org.junit.Assert.*;

public class KmerUtilsTest {
    @Test
    public void testKmerReverseComplement() throws Exception {
        LightDna testDna = new Dna("TATGTTCAGATAATGCCCGATGACTTTGTCATGCAGCTCCAC");
        final int k = 29;
        LightDna subDna = new DnaView(testDna, 0, k);
        LightDna subDnaRC = DnaView.rcView(subDna);

        long kmer = KmerUtils.toLong(subDna);
        assertEquals(KmerUtils.toLong(subDnaRC), KmerUtils.reverseComplement(kmer, k));

    }

    @Test
    public void testToLongSimple() throws Exception {
        Dna dna1 = new Dna("GTCAGG");
        assertEquals(
                DnaTools.fromChar('G') * 1L +
                DnaTools.fromChar('G') * 4L +
                DnaTools.fromChar('A') * 16L +
                DnaTools.fromChar('C') * 64L +
                DnaTools.fromChar('T') * 256L +
                DnaTools.fromChar('G') * 1024L,
                KmerUtils.toLong(dna1));
    }

    @Test
    public void testToLongIndexed() throws Exception {
        LightDna testDna = new Dna("TATGTTCAGATAATGCCCGATGACTTTGTCATGCAGCTCCAC");
        final int k = 29;
        for (int i = 0; i + k <= testDna.length(); ++i) {
            System.err.println(i);
            LightDna subDna = new DnaView(testDna, i, i + k);
            LightDna subDnaRC = DnaView.rcView(subDna);

            assertEquals(KmerUtils.toLong(subDna), KmerUtils.toLong(testDna, i, i + k));
            assertEquals(KmerUtils.toLong(subDnaRC), KmerUtils.toLong(testDna, i + k, i));
        }
    }
}
