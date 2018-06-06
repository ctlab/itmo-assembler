package ru.ifmo.genetics.dna.kmers;

import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.utils.KmerUtils;

import static org.junit.Assert.*;

public class ShortKmerTest {
    @Test
    public void testKmersOf() throws Exception {
        int k = 21;
        long MASK = (1L << (2 * k)) - 1;
        Dna dna = new Dna("ATGCATCATTCAGCTACGACTTTCAGCTCATCACTACTA");
        long cur = 0;
        int i;
        for (i = 0; i < k - 1; i++) {
            cur = cur << 2 | dna.nucAt(i);
        }
        for (ShortKmer kmer : ShortKmer.kmersOf(dna, k)) {
            cur = cur & (MASK >> 2);
            cur = (cur << 2) | dna.nucAt(i);

            long curRc = KmerUtils.reverseComplement(cur, k);
            assertEquals(cur, kmer.fwKmer());
            assertEquals(curRc, kmer.rcKmer());
            assertEquals(Math.min(cur, curRc), kmer.toLong());
            i++;
        }
    }
}
