package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class AbstractUnionWritable {
    public abstract byte getType();

    protected abstract void setType(byte type);

    public abstract Writable getValue();

    public void write(DataOutput out) throws IOException {
        out.writeByte(getType());
        getValue().write(out);
    }

    public void readFields(DataInput in) throws IOException {
        setType(in.readByte());
        getValue().readFields(in);
    }
}
