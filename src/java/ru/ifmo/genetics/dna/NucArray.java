package ru.ifmo.genetics.dna;

import java.util.Arrays;

public class NucArray {
    public final int length;
    private int[] array;

    protected final static int NUCS_IN_SINGLE_ELEMENT_LOG = 4;
    protected final static int NUCS_IN_SINGLE_ELEMENT = 1 << NUCS_IN_SINGLE_ELEMENT_LOG;
    protected final static int INDEX_MASK = NUCS_IN_SINGLE_ELEMENT - 1;

    protected final static int NUC_SIZE_BITS = 2;
    protected final static int NUC_MASK = (1 << NUC_SIZE_BITS) - 1;

    public NucArray(LightDna dna) {
        this(dna.length());
        for (int i = 0; i < length; ++i) {
            set(i, dna.nucAt(i));
        }
    }

    public NucArray(int length) {
        this.length = length;
        this.array = new int[(length + NUCS_IN_SINGLE_ELEMENT - 1) >> NUCS_IN_SINGLE_ELEMENT_LOG];
    }

    public NucArray(NucArray nucs) {
        this.length = nucs.length;
        this.array = Arrays.copyOf(nucs.array, nucs.array.length);
    }

    public void set(int index, byte value) {
        int i = index >> NUCS_IN_SINGLE_ELEMENT_LOG;
        int j = index & INDEX_MASK;

        byte oldValue = (byte) ((array[i] >>> (NUC_SIZE_BITS * j)) & NUC_MASK);
        array[i] ^= (oldValue ^ value) << (NUC_SIZE_BITS * j);
    }

    public byte get(int index) {
        int i = index >> NUCS_IN_SINGLE_ELEMENT_LOG;
        int j = index & INDEX_MASK;
        return (byte) ((array[i] >>> (NUC_SIZE_BITS * j)) & NUC_MASK);
    }

    public NucArray copy(int newLength) {
        NucArray res = new NucArray(newLength);
        System.arraycopy(array, 0, res.array, 0, array.length);
        return res;
    }

    @Override
    public String toString() {
        char[] c = new char[length];
        for (int i = 0; i < length; i++) {
            c[i] = DnaTools.toChar(get(i));
        }
        return new String(c);
    }

}
