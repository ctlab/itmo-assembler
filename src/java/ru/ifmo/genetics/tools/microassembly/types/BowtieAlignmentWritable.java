package ru.ifmo.genetics.tools.microassembly.types;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.io.formats.Sanger;
import ru.ifmo.genetics.utils.TextUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class BowtieAlignmentWritable implements Writable {
    public Text readId = new Text();
    public boolean onForwardStrand;
    public Text contigId = new Text();
    public int offset;
    public DnaQWritable sequence = new DnaQWritable();
    public int magic; // I don't understand what this field means
    public Text mismatches = new Text();

    private static int getColumnEnd(byte[] bytes, int length, int columntStart) {
        for (; columntStart < length && bytes[columntStart] != '\t'; ++columntStart)
            ;
        return columntStart;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BowtieAlignmentWritable that = (BowtieAlignmentWritable) o;

        if (magic != that.magic) return false;
        if (offset != that.offset) return false;
        if (onForwardStrand != that.onForwardStrand) return false;
        if (contigId != null ? !contigId.equals(that.contigId) : that.contigId != null)
            return false;
        if (mismatches != null ? !mismatches.equals(that.mismatches) : that.mismatches != null)
            return false;
        if (readId != null ? !readId.equals(that.readId) : that.readId != null)
            return false;
        if (sequence != null ? !sequence.equals(that.sequence) : that.sequence != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = readId != null ? readId.hashCode() : 0;
        result = 31 * result + (onForwardStrand ? 1 : 0);
        result = 31 * result + (contigId != null ? contigId.hashCode() : 0);
        result = 31 * result + offset;
        result = 31 * result + (sequence != null ? sequence.hashCode() : 0);
        result = 31 * result + magic;
        result = 31 * result + (mismatches != null ? mismatches.hashCode() : 0);
        return result;
    }

    public void parseFromLine(Text line, QualityFormat qf) {
        try {
            byte[] bytes = line.getBytes();
            int len = line.getLength();

            // 1.
            int columnStart = 0;
            int columnEnd = getColumnEnd(bytes, len, columnStart);
            readId.set(bytes, columnStart, columnEnd - columnStart);

            // 2.
            columnStart = columnEnd + 1;
            columnEnd = getColumnEnd(bytes, len, columnStart);
            assert columnEnd == columnStart + 1 : columnStart + " " + columnEnd;
            byte b = bytes[columnStart];
            assert b == '+' || b == '-' : bytes[columnStart];
            onForwardStrand = (b == '+');

            // 3.
            columnStart = columnEnd + 1;
            columnEnd = getColumnEnd(bytes, len, columnStart);
            contigId.set(bytes, columnStart, columnEnd - columnStart);

            // 4.
            columnStart = columnEnd + 1;
            columnEnd = getColumnEnd(bytes, len, columnStart);
            offset = TextUtils.parseInt(bytes, columnStart, columnEnd);

            // 5.
            columnStart = columnEnd + 1;
            columnEnd = getColumnEnd(bytes, len, columnStart);
            sequence.set(bytes, columnStart, bytes, columnEnd + 1, columnEnd - columnStart, qf);

            // 6.
            int expectedColumnEnd = columnEnd + 1 + (columnEnd - columnStart);
            columnStart = columnEnd + 1;
            columnEnd = getColumnEnd(bytes, len, columnStart);
            assert columnEnd == expectedColumnEnd;

            // 7.
            columnStart = columnEnd + 1;
            columnEnd = getColumnEnd(bytes, len, columnStart);
            magic = TextUtils.parseInt(bytes, columnStart, columnEnd);

            // 8.
            columnStart = columnEnd + 1;
            columnEnd = getColumnEnd(bytes, len, columnStart);
            mismatches.set(bytes, columnStart, columnEnd - columnStart);
            assert columnEnd == len;
        } catch (AssertionError e) {
            throw new AssertionError(e.getMessage() + "\nin line:\n" + line);
        }
    }
    
    
    @Override
    public void write(DataOutput out) throws IOException {
        readId.write(out);
        out.writeBoolean(onForwardStrand);
        contigId.write(out);
        out.writeInt(offset);
        sequence.write(out);
        out.writeInt(magic);
        mismatches.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        readId.readFields(in);
        onForwardStrand = in.readBoolean();
        contigId.readFields(in);
        offset = in.readInt();
        sequence.readFields(in);
        magic = in.readInt();
        mismatches.readFields(in);
    }

    @Override
    public String toString() {
        return "BowtieAlignmentWritable{" +
                "readId=" + readId +
                ", onForwardStrand=" + onForwardStrand +
                ", contigId=" + contigId +
                ", offset=" + offset +
                ", sequence=" + sequence +
                ", magic=" + magic +
                ", mismatches=" + mismatches +
                '}';
    }
}
