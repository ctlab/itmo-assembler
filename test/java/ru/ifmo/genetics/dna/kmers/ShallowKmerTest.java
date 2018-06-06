package ru.ifmo.genetics.dna.kmers;

import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.utils.KmerUtils;

import static org.junit.Assert.*;

public class ShallowKmerTest {
    @Test
    public void testAppendRight() throws Exception {
        String s = "GTCAGTCATTTCAGCAC";
        char c = 'G';
        ImmutableBigKmer kmer = new ImmutableBigKmer(s);
        ImmutableBigKmer k1mer = new ImmutableBigKmer(s + c);

        ShallowBigKmer shallowKmer = new ShallowBigKmer(kmer);
        assertEquals(kmer.biLongHashCode(), shallowKmer.biLongHashCode());
        shallowKmer.appendRight(DnaTools.fromChar(c));
        assertEquals(k1mer.biLongHashCode(), shallowKmer.biLongHashCode());
    }

    @Test
    public void testRemoveRight() throws Exception {
        String s = "GTCAGTCATTTCAGCAC";
        char c = 'G';
        ImmutableBigKmer kmer = new ImmutableBigKmer(s);
        ImmutableBigKmer k1mer = new ImmutableBigKmer(s + c);

        ShallowBigKmer shallowKmer = new ShallowBigKmer(k1mer);
        assertEquals(k1mer.biLongHashCode(), shallowKmer.biLongHashCode());
        shallowKmer.removeRight(DnaTools.fromChar(c));
        assertEquals(kmer.biLongHashCode(), shallowKmer.biLongHashCode());
    }

    @Test
    public void testAppendLeft() throws Exception {
        String s = "GTCAGTCATTTCAGCAC";
        char c = 'G';
        ImmutableBigKmer kmer = new ImmutableBigKmer(s);
        ImmutableBigKmer k1mer = new ImmutableBigKmer(c + s);

        ShallowBigKmer shallowKmer = new ShallowBigKmer(kmer);
        assertEquals(kmer.biLongHashCode(), shallowKmer.biLongHashCode());
        shallowKmer.appendLeft(DnaTools.fromChar(c));
        assertEquals(k1mer.biLongHashCode(), shallowKmer.biLongHashCode());
    }

    @Test
    public void testRemoveLeft() throws Exception {
        String s = "GTCAGTCATTTCAGCAC";
        char c = 'G';
        ImmutableBigKmer kmer = new ImmutableBigKmer(s);
        ImmutableBigKmer k1mer = new ImmutableBigKmer(c + s);

        ShallowBigKmer shallowKmer = new ShallowBigKmer(k1mer);
        assertEquals(k1mer.biLongHashCode(), shallowKmer.biLongHashCode());
        shallowKmer.removeLeft(DnaTools.fromChar(c));
        assertEquals(kmer.biLongHashCode(), shallowKmer.biLongHashCode());
    }

    @Test
    public void testShiftRight() throws Exception {
        String s = "GTCAGTCATTTCAGCAC";
        char c = 'G';
        ImmutableBigKmer kmer = new ImmutableBigKmer(s);
        ImmutableBigKmer kmer1 = kmer.shiftRight(c);

        ShallowBigKmer shallowKmer = new ShallowBigKmer(kmer);
        assertEquals(kmer.biLongHashCode(), shallowKmer.biLongHashCode());
        shallowKmer.shiftRight(kmer1.lastNuc(), kmer.firstNuc());
        assertEquals(kmer1.biLongHashCode(), shallowKmer.biLongHashCode());
    }

    @Test
    public void testShiftLeft() throws Exception {
        String s = "GTCAGTCATTTCAGCAC";
        char c = 'G';
        ImmutableBigKmer kmer = new ImmutableBigKmer(s);
        ImmutableBigKmer kmer1 = kmer.shiftLeft(c);

        ShallowBigKmer shallowKmer = new ShallowBigKmer(kmer);
        assertEquals(kmer.biLongHashCode(), shallowKmer.biLongHashCode());
        shallowKmer.shiftLeft(kmer1.firstNuc(), kmer.lastNuc());
        assertEquals(kmer1.biLongHashCode(), shallowKmer.biLongHashCode());
    }

    @Test
    public void testUpdateAt() throws Exception {
        String s = "GTCAGTCATTTCAGCAC";
        int i = 6;
        char c = 'G';
        String s1 = s.substring(0, i) + c + s.substring(i + 1);
        ImmutableBigKmer kmer = new ImmutableBigKmer(s);
        ImmutableBigKmer kmer1 = new ImmutableBigKmer(s1);
        assertFalse(kmer.equals(kmer1));

        ShallowBigKmer shallowKmer = new ShallowBigKmer(kmer);
        assertEquals(kmer.biLongHashCode(), shallowKmer.biLongHashCode());
        shallowKmer.updateAt(i, kmer.nucAt(i), kmer1.nucAt(i));
        assertEquals(kmer1.biLongHashCode(), shallowKmer.biLongHashCode());

    }

    @Test
    public void testConstructFromLong() {
        String s = "GTCAGTCGTATGCATTTCAGCAC";
        Dna dna = new Dna(s);
        long kmerLong = KmerUtils.toLong(dna);
        ShallowBigKmer kmer1 = new ShallowBigKmer(dna);
        ShallowBigKmer kmer2 = new ShallowBigKmer(kmerLong, s.length());
        assertEquals(kmer1, kmer2);
        assertEquals(kmer1.fwLongHashCode(), kmer2.fwLongHashCode());
        assertEquals(kmer1.rcLongHashCode(), kmer2.rcLongHashCode());
    }

    @Test
    public void testHashIsGood() {
        String s1 = "AAAGCCGTGGCGGGCGTCGAAT";
        String s2 = "AAAAGCACTCCGCTGATGAAAT";
        ShallowBigKmer kmer1 = new ShallowBigKmer(new Dna(s1));
        ShallowBigKmer kmer2 = new ShallowBigKmer(new Dna(s2));
        System.err.println(kmer1.fwLongHashCode());
        System.err.println(kmer1.rcLongHashCode());
        System.err.println(kmer2.fwLongHashCode());
        System.err.println(kmer2.rcLongHashCode());
        assertFalse(kmer1.biLongHashCode() == kmer2.biLongHashCode());

    }

}
