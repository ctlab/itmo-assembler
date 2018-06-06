package ru.ifmo.genetics.tools.rf.task;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.io.writers.DedicatedWriter;
import ru.ifmo.genetics.statistics.reporter.IgnoringReporter;
import ru.ifmo.genetics.statistics.reporter.Reporter;
import ru.ifmo.genetics.structures.debriujn.DeBruijnGraph;
import ru.ifmo.genetics.tools.rf.Orientation;
import ru.ifmo.genetics.utils.pairs.UniPair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class GlobalContext {
    final DedicatedWriter<LightDna> dnaWriter;
    final Queue<List<UniPair<DnaQ>>> failedQueue;
    final int k;
    
    final int maxFragmentSize; // = averageFragmentSize + fragmentSizeDeviation;
    final int minFragmentSize; // = averageFragmentSize - fragmentSizeDeviation;
    
    final long toVertexMask;
    final int dnaBuilderCapacity;
    
    final DeBruijnGraph graph;

    final List<Orientation> orientationsToCheck;

    final Reporter<FillingReport> reporter;

    public final AtomicLong sumOutput = new AtomicLong();
    public final long maxSumOutput;


    public GlobalContext(DedicatedWriter<LightDna> dnaWriter, Queue<List<UniPair<DnaQ>>> failedQueue,
                         int k, int minFragmentSize,
                         int maxFragmentSize, DeBruijnGraph graph) {
        this(dnaWriter, failedQueue, k, minFragmentSize, maxFragmentSize, graph, Arrays.asList(Orientation.FR),
                0, Long.MAX_VALUE, new IgnoringReporter<FillingReport>());
    }

    public GlobalContext(DedicatedWriter<LightDna> dnaWriter, Queue<List<UniPair<DnaQ>>> failedQueue,
                         int k, int minFragmentSize,
            int maxFragmentSize, DeBruijnGraph graph,
            List<Orientation> orientationsToCheck, long curSumOutput, long maxSumOutput,
            Reporter<FillingReport> reporter
            ) {
        this.dnaWriter = dnaWriter;
        this.failedQueue = failedQueue;
        this.k = k;
        this.minFragmentSize = minFragmentSize;
        this.maxFragmentSize = maxFragmentSize;
        this.graph = graph;
        this.orientationsToCheck = new ArrayList<Orientation>(orientationsToCheck);
        sumOutput.addAndGet(curSumOutput);
        this.maxSumOutput = maxSumOutput;

        toVertexMask = (1L << (2 * k)) - 1;
        dnaBuilderCapacity = maxFragmentSize;
        this.reporter = reporter;
    }
}
