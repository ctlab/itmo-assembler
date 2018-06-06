package ru.ifmo.genetics.dna;

import ru.ifmo.genetics.utils.Misc;

/**
 * deprecated use DnaQWritable instead
 * This class is very heterogeneous.
 */
public class DnaQ implements IDnaQ {
    protected final static int sz_shift = 2;
    protected final static int sz = 1 << sz_shift;
    protected final static int sz_mod_mask = sz - 1; 
    
    public byte[] value;
    public int length;
    public int offset;
    private boolean rev;
    private boolean compl;
    public static DnaQ emptyDnaQ = new DnaQ(0);

    public DnaQ(String nucs, int phred) {
        this(nucs.length());
        for (int i = 0; i < nucs.length(); ++i) {
            if (nucs.charAt(i) == 'N' || nucs.charAt(i) == 'n' || nucs.charAt(i) == '.') {
                set(i, 0, 0);
            } else {
                set(i, DnaTools.fromChar(nucs.charAt(i)), phred);
            }
        }
    }

    public DnaQ(byte[] value) {
        this(value, 0, value.length);
    }

    public DnaQ(byte[] value, int offset, int length, boolean rev, boolean compl) {
        this.value = value;
        this.offset = offset;
        this.length = length;
        this.rev = rev;
        this.compl = compl;
    }

    public DnaQ(byte[] value, int offset, int length) {
        this(value, offset, length, false, false);
    }

    public DnaQ(int length) {
        this(new byte[length], 0, length);
    }

    public DnaQ(LightDnaQ... dnas) {
        this(Misc.sumOfDnaLengths(dnas));
        int x = 0;
        for (LightDnaQ dna : dnas) {
            for (int i = 0; i < dna.length(); i++) {
                set(x++, dna.nucAt(i), dna.phredAt(i));
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof DnaQ)) return false;

        DnaQ that = (DnaQ)other;
        if (length != that.length) {
            return false;
        }
        for (int i = 0; i < length; ++i) {
            if (byteAt(i) != that.byteAt(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean nucEquals(LightDna other) {
        if (other == null) return false;

        if (length != other.length()) {
            return false;
        }
        for (int i = 0; i < length; ++i) {
            if (nucAt(i) != other.nucAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static void setNucInArray(byte[] array, int xxx, byte value) {
        int i = xxx >> sz_shift;
        int j = xxx & sz_mod_mask;
        array[i] |= value << (2 * j);
    }

    public DnaQ reverse() {
        return new DnaQ(value, offset, length, !rev, compl);
    }

    public DnaQ complement() {
        return new DnaQ(value, offset, length, rev, !compl);
    }

    public DnaQ reverseComplement() {
        return clone().inplaceReverseComplement();
    }

    public DnaQ inplaceReverseComplement() {
        rev = !rev;
        compl = !compl;
        return this;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public byte nucAt(int index) {
        int x = offset + (rev ? length - 1 - index : index);
        byte nuc = (byte) (value[x] & 3);
        return (byte) (compl ? nuc ^ 3 : nuc);
    }

    public byte byteAt(int index) {
        int x = offset + (rev ? length - 1 - index : index);
        return (byte) (compl ? value[x] ^ 3 : value[x]);
    }

    public void setByte(int index, byte nv) {
        int x = offset + (rev ? length - 1 - index : index);
        value[x] = (byte) (compl ? nv ^ 3 : nv);
    }

    public void set(int index, int nuc, int phred) {
        int nv = (phred << 2) | nuc;
        int x = offset + (rev ? length - 1 - index : index);
        value[x] = (byte) (compl ? nv ^ 3 : nv);
    }
    
    @Override
    public byte phredAt(int index) {
        int x = offset + (rev ? length - 1 - index : index);
        return (byte)((value[x] >>> 2) & 63);
    }

    @Override
    public void setPhred(int index, int phred) {
        set(index, nucAt(index), phred);
    }

    @Override
    public void setNuc(int index, int nuc) {
        set(index, nuc, phredAt(index));
    }
    
    public DnaQ substring(int beginIndex, int endIndex) {
        return clone().inplaceSubstring(beginIndex, endIndex);
    }

    public DnaQ inplaceSubstring(int beginIndex, int endIndex) {
        offset += (rev ? length - endIndex : beginIndex);
        length = endIndex - beginIndex;
        return this;
    }

    public DnaQ truncateByQuality(int phredThreshold) {
        return clone().inplaceTruncateByQuality(phredThreshold);
    }

    public DnaQ inplaceTruncateByQuality(int phredThreshold) {
        int trustLength = 0;
        while (trustLength < length) {
            if (phredAt(trustLength) < phredThreshold) {
                break;
            }
            trustLength++;
        }
        length = trustLength;
        return this;
    }

    public boolean startsWithGenerous(LightDna prefix) {
        return startsWithGenerous(prefix, 0);
    }

    public boolean startsWithGenerous(LightDna prefix, int from) {
        if (length - from < prefix.length())
            return false;
        for (int i = 0; i < prefix.length(); i++) {
            if (phredAt(from + i) > 0 && nucAt(from + i) != prefix.nucAt(i))
                return false;
        }
        return true;
    }

    public byte[] toByteArray() {
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = byteAt(i);
        }
        return b;
    }

    public byte[] toNucArray() {
        byte[] b = new byte[length];
        for (int i = 0; i < length; ++i) {
            b[i] = nucAt(i);
        }
        return b;
    }

    public DnaQ clone() {
        return new DnaQ(value, offset, length, rev, compl);
    }

    public static DnaQ getPrototype(int length) {
        return new DnaQ(length);
    }

    @Override
    public String toString() {
        return DnaTools.toString(this);
    }

}
