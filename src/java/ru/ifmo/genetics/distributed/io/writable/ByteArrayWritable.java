package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.security.MessageDigest;

public class ByteArrayWritable implements Writable {
    private byte[] value = new byte[0];
    private int length;

    public int length() {
        return length;
    }

    public void reset(int length) {
        if (value == null || value.length < length) {
            value = new byte[length];
        }
        this.length = length;
    }

    private boolean goodIndex(int i) {
        return 0 <= i && i < length;
    }

    public byte get(int i) {
        assert goodIndex(i) : i;
        return value[i];
    }

    public void set(int i, byte b) {
        assert goodIndex(i) : i;
        value[i] = b;
    }
    
    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(length);
        if (length > 0) {
            dataOutput.write(value, 0, length);
        }
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        length = dataInput.readInt();
        reset(length);
        if (length > 0) {
            dataInput.readFully(value, 0, length);
        }
    }

    public void updateDigest(MessageDigest md) {
        md.update(value, 0, length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ByteArrayWritable that = (ByteArrayWritable) o;

        if (length != that.length) return false;
        for (int i = 0; i < length; ++i) {
            if (value[i] != that.value[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = length;
        if (value != null) {
            for (int i = 0; i < length; ++i) {
                result = 31 * result + value[i];
            }
        }
        return result;
    }

    public void clear() {
        length = 0;
    }
}
