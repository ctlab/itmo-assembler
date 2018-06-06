package ru.ifmo.genetics.distributed.contigsJoining.types;

import org.apache.hadoop.io.WritableComparable;
import ru.ifmo.genetics.tools.microassembly.types.PairedMaybeAlignedDnaQWritable;
import ru.ifmo.genetics.utils.Misc;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Hole implements WritableComparable<Hole> {
    public boolean leftComplemented;
    public int leftContigId;
    public boolean rightComplemented;
    public int rightContigId;

    public static final int NONEXISTENT_CONTIG_ID = Integer.MAX_VALUE;

    public void set(int leftContigId, boolean leftComplemented,
                    int rightContigId, boolean rightComplemented) {
        if (leftContigId > rightContigId) {
            this.rightContigId = leftContigId;
            this.rightComplemented = !leftComplemented;
            this.leftContigId = rightContigId;
            this.leftComplemented = !rightComplemented;
        } else {
            this.leftContigId = leftContigId;
            this.leftComplemented = leftComplemented;
            this.rightContigId = rightContigId;
            this.rightComplemented = rightComplemented;
        }
    }

    public void setOpen(int leftContigId, boolean leftComplemented) {
        set(leftContigId, leftComplemented, NONEXISTENT_CONTIG_ID, false);
    }

    public boolean isOpen() {
        return rightContigId == NONEXISTENT_CONTIG_ID;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(leftComplemented);
        out.writeInt(leftContigId);
        out.writeBoolean(rightComplemented);
        out.writeInt(rightContigId);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        leftComplemented = in.readBoolean();
        leftContigId = in.readInt();
        rightComplemented = in.readBoolean();
        rightContigId = in.readInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Hole hole = (Hole) o;

        if (leftComplemented != hole.leftComplemented) return false;
        if (leftContigId != hole.leftContigId) return false;
        if (rightComplemented != hole.rightComplemented) return false;
        if (rightContigId != hole.rightContigId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (leftComplemented ? 1 : 0);
        result = 31 * result + leftContigId;
        result = 31 * result + (rightComplemented ? 1 : 0);
        result = 31 * result + rightContigId;
        return result;
    }

    @Override
    public String toString() {
        return  leftContigId +
                (leftComplemented ? "rc" : "") +
                " -> " +
                rightContigId +
                (rightComplemented ? "rc" : "");
    }

    public void set(PairedMaybeAlignedDnaQWritable value) {
        assert value.first.isAligned && value.second.isAligned;
        set(value.first.alignment.contigId, !value.first.alignment.onForwardStrand,
            value.second.alignment.contigId, value.second.alignment.onForwardStrand);
    }


    public void copyFieldsFrom(Hole other) {
        leftComplemented = other.leftComplemented;
        leftContigId = other.leftContigId;
        rightComplemented = other.rightComplemented;
        rightContigId = other.rightContigId;
    }

    @Override
    public int compareTo(Hole o) {
        int res;
        assert leftContigId >= 0;
        assert o.leftContigId >= 0;
        assert rightContigId >= 0;
        assert o.rightContigId >= 0;

        res = leftContigId - o.leftContigId;
        if (res != 0) {
            return res;
        }

        res = rightContigId - o.rightContigId;
        if (res != 0) {
            return res;
        }

        res = Misc.compare(leftComplemented, o.leftComplemented);
        if (res != 0) {
            return res;
        }

        res = Misc.compare(rightComplemented, o.rightComplemented);
        return res;

    }

    public void parse(String holeSting) {
        String[] holeParts = holeSting.split(" ");
        String from = holeParts[0];
        String to = holeParts[2];
        int iFrom = stringIdToInt(from);
        int iTo = stringIdToInt(to);
        if (iFrom == NONEXISTENT_CONTIG_ID) {
            leftContigId = iFrom;
            leftComplemented = false;
        } else {
            leftContigId = iFrom / 2;
            leftComplemented = (iFrom & 1) == 1;
        }

        if (iTo == NONEXISTENT_CONTIG_ID) {
            rightContigId = iTo;
            rightComplemented = false;
        } else {
            rightContigId = iTo / 2;
            rightComplemented = (iTo & 1) == 1;
        }
    }

    public static int stringIdToInt(String id) {
        if (id.equals("" + Integer.MAX_VALUE)) {
            return Integer.MAX_VALUE;
        }
        if (id.endsWith("rc")) {
            return Integer.parseInt(id.substring(0, id.length() - 2)) * 2 + 1;
        } else {
            return Integer.parseInt(id) * 2;
        }
    }

}

