package ru.ifmo.genetics.dna;

import ru.ifmo.genetics.utils.Misc;

/**
 * Immutable, 2-bits-per-nucleotide.
 */
public class Dna extends AbstractLightDna {
    public static final Dna emptyDna = new Dna("");

    public static final Dna A = new Dna("A");
    public static final Dna T = new Dna("T");
    public static final Dna G = new Dna("G");
    public static final Dna C = new Dna("C");

    public static final Dna[] oneNucDnas;
    static {
        oneNucDnas = new Dna[DnaTools.NUCLEOTIDES.length];
        for (byte nuc = 0; nuc < DnaTools.NUCLEOTIDES.length; nuc++) {
            oneNucDnas[nuc] = new Dna(String.valueOf(DnaTools.toChar(nuc)));
        }
    }

    protected final NucArray nucs;
    protected final int offset;
    protected final int length;
    protected final boolean rev;
    protected final boolean compl;

    protected Dna(NucArray nucs, int offset, int length, boolean rev, boolean compl) {
        this.nucs = nucs;
        this.offset = offset;
        this.length = length;
        this.rev = rev;
        this.compl = compl;
    }
    protected Dna(NucArray nucs, int offset, int length) {
        this(nucs, offset, length, false, false);
    }

    public Dna(Dna other) {
        this(other.nucs, other.offset, other.length, other.rev, other.compl);
    }

    // one nuc per index
    public Dna(byte[] nucs) {
        this(nucs.length);
        for (int i = 0; i < nucs.length; ++i) {
            this.nucs.set(i, nucs[i]);
        }
    }
    


    public Dna(int length) {
        this(new NucArray(length), 0, length);
    }

    public Dna(CharSequence s) {
        this(s.length());
        for (int i = 0; i < length; i++) {
            nucs.set(i, DnaTools.fromChar(s.charAt(i)));
        }
    }

    /**
     * Makes full DNA copy
     */
    public Dna(LightDna dna) {
        this(dna.length());
        for (int i = 0; i < length; i++) {
            nucs.set(i, dna.nucAt(i));
        }
    }

    /**
     * Makes full copy of all DNAs
     */
    public Dna(LightDna... dnas) {
        this(Misc.sumOfDnaLengths(dnas));
        int x = 0;
        for (LightDna dna : dnas) {
            for (int i = 0; i < dna.length(); i++, x++) {
                nucs.set(x, dna.nucAt(i));
            }
        }
    }

    public Dna reverse() {
        return new Dna(nucs, offset, length, !rev, compl);
    }

    public Dna complement() {
        return new Dna(nucs, offset, length, rev, !compl);
    }

    public Dna reverseComplement() {
        return new Dna(nucs, offset, length, !rev, !compl);
    }
    
    @Override
    public int length() {
        return length;
    }

    public byte nucAt(int index) {
        int x = offset + (rev ? length - 1 - index: index);
        byte nuc = nucs.get(x);
        return (byte) (compl ? nuc ^ 3 : nuc);
    }
    
    public byte[] toByteArray() {
        byte[] array = new byte[length()];
        for (int i = 0; i < length; ++i) {
            array[i] = nucAt(i);
        }
        return array;
    }

    public Dna substring(int beginIndex, int endIndex) {
        if (beginIndex == 0 && endIndex == length) {
            return this;
        }
        return new Dna(nucs, offset + (rev ? length - endIndex : beginIndex), endIndex - beginIndex, rev, compl);
    }

//    public Dna concat(Dna that) {
//        int length = count + that.count;
//        int[] value = newArray(length);
//        for (int i = 0; i < count; i++) {
//            setNucInArray(value, i, nucAt(i));
//        }
//        for (int i = 0; i < that.count; i++) {
//            setNucInArray(value, count + i, that.nucAt(i));
//        }
//        return new Dna(value, 0, length);
//    }
//
//    @Override
//    public int compareTo(IDna o) {
//        if (this == o) {
//            return 0;
//        }
//        int i = 0;
//        int j = 0;
//        // TODO faster?
//        while (i < count && j < o.length()) {
//            byte n1 = nucAt(i++);
//            byte n2 = o.nucAt(j++);
//            if (n1 != n2) {
//                return n1 - n2;
//            }
//        }
//        return count - o.length();
//    }

}
