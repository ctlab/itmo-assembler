package ru.ifmo.genetics.tools.microassembly.types;

import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.distributed.io.writable.Copyable;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class MaybeAlignedDnaQWritable implements Writable, Copyable<MaybeAlignedDnaQWritable> {
    public DnaQWritable dnaq = new DnaQWritable();
    public boolean isAligned = false;
    public AlignmentWritable alignment = new AlignmentWritable();


    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dnaq.write(dataOutput);
        dataOutput.writeBoolean(isAligned);
        if (isAligned) {
            alignment.write(dataOutput);
        }
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        dnaq.readFields(dataInput);
        isAligned = dataInput.readBoolean();
        if (isAligned) {
            alignment.readFields(dataInput);
        }
    }

    @Override
    public void copyFieldsFrom(MaybeAlignedDnaQWritable source) {
        dnaq.copyFieldsFrom(source.dnaq);
        isAligned = source.isAligned;
        if (isAligned) {
            alignment.copyFieldsFrom(source.alignment);
        }
    }

    public void reverseComplement(int contigLength) {
        alignment.reverseComplement(contigLength, dnaq.length());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MaybeAlignedDnaQWritable that = (MaybeAlignedDnaQWritable) o;

        if (isAligned != that.isAligned) return false;
        if (alignment != null ? !alignment.equals(that.alignment) : that.alignment != null)
            return false;
        if (dnaq != null ? !dnaq.equals(that.dnaq) : that.dnaq != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = dnaq != null ? dnaq.hashCode() : 0;
        result = 31 * result + (isAligned ? 1 : 0);
        result = 31 * result + (alignment != null ? alignment.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MaybeAlignedDnaQWritable{" +
                "dnaq=" + dnaq +
                ", isAligned=" + isAligned +
                ( isAligned ? ", alignment=" + alignment : "" ) +
                '}';
    }
}
