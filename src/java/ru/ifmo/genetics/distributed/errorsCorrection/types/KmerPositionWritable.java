package ru.ifmo.genetics.distributed.errorsCorrection.types;

import org.apache.hadoop.io.WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.Copyable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.utils.NumUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class KmerPositionWritable implements WritableComparable<KmerPositionWritable>, Copyable<KmerPositionWritable> {
    private Int128WritableComparable pairReadId = new Int128WritableComparable();
    private int leftmostPosition;
    private byte flags;
    private final static int RC_FLAG = 1;
    private final static int SECOND_IN_PAIR_FLAG = 2;

    public KmerPositionWritable() {
    }

    public KmerPositionWritable(Int128WritableComparable pairReadId, int leftmostPosition, byte flags) {
        this.pairReadId = pairReadId;
        this.leftmostPosition = leftmostPosition;
        this.flags = flags;
    }

    public KmerPositionWritable(KmerPositionWritable other) {
        copyFieldsFrom(other);
    }

    public Int128WritableComparable getPairReadId() {
        return pairReadId;
    }

    public void setPairReadId(Int128WritableComparable pairReadId) {
        this.pairReadId = pairReadId;
    }

    public void copyPairReadId(Int128WritableComparable pairReadId) {
        this.pairReadId.copyFieldsFrom(pairReadId);
    }

    public int getLeftmostPosition() {
        return leftmostPosition;
    }

    public void setLeftmostPosition(int leftmostPosition) {
        this.leftmostPosition = leftmostPosition;
    }

    public void setRcFlag() {
        flags |= RC_FLAG;
    }

    public void unsetRcFlag() {
        flags &= ~RC_FLAG;
    }

    public void setSecondInPairFlag() {
        flags |= SECOND_IN_PAIR_FLAG;
    }

    public void unsetSecondInPairFlag() {
        flags &= ~SECOND_IN_PAIR_FLAG;
    }

    public boolean isRc() {
        return (flags & RC_FLAG) != 0;
    }

    public boolean isSecondInPair() {
        return (flags & SECOND_IN_PAIR_FLAG) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KmerPositionWritable that = (KmerPositionWritable) o;

        if (flags != that.flags) return false;
        if (leftmostPosition != that.leftmostPosition) return false;
        if (pairReadId != null ? !pairReadId.equals(that.pairReadId) : that.pairReadId != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = pairReadId != null ? pairReadId.hashCode() : 0;
        result = 31 * result + leftmostPosition;
        result = 31 * result + (int) flags;
        return result;
    }

    @Override
    public int compareTo(KmerPositionWritable o) {
        int res = pairReadId.compareTo(o.pairReadId);
        if (res != 0)
            return res;
        res = NumUtils.compare(leftmostPosition, o.leftmostPosition);
        if (res != 0)
            return res;
        return NumUtils.compare(flags, o.flags);
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        pairReadId.write(dataOutput);
        dataOutput.writeInt(leftmostPosition);
        dataOutput.writeByte(flags);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        pairReadId.readFields(dataInput);
        leftmostPosition = dataInput.readInt();
        flags = dataInput.readByte();
    }

    @Override
    public String toString() {
        return "KmerPositionWritable{" +
                "pairReadId=" + pairReadId +
                ", leftmostPosition=" + leftmostPosition +
                ", flags=" + flags +
                '}';
    }

    @Override
    public void copyFieldsFrom(KmerPositionWritable source) {
        pairReadId.copyFieldsFrom(source.pairReadId);
        leftmostPosition = source.leftmostPosition;
        flags = source.flags;
    }
}
