package ru.ifmo.genetics.dna.kmers;

import ru.ifmo.genetics.dna.*;

public class ShallowBigKmer extends AbstractBigKmer {
    private long fwLongHash;
    private long rcLongHash;
    private int k;

    public ShallowBigKmer(BigKmer kmer) {
        this(kmer.fwLongHashCode(), kmer.rcLongHashCode(), kmer.length());
    }

    public ShallowBigKmer(LightDna kmer) {
        this(DnaTools.longHashCode(kmer), DnaTools.longHashCode(DnaView.rcView(kmer)), kmer.length());
    }

    public ShallowBigKmer(long kmer, int k) {
        for (int i = 0; i < k; ++i) {
            byte nuc = (byte) (kmer & 3);
            kmer >>>= 2;
            appendLeft(nuc);
        }
    }

    protected ShallowBigKmer(long fwLongHash, long rcLongHash, int k) {
        this.fwLongHash = fwLongHash;
        this.rcLongHash = rcLongHash;
        this.k = k;
    }

    public void appendRight(byte nuc) {
        fwLongHash *= DnaTools.HASH_BASE;
        fwLongHash += nuc;

        rcLongHash += DnaTools.LONG_HASH_BASE_POWERS[k][(nuc ^ 3) + 3];
        k++;
    }

    public void removeRight(byte rightNuc) {
        k--;
        fwLongHash -= rightNuc;
        fwLongHash *= DnaTools.HASH_INVERSED_BASE;

        rcLongHash -= DnaTools.LONG_HASH_BASE_POWERS[k][(rightNuc ^ 3) + 3];
    }

    public void appendLeft(byte nuc) {
        fwLongHash += DnaTools.LONG_HASH_BASE_POWERS[k][nuc + 3];

        rcLongHash *= DnaTools.HASH_BASE;
        rcLongHash += (nuc ^ 3);
        k++;
    }

    public void removeLeft(byte leftNuc) {
        k--;
        fwLongHash -= DnaTools.LONG_HASH_BASE_POWERS[k][leftNuc + 3];

        rcLongHash -= (leftNuc ^ 3);
        rcLongHash *= DnaTools.HASH_INVERSED_BASE;
    }

    public void shiftRight(byte newRightNuc, byte oldLeftNuc) {
        appendRight(newRightNuc);
        removeLeft(oldLeftNuc);
    }

    public void shiftLeft(byte newLeftNuc, byte oldRightNuc) {
        appendLeft(newLeftNuc);
        removeRight(oldRightNuc);
    }

    public void updateAt(int i, byte oldNuc, byte newNuc) {
        fwLongHash += DnaTools.LONG_HASH_BASE_POWERS[k - i - 1][newNuc - oldNuc + 3];
        rcLongHash += DnaTools.LONG_HASH_BASE_POWERS[i][(newNuc ^ 3) - (oldNuc ^ 3) + 3];
    }

    @Override
    public int length() {
        return k;
    }

    @Override
    public byte nucAt(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long fwLongHashCode() {
        return fwLongHash;
    }

    @Override
    public long rcLongHashCode() {
        return rcLongHash;
    }

    @Override
    public String toString() {
        return "<shallow " + k + "-mer>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShallowBigKmer)) return false;

        ShallowBigKmer that = (ShallowBigKmer) o;

        if (fwLongHash != that.fwLongHash) return false;
        if (rcLongHash != that.rcLongHash) return false;
        if (k != that.k) return false;

        return true;
    }
}
