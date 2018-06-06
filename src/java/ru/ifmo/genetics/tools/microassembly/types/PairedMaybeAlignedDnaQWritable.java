package ru.ifmo.genetics.tools.microassembly.types;

import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.distributed.io.writable.Copyable;
import ru.ifmo.genetics.distributed.util.PublicCloneable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PairedMaybeAlignedDnaQWritable implements Writable, PublicCloneable<PairedMaybeAlignedDnaQWritable>,Copyable<PairedMaybeAlignedDnaQWritable> {
    public MaybeAlignedDnaQWritable first = new MaybeAlignedDnaQWritable();
    public MaybeAlignedDnaQWritable second = new MaybeAlignedDnaQWritable();

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
    public void copyFieldsFrom(PairedMaybeAlignedDnaQWritable source) {
        first.copyFieldsFrom(source.first);
        second.copyFieldsFrom(source.second);
    }

    @Override
    public PairedMaybeAlignedDnaQWritable publicClone() {
        PairedMaybeAlignedDnaQWritable res = new PairedMaybeAlignedDnaQWritable();
        res.copyFieldsFrom(this);
        return res;
    }

    public void reverseComplement() {
        MaybeAlignedDnaQWritable t = first;
        first = second;
        second = t;
    }

    @Override
    public String toString() {
        return "PairedMaybeAlignedDnaQWritable{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
