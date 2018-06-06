package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.distributed.clusterization.types.Kmer;
import ru.ifmo.genetics.distributed.io.KmerIterable;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.IDna;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.utils.KmerUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Created by IntelliJ IDEA.
 * User: alserg
 * Date: 03.11.11
 * <p/>
 * Time: 11:49
 */
public class DnaWritable implements Writable, IDna, KmerIterable, Copyable<DnaWritable> {
    protected final static int sz_shift = 2;
    protected final static int sz = 1 << sz_shift;
    protected final static int sz_mod_mask = sz - 1;
    byte[] array;
    private int length;

    public DnaWritable(LightDna dna, int start, int end) {
        this(end - start);
        for (int i = start; i < end; i++) {
            setNuc(i - start, dna.nucAt(i));
        }
    }

    public DnaWritable(LightDna dna) {
        this(dna, 0, dna.length());
    }

    public void adjustLength(int needLength) {
        if (needLength > array.length) {
            array = new byte[needLength * 3 / 2 + 1];
        }
    }

    private static int getArrayLength(int length) {
        return (length + sz - 1) >> sz_shift;
    }

    public DnaWritable() {
        this(0);
    }

    public DnaWritable(int length) {
        this.length = length;
        this.array = new byte[getArrayLength(length)];
    }

    public void set(LightDna dna) {
        length = dna.length();
        int needLength = getArrayLength(length);
        adjustLength(needLength);
        for (int i = 0; i < length; ++i) {
            setNuc(i, dna.nucAt(i));
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(length);
        int needLength = getArrayLength(length);
        dataOutput.write(array, 0, needLength);
    }


    @Override
    public void readFields(DataInput dataInput) throws IOException {
        length = dataInput.readInt();
        int needLength = getArrayLength(length);
        adjustLength(needLength);
        dataInput.readFully(array, 0, needLength);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public byte nucAt(int pos) {
        int i = pos >> sz_shift;
        int j = pos & sz_mod_mask;
        return (byte) ((array[i] >>> (2 * j)) & 3);
    }

    @Override
    public IDna reverse() {
        for (int i = 0; i < length / 2; ++i) {
            byte nuc = nucAt(i);
            setNuc(i, nucAt(length - i - 1));
            setNuc(length - i - 1, nuc);
        }
        return this;
    }


    @Override
    public IDna complement() {
        for (int i = 0; i < array.length; ++i) {
            array[i] = (byte) ~array[i];
        }
        return this;
    }

    public void setNuc(int pos, int value) {
        assert (0 <= value) && (value < 4);
        int i = pos >> sz_shift;
        int j = pos & sz_mod_mask;
        int change = ((array[i] >>> (2 * j)) & sz_mod_mask) ^ value;
        array[i] ^= (change << (2 * j));
    }

    @Override
    public String toString() {
        return DnaTools.toString(this);
    }

    @Override
    public void copyFieldsFrom(DnaWritable source) {
        length = source.length();
        int needLength = getArrayLength(length);
        adjustLength(needLength);
        System.arraycopy(source.array, 0, array, 0, needLength);
    }

    public void set(Text text) {
        length = text.getLength();
        int needLength = getArrayLength(length);
        adjustLength(needLength);

        byte[] bytes = text.getBytes();
        for (int i = 0; i < length; i++) {
            setNuc(i, DnaTools.fromChar((char)bytes[i]));
        }
    }

    private class KmerIterator implements Iterator<Kmer> {
        Kmer key = new Kmer();
        int position;
        int kmerLength;

        private KmerIterator(int kmerLength) {
            position = 0;
            this.kmerLength = kmerLength;
        }

        public void reset() {
            position = 0;
        }

        @Override
        public boolean hasNext() {
            return position <= length() - kmerLength;
        }

        @Override
        public Kmer next() {
            if (position <= length() - kmerLength) {
                key.set(KmerUtils.toLong(DnaWritable.this, position, position + kmerLength));
                position++;
                return key;
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


    @Override
    public Iterator<Kmer> kmerIterator(int kmerLength) {
        return new KmerIterator(kmerLength);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DnaWritable that = (DnaWritable) o;

        return DnaTools.equals(this, that);
    }

    @Override
    public int hashCode() {
        return DnaTools.hashCode(this);
    }
}
