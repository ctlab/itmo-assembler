package ru.ifmo.genetics.distributed.errorsCorrection.types;

import ru.ifmo.genetics.distributed.io.writable.Union2Writable;

public class KmerPositionOrFix extends Union2Writable<KmerPositionWritable, DnaFixWritable> {
    public KmerPositionOrFix() {
        this(new KmerPositionWritable(), new DnaFixWritable());
    }

    public KmerPositionOrFix(KmerPositionWritable first, DnaFixWritable second) {
        super(first, second, (byte)0);
    }

    public KmerPositionOrFix(KmerPositionWritable first, DnaFixWritable second, byte type) {
        super(first, second, type);
    }
}
