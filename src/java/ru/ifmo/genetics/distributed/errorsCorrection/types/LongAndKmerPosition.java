package ru.ifmo.genetics.distributed.errorsCorrection.types;

import org.apache.hadoop.io.LongWritable;
import ru.ifmo.genetics.distributed.io.writable.PairWritable;

public class LongAndKmerPosition extends PairWritable<LongWritable, KmerPositionWritable> {
    public LongAndKmerPosition() {
        this(new LongWritable(), new KmerPositionWritable());
    }

    public LongAndKmerPosition(LongWritable first, KmerPositionWritable second) {
        super(first, second);
    }
}
