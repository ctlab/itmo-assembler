package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import ru.ifmo.genetics.dna.*;
import ru.ifmo.genetics.io.formats.QualityFormat;

import java.io.*;
import java.security.MessageDigest;
import java.util.InputMismatchException;

public class DnaQWritable implements WritableComparable<DnaQWritable>, IDnaQ {
    private ByteArrayWritable value = new ByteArrayWritable();

    public DnaQWritable() {

    }

    public DnaQWritable(Text nucleotides, Text quality, QualityFormat qf) {
        this();
        set(nucleotides, quality, qf);
    }

    public int length() {
        return value.length();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        value.write(dataOutput);
    }


    @Override
    public void readFields(DataInput dataInput) throws IOException {
        value.readFields(dataInput);
    }

    @Override
    public void setNuc(int i, int nuc) {
        value.set(i, (byte) (value.get(i) & 0xfc | nuc));
    }

    @Override
    public void setPhred(int i, int phred) {
        phred = Math.max(phred, 1);
        value.set(i, (byte) (value.get(i) & 0x03 | (phred << 2)));
    }

    @Override
    public IDnaQ reverse() {
        int sz = length();
        for (int i = 0; i < sz / 2; ++i) {
            byte b = value.get(i);
            value.set(i, value.get(sz - i - 1));
            value.set(sz - i - 1, b);
        }

        return this;
    }

    @Override
    public IDnaQ complement() {
        int sz = length();
        for (int i = 0; i < sz; ++i) {
            value.set(i, (byte) (value.get(i) ^ 3));
        }

        return this;
    }

    public void set(int i, int nuc, int phred) {
        assert 0 <= nuc && nuc < 4;
        assert 0 <= phred && phred < 64;
        phred = Math.max(phred, 1);
        set(i, (byte) ((phred << 2) | nuc));
    }

    public byte nucAt(int i) {
        return (byte) (value.get(i) & 3);
    }

    public byte phredAt(int i) {
        return (byte) ((value.get(i) >>> 2) & 63);
    }

    private void set(int i, byte b) {
        //assert b >= 4;
        value.set(i, b);
    }

    public void set(byte[] nucleotides, int nucleotidesOffset,
                    byte[] qualities, int qualitiesOffset,
                    int length,
                    QualityFormat qf) {

        value.reset(length);
        for (int i = 0; i < length; i++) {
            byte nuc = nucleotides[i + nucleotidesOffset];
            byte phred = qualities[i + qualitiesOffset];
            boolean singleNuc = false;
            if (!DnaTools.isNucleotide((char) nuc)) {
                // :ToDo: check if it is a valid multinucleotide char
                set(i, 0, 0);
            } else {
                set(i, DnaTools.fromChar((char)nuc), qf.getPhred((char) phred));
            }
        }
    }

    public void set(Text nucleotides, Text quality, QualityFormat qf) {
        if (nucleotides.getLength() != quality.getLength()) {
            throw new InputMismatchException(
                    "nucleotides.length()" + nucleotides.getLength() +
                            " != quality.length()" + quality.getLength() +
                            "  nucs:" + nucleotides + " qual:" + quality);
        }
        set(nucleotides.getBytes(), 0,
                quality.getBytes(), 0,
                nucleotides.getLength(),
                qf);
    }

    public long toLong(int begin, int end) {
        if (end - begin > Long.SIZE / 2) {
            throw new IllegalArgumentException("Can't convert DnaQ to long, because length = " + (end - begin));
        }
        long res = 0;
        for (int i = begin; i < end; ++i) {
            res <<= 2;
            res |= nucAt(i);
        }
        return res;
    }

    public String toString() {
        return DnaTools.toString(this);
    }
    
    public String toPhredString() {
        return DnaTools.toPhredString(this);
    }

    @Override
    public int compareTo(DnaQWritable o) {
        return DnaTools.compare(this, o);
    }

    public void updateDigest(MessageDigest md) {
        value.updateDigest(md);
    }

    public void copyFieldsFrom(DnaQWritable old) {
        value.reset(old.length());
        for (int i = 0; i < old.length(); i++) {
            value.set(i, old.value.get(i));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DnaQWritable that = (DnaQWritable) o;

        if (value != null ? !value.equals(that.value) : that.value != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    public void clear() {
        value.clear();
    }
}
