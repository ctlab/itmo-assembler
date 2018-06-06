package ru.ifmo.genetics.tools.olc.overlapper;

import ru.ifmo.genetics.io.writers.BufferDedicatedWriter;
import ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString;
import ru.ifmo.genetics.tools.olc.overlaps.Overlaps;
import ru.ifmo.genetics.tools.olc.suffixArray.SuffixArray;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public class OverlapTaskContext {

    /**
     * String with all reads, formed as '$read1$read2$...$readN$'.
     */
    public final GluedDnasString fullString;

    /**
     * Sorted reads.
     */
    public final ArrayList<Integer> readsInBucket;

    /**
     * <code>readBegin[i]</code> = position in <code>fullString</code> where read number <code>i</code> begins.
     */
    public final long[] readBegin;
    public final int readsNumber;
    public final int realReadsNumber;   // not doubled

    public final SuffixArray sa;

    public final Executor executor;

    // for outputting
    public final BufferDedicatedWriter writer;
    public final Overlaps overlaps;

    // overlap algorithm params
    public final int maxNumberOfErrors;
    public final int errorsWindowSize;
    public final int minOverlap;

    // output value
    public AtomicLong foundOverlaps = new AtomicLong();


    public OverlapTaskContext(GluedDnasString fullString,
                              ArrayList<Integer> readsInBucket,
                              long[] readBegin,
                              int readsNumber,
                              int realReadsNumber,
                              SuffixArray sa,
                              Executor executor,
                              BufferDedicatedWriter writer,
                              Overlaps overlaps,
                              int maxNumberOfErrors,
                              int errorsWindowSize,
                              int minOverlap) {

        this.fullString = fullString;
        this.readsInBucket = readsInBucket;
        this.readBegin = readBegin;
        this.readsNumber = readsNumber;
        this.realReadsNumber = realReadsNumber;
        this.sa = sa;
        this.executor = executor;
        this.writer = writer;
        this.overlaps = overlaps;
        this.maxNumberOfErrors = maxNumberOfErrors;
        this.errorsWindowSize = errorsWindowSize;
        this.minOverlap = minOverlap;
    }

    @Override
    public String toString() {
        return "OverlapTaskContext{" +
                "\n\tfullString=" + fullString +
                ", \n\treadsInBucket=" + readsInBucket.getClass() +
                ", \n\treadBegin=" + readBegin +
                ", \n\treadsNumber=" + readsNumber +
                ", \n\trealReadsNumber=" + realReadsNumber +
                ", \n\tsa=" + sa +
                ", \n\texecutor=" + executor +
                ", \n\twriter=" + writer +
                ", \n\toverlaps=" + overlaps +
                ", \n\tmaxNumberOfErrors=" + maxNumberOfErrors +
                ", \n\terrorsWindowSize=" + errorsWindowSize +
                ", \n\tminOverlap=" + minOverlap +
                '}';
    }
}
