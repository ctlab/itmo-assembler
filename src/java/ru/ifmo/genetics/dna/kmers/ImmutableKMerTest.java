package ru.ifmo.genetics.dna.kmers;

import org.junit.Test;

import static org.junit.Assert.*;

public class ImmutableKMerTest {
    @Test
    public void testLongHashCode() throws Exception {
        String s = "TAGCGACAGGGG";
        ImmutableBigKmer kmer = new ImmutableBigKmer(s);
        ImmutableBigKmer kmer2 = new ImmutableBigKmer(s);

        assertEquals(false, kmer.longHashCode() == 0);

        assertEquals(kmer.longHashCode(), kmer2.longHashCode());

        ImmutableBigKmer kmer3 = new ImmutableBigKmer("GTATCGAGTACGATCTTACGTATGCCCGTCGCGCTACTACATCGCGCATCTCACGCGCTACATCAC");
        assertEquals(false, kmer3.longHashCode() == kmer3.hashCode());

        ImmutableBigKmer kmer4 = new ImmutableBigKmer("GTCA");
        assertEquals(kmer4.hashCode(), kmer4.longHashCode());
    }

    @Test
    public void testShiftRight() {
        String s = "GTAGAGACTGGCCCCGATAA";
        String s1 = (s + "G").substring(1);

        ImmutableBigKmer kmer = new ImmutableBigKmer(s);
        ImmutableBigKmer kmer1 = new ImmutableBigKmer(s1);
        ImmutableBigKmer kmer2 = kmer.shiftRight('G');
        assertEquals(kmer1, kmer2);
        assertEquals(kmer1.longHashCode(), kmer2.longHashCode());
    }

    @Test
    public void testShiftLeft() {
        String s = "GTAGAGACTGGCCCCGATAA";
        String s1 = ("G" + s).substring(0, s.length());

        ImmutableBigKmer kmer = new ImmutableBigKmer(s);
        ImmutableBigKmer kmer1 = new ImmutableBigKmer(s1);
        ImmutableBigKmer kmer2 = kmer.shiftLeft('G');
        assertEquals(kmer1, kmer2);
        assertEquals(kmer1.longHashCode(), kmer2.longHashCode());
    }
}
