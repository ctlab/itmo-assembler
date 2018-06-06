package ru.ifmo.genetics.tools.microassembly.types;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PairedBowtieAlignmentWritable implements Writable {
    private final static int FIRST_NOT_NULL = 1;
    private final static int SECOND_NOT_NULL = 2;
    private int notNullnessMask = 0;

    private BowtieAlignmentWritable first = new BowtieAlignmentWritable();
    private BowtieAlignmentWritable second = new BowtieAlignmentWritable();

    public boolean firstNotNull() {
        return (FIRST_NOT_NULL & notNullnessMask) != 0;
    }

    public boolean seconNotNull() {
        return (SECOND_NOT_NULL & notNullnessMask) != 0;
    }

    public void setNotNullness(boolean firstNotNull, boolean secondNotNull) {
        notNullnessMask = (firstNotNull ? FIRST_NOT_NULL : 0) + (secondNotNull ? SECOND_NOT_NULL : 0);
    }
    
    public BowtieAlignmentWritable first() {
        return firstNotNull() ? first : null;
    }
    
    public BowtieAlignmentWritable second() {
        return seconNotNull() ? second : null;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(notNullnessMask);
        first.write(dataOutput);
        second.write(dataOutput);
    }


    @Override
    public void readFields(DataInput dataInput) throws IOException {
        notNullnessMask = dataInput.readInt();
        first.readFields(dataInput);
        second.readFields(dataInput);
    }
}
