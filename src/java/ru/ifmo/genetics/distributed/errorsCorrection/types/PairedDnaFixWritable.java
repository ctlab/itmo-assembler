package ru.ifmo.genetics.distributed.errorsCorrection.types;

import org.apache.hadoop.io.WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.Copyable;
import ru.ifmo.genetics.distributed.util.PublicCloneable;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.NumUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PairedDnaFixWritable implements WritableComparable<PairedDnaFixWritable>,PublicCloneable<PairedDnaFixWritable>,Copyable<PairedDnaFixWritable> {
    public boolean isSecondInPair;
    private DnaFixWritable dnaFix = new DnaFixWritable();

    public void setNucPosition(int p) {
        dnaFix.nucPosition = p;
    }
    
    public void setNewNuc(byte n) {
        dnaFix.newNuc = n;
    }
    
    public int nucPosition() {
        return dnaFix.nucPosition;
    }

    public byte newNuc() {
        return dnaFix.newNuc;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(isSecondInPair);
        dnaFix.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        isSecondInPair = in.readBoolean();
        dnaFix.readFields(in);
    }

    @Override
    public int compareTo(PairedDnaFixWritable o) {
        int res;
        res = Misc.compare(isSecondInPair, o.isSecondInPair);
        if (res != 0)
            return res;
        res = NumUtils.compare(dnaFix.nucPosition, o.dnaFix.nucPosition);
        if (res != 0)
            return res;
        return NumUtils.compare(dnaFix.newNuc, o.dnaFix.newNuc);
    }

    @Override
    public void copyFieldsFrom(PairedDnaFixWritable source) {
        dnaFix.copyFieldsFrom(source.dnaFix);
        isSecondInPair = source.isSecondInPair;
    }

    @Override
    public PairedDnaFixWritable publicClone() {
        
        PairedDnaFixWritable res = new PairedDnaFixWritable();
        res.copyFieldsFrom(this);
        return res;
    }
    
    @Override
    public String toString() {
        return isSecondInPair + " " + dnaFix;
    }

}

