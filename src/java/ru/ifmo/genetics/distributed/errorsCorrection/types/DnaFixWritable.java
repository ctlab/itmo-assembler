package ru.ifmo.genetics.distributed.errorsCorrection.types;

import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.distributed.io.writable.Copyable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class DnaFixWritable implements Writable, Copyable<DnaFixWritable> {
    public int nucPosition;
    public byte newNuc;

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(nucPosition);
        out.writeByte(newNuc);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        nucPosition = in.readInt();
        newNuc = in.readByte();
    }

    @Override
    public String toString() {
        return "DnaFixWritable{" +
                "nucPosition=" + nucPosition +
                ", newNuc=" + newNuc +
                '}';
    }

    @Override
    public void copyFieldsFrom(DnaFixWritable source) {
        nucPosition = source.nucPosition;
        newNuc = source.newNuc;
    }
}
