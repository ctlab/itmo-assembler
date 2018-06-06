package ru.ifmo.genetics.distributed.contigsJoining.types;

import ru.ifmo.genetics.distributed.io.writable.Union2Writable;
import ru.ifmo.genetics.tools.microassembly.types.PairedMaybeAlignedDnaQWritable;

public class ContigOrAlignment extends Union2Writable<Contig, PairedMaybeAlignedDnaQWritable> {
    public ContigOrAlignment() {
        this(new Contig(), new PairedMaybeAlignedDnaQWritable(), (byte)0);
    }
    public ContigOrAlignment(Contig first, PairedMaybeAlignedDnaQWritable second, byte type) {
        super(first, second, type);
    }

    public ContigOrAlignment(Contig first) {
        this(first, new PairedMaybeAlignedDnaQWritable(), (byte)0);
    }

    public ContigOrAlignment(PairedMaybeAlignedDnaQWritable align) {
        this(new Contig(), align, (byte)0);
    }
}

