package ru.ifmo.genetics.dna.kmers;

import ru.ifmo.genetics.dna.*;

import java.util.Iterator;

public class MutableBigKmer extends AbstractBigKmer {
    private NucArray nucs;
    private ShallowBigKmer shallowKmer;
    private int first;


    private MutableBigKmer(NucArray nucs, ShallowBigKmer shallowKmer, int first) {
        this.nucs = nucs;
        this.shallowKmer = shallowKmer;
        this.first = first;
    }

    public MutableBigKmer(MutableBigKmer kmer) {
        this(new NucArray(kmer.nucs), new ShallowBigKmer(kmer.shallowKmer), kmer.first);
    }

    public MutableBigKmer(LightDna dna) {
        shallowKmer = new ShallowBigKmer(dna);
        nucs = new NucArray(dna);
    }

    public MutableBigKmer(BigKmer kmer) {
        shallowKmer = new ShallowBigKmer(kmer);
        nucs = new NucArray(kmer);
    }

    public MutableBigKmer(CharSequence s) {
        this(new Dna(s));
    }

    public void shiftRight(char nuc) {
        shiftRight(DnaTools.fromChar(nuc));
    }

    public void shiftRight(byte nuc) {
        shallowKmer.shiftRight(nuc, nucs.get(first));
        nucs.set(first, nuc);
        first = (first + 1) % nucs.length;
    }

    public void shiftLeft(char nuc) {
        shiftLeft(DnaTools.fromChar(nuc));
    }

    public void shiftLeft(byte nuc) {
        first = (first - 1 + nucs.length) % nucs.length;
        shallowKmer.shiftLeft(nuc, nucs.get(first));
        nucs.set(first, nuc);
    }

    @Override
    public int length() {
        return nucs.length;
    }

    @Override
    public byte nucAt(int index) {
        return nucs.get((first + index) % nucs.length);
    }

    public static Iterable<MutableBigKmer> kmersOf(LightDna dna, int k) {
        return new KmerIterable(dna, k);
    }

    private static class KmerIterable implements Iterable<MutableBigKmer> {
        final LightDna dna;
        final int k;

        private KmerIterable(LightDna dna, int k) {
            this.dna = dna;
            this.k = k;
        }

        @Override
        public Iterator<MutableBigKmer> iterator() {
            return new KmerIterator();
        }

        private class KmerIterator implements Iterator<MutableBigKmer> {
            int i = k - 1;
            MutableBigKmer kmer = null;

            @Override
            public boolean hasNext() {
                return i < dna.length();
            }

            @Override
            public MutableBigKmer next() {
                if (kmer == null) {
                    kmer = new MutableBigKmer(new DnaView(dna, 0, k));
                } else {
                    kmer.shiftRight(dna.nucAt(i));
                }
                i++;
                return kmer;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public long rcLongHashCode() {
        return shallowKmer.rcLongHashCode();
    }

    @Override
    public long fwLongHashCode() {
        return shallowKmer.fwLongHashCode();
    }

    @Override
    public long longHashCode() {
        return shallowKmer.longHashCode();
    }

    @Override
    public int hashCode() {
        return (int) longHashCode();
    }
}
