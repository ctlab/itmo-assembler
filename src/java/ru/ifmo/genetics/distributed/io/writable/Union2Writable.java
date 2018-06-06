package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Union2Writable
        <A extends Writable,
                B extends Writable>
        extends AbstractUnionWritable implements Writable {
    protected A first;
    protected B second;
    protected byte type;

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public A getFirst() {
        assert isFirst();
        return first;
    }

    public B getSecond() {
        assert isSecond();
        return second;
    }

    public boolean isFirst() {
        return type == 0;
    }

    public boolean isSecond() {
        return type == 1;
    }

    public void setFirst(A first) {
        this.first = first;
        type = (byte) 0;
    }

    public void setSecond(B second) {
        this.second = second;
        type = (byte) 1;
    }

    public Writable getValue() {
        switch (type) {
            case 0:
                return first;
            case 1:
                return second;
            default:
                throw new RuntimeException("Unexpected type " + type);
        }
    }

    protected Union2Writable(A first, B second, byte type) {
        this.first = first;
        this.second = second;
        this.type = type;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(type);
        getValue().write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        type = in.readByte();
        getValue().readFields(in);
    }

    @Override
    public String toString() {
        return "Union2Writable{" +
                (type == 0 ? "first=" : "second=") + getValue() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Union2Writable)) {
            return false;
        }

        Union2Writable that = (Union2Writable) o;

        if (type != that.type) {
            return false;
        }
        if (type == 0) {
            return first.equals(that.first);
        } else {
            return second.equals(that.second);
        }
    }

    @Override
    public int hashCode() {
        if (type == 0) {
            return first.hashCode();
        } else {
            return second.hashCode();
        }
    }
}
