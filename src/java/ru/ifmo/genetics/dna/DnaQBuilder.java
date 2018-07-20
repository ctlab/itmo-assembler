package ru.ifmo.genetics.dna;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import ru.ifmo.genetics.io.formats.QualityFormat;

import java.util.InputMismatchException;

public class DnaQBuilder {
    ByteArrayList data;

    public DnaQBuilder() {
        this(16);
    }

    public DnaQBuilder(int capacity) {
        data = new ByteArrayList(capacity);
    }

    public DnaQBuilder(DnaQ dnaq) {
        data = new ByteArrayList(dnaq.toByteArray());
    }

    private void ensureCapacity(int minimumCapacity) {
        data.ensureCapacity(minimumCapacity);
    }

    public static byte convertOne(byte nuc, byte phred) {
        nuc |= phred << 2;
        return nuc;
    }

    public void unsafeAppend(byte nuc, byte phred) {
        nuc |= phred << 2;
        data.add(nuc);
    }
    
    public void append(byte nuc, byte phred) {
        unsafeAppend(nuc, phred);
    }

    public void append(byte b) {
        data.add(b);
    }

    public void unsafeAppendUnknown() {
        unsafeAppend((byte) 0, (byte) 0);
    }

    public void appendUnknown() {
        append((byte) 0, (byte) 0);
    }
    
    public void append(DnaQ dnaq) {
        data.addElements(data.size(), dnaq.value, dnaq.offset, dnaq.length);
    }
    
    public void append(DnaQ dnaQ, int length) {
        append(dnaQ);
        // TODO faster
        for (int i = dnaQ.length; i < length; i++) {
            appendUnknown();
        }
    }

    public DnaQ toDnaQ() {
        return new DnaQ(data.toByteArray());
    }

    public DnaQ build() {
        DnaQ res = toDnaQ();
        data = null;
        return res;
    }
    
    public byte[] buildToByteArray() {
        return data.toByteArray();
    }


    public static DnaQ convert(char[] data, int dataFrom, int dataTo,
                               char[] qual, int qualFrom, int qualTo, QualityFormat qf) {
        if (dataTo - dataFrom != qualTo - qualFrom) {
            throw new InputMismatchException("Bad DnaQ record: length of chars and quality is not the same. " +
                    "File is corrupted/Format mismatch.");
        }

        byte[] res = new byte[dataTo - dataFrom + 1];
        for (int i = 0; i < res.length; i++) {
            if (data[dataFrom + i] == 'N' || data[dataFrom + i] == '.') {
                res[i] = 0;
//                res[i] = convertOne((byte) 0, (byte) 0);
            } else {
                res[i] = convertOne(DnaTools.fromChar(data[dataFrom + i]), qf.getPhred(qual[qualFrom + i]));
            }
        }
        return new DnaQ(res);
    }



    public int length() {
        return data.size();
    }

    public byte byteAt(int ind) {
        return data.get(ind);
    }

    public byte nucAt(int ind) {
        return (byte)(byteAt(ind) & 3);
    }

    public byte phredAt(int ind) {
        return (byte)(byteAt(ind) >> 2);
    }

    public void insert(int ind, byte b) {
        data.add(ind, b);
    }

    public void insert(int ind, byte nuc, byte phred) {
        insert(ind, (byte)((phred << 2) + nuc));
    }

    public void delete(int ind) {
        data.remove(ind);
    }

    public void set(int ind, byte b) {
        data.set(ind, b);
    }

    public void setNuc(int ind, byte nuc) {
        set(ind, nuc, phredAt(ind));
    }

    public void setPhred(int ind, byte phred) {
        set(ind, nucAt(ind), phred);
    }

    public void set(int ind, byte nuc, byte phred) {
        data.set(ind, (byte)((phred << 2) + nuc));
    }

    public DnaQ subDnaQ(int begin, int end) {
        return new DnaQ(data.subList(begin, end).toByteArray());
    }

    @Override
    public String toString() {
        return toDnaQ().toString();
    }

}
