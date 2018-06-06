package ru.ifmo.genetics.distributed.errorsCorrection.types;

import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.Union2WritableComparable;

public class PairedDnaQOrFix extends Union2WritableComparable<PairedDnaQWritable, PairedDnaFixWritable> {

    public PairedDnaQOrFix() {
        this((byte)0);
    }

    public PairedDnaQOrFix(byte type) {
        super(new PairedDnaQWritable(), new PairedDnaFixWritable(), type);
    }
}
