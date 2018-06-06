package ru.ifmo.genetics.distributed.contigsJoining.types;

import org.apache.hadoop.io.WritableComparable;
import ru.ifmo.genetics.utils.Misc;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


public class ContigEnd implements WritableComparable<ContigEnd>{
    public int contigId;
    public boolean rightEnd;

    public void set(int contigId, boolean rightEnd) {
        this.contigId = contigId;
        this.rightEnd = rightEnd;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(contigId);
        out.writeBoolean(rightEnd);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        contigId = in.readInt();
        rightEnd = in.readBoolean();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContigEnd contigEnd = (ContigEnd) o;

        if (contigId != contigEnd.contigId) return false;
        if (rightEnd != contigEnd.rightEnd) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = contigId;
        result = 31 * result + (rightEnd ? 1 : 0);
        return result;
    }

    @Override
    public int compareTo(ContigEnd o) {
        int res = contigId - o.contigId;
        if (res != 0)
            return res;
        return Misc.compare(rightEnd, o.rightEnd);
    }

    @Override
    public String toString() {
        return "ContigEnd{" +
                "contigId=" + contigId +
                ", rightEnd=" + rightEnd +
                '}';
    }
}
