package ru.ifmo.genetics.dna.kmers;

import ru.ifmo.genetics.dna.*;

public class ImmutableBigKmer extends AbstractBigKmer {
    private final MutableBigKmer internalKmer;

    protected ImmutableBigKmer(MutableBigKmer internalKmer) {
        this.internalKmer = internalKmer;
    }

    public ImmutableBigKmer(LightDna dna) {
        this(new MutableBigKmer(dna));
    }

    public ImmutableBigKmer(CharSequence s) {
        this(new Dna(s));
    }

    public ImmutableBigKmer shiftRight(char nuc) {
        return shiftRight(DnaTools.fromChar(nuc));
    }

    public ImmutableBigKmer shiftRight(byte nuc) {
        MutableBigKmer tempKmer = new MutableBigKmer(internalKmer);
        tempKmer.shiftRight(nuc);
        return new ImmutableBigKmer(tempKmer);
    }

    public ImmutableBigKmer shiftRight(LightDna nucs) {
        MutableBigKmer tempKmer = new MutableBigKmer(internalKmer);
        for (int i = 0; i < nucs.length(); ++i) {
            tempKmer.shiftRight(nucs.nucAt(i));
        }
        return new ImmutableBigKmer(tempKmer);
    }

    public ImmutableBigKmer shiftLeft(char nuc) {
        return shiftLeft(DnaTools.fromChar(nuc));
    }

    public ImmutableBigKmer shiftLeft(byte nuc) {
        MutableBigKmer tempKmer = new MutableBigKmer(internalKmer);
        tempKmer.shiftLeft(nuc);
        return new ImmutableBigKmer(tempKmer);
    }

    public ImmutableBigKmer shiftLeft(LightDna nucs) {
        MutableBigKmer tempKmer = new MutableBigKmer(internalKmer);
        for (int i = nucs.length() - 1; i >= 0; --i) {
            tempKmer.shiftLeft(nucs.nucAt(i));
        }
        return new ImmutableBigKmer(tempKmer);
    }

    @Override
    public int length() {
        return internalKmer.length();
    }

    @Override
    public byte nucAt(int index) {
        return internalKmer.nucAt(index);
    }

    @Override
    public long fwLongHashCode() {
        return internalKmer.fwLongHashCode();
    }

    @Override
    public long rcLongHashCode() {
        return internalKmer.rcLongHashCode();
    }

    @Override
    public int hashCode() {
        return internalKmer.hashCode();
    }
}
