package ru.ifmo.genetics.dna;

import org.junit.Test;
import ru.ifmo.genetics.dna.kmers.ImmutableBigKmer;
import ru.ifmo.genetics.dna.kmers.MutableBigKmer;

import static org.junit.Assert.*;

public class MutableKMerTest {

    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    @Test
    public void testShiftRight() throws Exception {
        int k = 21;
        String s = "GTCAGTCTCAGTCATCAGACGATATCATCATCAGGAGGAGCTATCAGAGC";

        MutableBigKmer kmer = null;

        for (int i = k; i <= s.length(); ++i) {
            if (kmer == null) {
                kmer = new MutableBigKmer(s.substring(i - k, i));
            } else {
                kmer.shiftRight(s.charAt(i - 1));
            }
            ImmutableBigKmer kmer1 = new ImmutableBigKmer(s.substring(i - k, i));
            assertEquals(kmer1, kmer);
            assertEquals(kmer1.longHashCode(), kmer.longHashCode());
            assertEquals(kmer1.longHashCode(), kmer.fwLongHashCode());
            assertEquals(DnaView.rcView(kmer1).longHashCode(), kmer.rcLongHashCode());
        }

    }

    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    @Test
    public void testShiftLeft() throws Exception {
        int k = 21;
        String s = "GTCAGTCTCAGTCATCAGACGATATCATCATCAGGAGGAGCTATCAGAGC";
        MutableBigKmer kmer = null;

        for (int i = s.length() - k; i >= 0; --i) {
            if (kmer == null) {
                kmer = new MutableBigKmer(s.substring(i));
            } else {
                kmer.shiftLeft(s.charAt(i));
            }
            ImmutableBigKmer kmer1 = new ImmutableBigKmer(s.substring(i, i + k));
            assertEquals(kmer1, kmer);
            assertEquals(kmer1.longHashCode(), kmer.longHashCode());
            assertEquals(kmer1.longHashCode(), kmer.fwLongHashCode());
            assertEquals(DnaView.rcView(kmer1).longHashCode(), kmer.rcLongHashCode());
        }

    }

    @Test
    public void testBiLongHashCode() {
        int k = 21;
        String s = "GTCAGTCTCAGTCATCAGACGATATCATCATCAGGAGGAGCTATCAGAGC";

        boolean firstly = true;
        MutableBigKmer kmer = null;
        MutableBigKmer rcKmer = null;

        for (int i = 0; i + k < s.length(); ++i) {
            if (firstly) {
                kmer = new MutableBigKmer(s.substring(i, i + k));
                rcKmer = new MutableBigKmer(DnaView.rcView(kmer));
                firstly = false;
            } else {
                kmer.shiftRight(s.charAt(i + k - 1));
                rcKmer.shiftLeft((byte) (DnaTools.fromChar(s.charAt(i + k - 1)) ^ 3));
            }
            assertEquals(kmer.biLongHashCode(), rcKmer.biLongHashCode());
        }
    }

    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    @Test
    public void testKmerIterable() {
        int k = 21;
        String s = "GTCAGTCTCAGTCATCAGACGATATCATCATCAGGAGGAGCTATCAGAGC";

        int i = 0;
        for (MutableBigKmer kmer: MutableBigKmer.kmersOf(new Dna(s), k)) {
            ImmutableBigKmer kmer1 = new ImmutableBigKmer(s.substring(i, i + k));
            assertEquals(kmer1, kmer);
            assertEquals(kmer1.longHashCode(), kmer.longHashCode());
            i++;
        }
    }
}
