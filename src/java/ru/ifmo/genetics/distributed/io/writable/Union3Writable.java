package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Union3Writable
        <A extends Writable,
                B extends Writable,
                C extends Writable>
        extends AbstractUnionWritable implements Writable {

    protected A first;
    protected B second;
    protected C third;
    protected byte type;

    public byte getType() {
        return type;
    }

    public boolean isFirst() {
        return type == 0;
    }

    public boolean isSecond() {
        return type == 1;
    }

    public boolean isThird() {
        return type == 2;
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

    public C getThird() {
        assert isThird();
        return third;
    }

    public void setFirst(A first) {
        this.first = first;
        type = (byte) 0;
    }

    public void setSecond(B second) {
        this.second = second;
        type = (byte) 1;
    }

    public void setThird(C third) {
        this.third = third;
        type = (byte) 2;
    }

    public Writable getValue() {
        switch (type) {
            case 0:
                return first;
            case 1:
                return second;
            case 2:
                return third;
            default:
                throw new RuntimeException("Unexpected type " + type);
        }
    }

    protected Union3Writable(A first, B second, C third, byte type) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Union3Writable{" +
                (type == 0 ? ("first=" + first) : (type == 1 ? ("second=" + second) : ("third=" + third))) + "}";
    }
}
