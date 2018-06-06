package ru.ifmo.genetics.distributed.clusterization.types;

import org.apache.hadoop.io.WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class UndirectUnweightEdge implements WritableComparable<UndirectUnweightEdge> {
    public Int128WritableComparable first = new Int128WritableComparable();
    public Int128WritableComparable second = new Int128WritableComparable();

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

    @Override
    public int compareTo(UndirectUnweightEdge o) {
        int x = first.compareTo(o.first);
        if (x != 0) {
            return x;
        }
        return  second.compareTo(o.second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UndirectUnweightEdge undirectUnweightEdge = (UndirectUnweightEdge) o;

        if (first != undirectUnweightEdge.first) return false;
        if (second != undirectUnweightEdge.second) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = first.hashCode() + 31 * second.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "UndirectUnweightEdge{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
