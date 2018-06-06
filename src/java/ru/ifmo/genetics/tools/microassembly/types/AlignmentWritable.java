package ru.ifmo.genetics.tools.microassembly.types;

import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.utils.TextUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class AlignmentWritable implements Writable {
    public int contigId;
    public boolean onForwardStrand;
    public int offset; // 0-based leftmost aligned nucleotide position

    public void copyFieldsFrom(AlignmentWritable alignment) {
        contigId = alignment.contigId;
        onForwardStrand = alignment.onForwardStrand;
        offset = alignment.offset;
    }

    public void copyFieldsFrom(BowtieAlignmentWritable alignment) {
        contigId = TextUtils.parseInt(alignment.contigId);
        onForwardStrand = alignment.onForwardStrand;
        offset = alignment.offset;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(contigId);
        dataOutput.writeBoolean(onForwardStrand);
        dataOutput.writeInt(offset);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        contigId = dataInput.readInt();
        onForwardStrand = dataInput.readBoolean();
        offset = dataInput.readInt();
    }

    public void reverseComplement(int contigLength, int readLength) {
        onForwardStrand = !onForwardStrand;
        offset = contigLength - offset - readLength;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlignmentWritable that = (AlignmentWritable) o;

        if (contigId != that.contigId) return false;
        if (offset != that.offset) return false;
        if (onForwardStrand != that.onForwardStrand) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = contigId;
        result = 31 * result + (onForwardStrand ? 1 : 0);
        result = 31 * result + offset;
        return result;
    }

    @Override
    public String toString() {
        return "AlignmentWritable{" +
                "contigId=" + contigId +
                ", onForwardStrand=" + onForwardStrand +
                ", offset=" + offset +
                '}';
    }
}
