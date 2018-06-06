package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import ru.ifmo.genetics.distributed.clusterization.types.Kmer;
import ru.ifmo.genetics.distributed.io.KmerIterable;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.utils.KmerUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class PairedDnaQWritable implements WritableComparable<PairedDnaQWritable>, KmerIterable {
    public DnaQWritable first;
    public DnaQWritable second;

    public PairedDnaQWritable() {
        this(new DnaQWritable(), new DnaQWritable());
    }

    public PairedDnaQWritable(DnaQWritable first, DnaQWritable second) {
        this.first = first;
        this.second = second;
    }

    public void reverseComplement() {
        DnaQWritable t = second;
        second = first;
        first = t;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        first.write(dataOutput);
        second.write(dataOutput);
    }


    @Override
    public void readFields(DataInput dataInput) throws IOException {
        first.readFields(dataInput);
        second.readFields(dataInput);
    }


    public void set(Text nucleotide1, Text quality1, Text nucleotide2, Text quality2, QualityFormat qf) {
        first.set(nucleotide1, quality1, qf);
        second.set(nucleotide2, quality2, qf);
    }

    @Override
    public String toString() {
        return "PairedDnaQWritable{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PairedDnaQWritable that = (PairedDnaQWritable) o;

        if (first != null ? !first.equals(that.first) : that.first != null) return false;
        if (second != null ? !second.equals(that.second) : that.second != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(PairedDnaQWritable o) {
        int res = first.compareTo(o.first);
        if (res != 0)
            return res;
        return second.compareTo(o.second);
    }

    public void updateDigest(MessageDigest md) {
        first.updateDigest(md);
        second.updateDigest(md);
    }

    public void setFieldsFrom(PairedDnaQWritable next) {
        first.copyFieldsFrom(next.first);
        second.copyFieldsFrom(next.second);
    }

    private class KmerIterator implements Iterator<Kmer> {
        Kmer key = new Kmer();
        boolean isFirst;
        int position;
        int kmerLength;

        private KmerIterator(int kmerLength) {
            isFirst = true;
            position = 0;
            this.kmerLength = kmerLength;
        }

        @Override
        public boolean hasNext() {
            return (isFirst && kmerLength <= second.length())|| (!isFirst && position <= second.length() - kmerLength);
        }

        @Override
        public Kmer next() {
            if (isFirst) {
                if (position <= first.length() - kmerLength) {
                    key.set(KmerUtils.toLong(first, position, position + kmerLength));
                    position++;
                    return key;
                } else {
                    isFirst = false;
                    position = 0;
                }
            }
            if (position <= second.length() - kmerLength) {
                key.set(KmerUtils.toLong(second, position, position + kmerLength));
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
}
