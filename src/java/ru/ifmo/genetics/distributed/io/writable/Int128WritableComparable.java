package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.WritableComparable;
import ru.ifmo.genetics.utils.NumUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Int128WritableComparable implements WritableComparable/*<Int128WritableComparable>*/, Copyable<Int128WritableComparable> {
    public long hiLong;
    public long loLong;

    public Int128WritableComparable() {
    }

    public Int128WritableComparable(long loLong) {
        this.loLong = loLong;
        this.hiLong = 0;
    }

    public Int128WritableComparable(long loLong, long hiLong) {
        this.loLong = loLong;
        this.hiLong = hiLong;
    }

    public Int128WritableComparable(Int128WritableComparable value) {
        this.hiLong = value.hiLong;
        this.loLong = value.loLong;
    }

    public void set(byte[] bytes) {
        assert bytes.length == 16;
        hiLong = bytesToLong(bytes, 0);
        loLong = bytesToLong(bytes, 8);

    }

    private static long bytesToLong(byte[] bytes, int off) {
        long res = ((long) bytes[off] << 56L) +
                ((long) bytes[off + 1] << 48L) +
                ((long) bytes[off + 2] << 48L) +
                ((long) bytes[off + 3] << 32L) +
                ((long) bytes[off + 4] << 24L) +
                ((long) bytes[off + 5] << 16L) +
                ((long) bytes[off + 6] << 8L) +
                ((long) bytes[off + 7]);
        return res;
    }

    @Override
    public int compareTo(Object other) {
        Int128WritableComparable o = (Int128WritableComparable) other;
        int res = NumUtils.compare(hiLong, o.hiLong);
        if (res != 0) {
            return res;
        }
        return NumUtils.compare(loLong, o.loLong);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(hiLong);
        out.writeLong(loLong);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        hiLong = in.readLong();
        loLong = in.readLong();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Int128WritableComparable that = (Int128WritableComparable) o;

        if (hiLong != that.hiLong) return false;
        if (loLong != that.loLong) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (hiLong ^ (hiLong >>> 32));
        result = 31 * result + (int) (loLong ^ (loLong >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("%016x%016x", hiLong, loLong);
    }

    @Override
    public void copyFieldsFrom(Int128WritableComparable pairReadId) {
        hiLong = pairReadId.hiLong;
        loLong = pairReadId.loLong;
    }
}
