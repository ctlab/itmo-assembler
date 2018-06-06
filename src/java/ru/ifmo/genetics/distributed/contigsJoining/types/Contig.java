package ru.ifmo.genetics.distributed.contigsJoining.types;

import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.distributed.io.writable.Copyable;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Contig implements Writable, Copyable<Contig>{
    public int id;
    public DnaQWritable sequence = new DnaQWritable();

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(id);
        sequence.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        id = in.readInt();
        sequence.readFields(in);
    }

    @Override
    public void copyFieldsFrom(Contig source) {
        id = source.id;
        sequence.copyFieldsFrom(source.sequence);
    }
}
