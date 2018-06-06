package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.utils.pairs.Pair;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PairWritable<A extends Writable, B extends Writable> implements Writable, Pair<A, B> {
    public A first;
    public B second;

    
    public PairWritable(A first, B second) {
        this.first = first;
        this.second = second;
    }
    
    public A first() {
        return first;
    }
    
    public B second() {
        return second;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PairWritable that = (PairWritable) o;

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
    public void write(DataOutput dataOutput) throws IOException {
        first.write(dataOutput);
        second.write(dataOutput);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        first.readFields(dataInput);
//        System.err.println("read key: " + first);
        second.readFields(dataInput);
//        System.err.println("read value: " + second);
    }

    @Override
    public String toString() {
        return "PairWritable{" +
                "second=" + second +
                ", first=" + first +
                '}';
    }
}
