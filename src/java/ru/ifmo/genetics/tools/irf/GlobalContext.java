package ru.ifmo.genetics.tools.irf;

import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.io.writers.AbstractDedicatedWriter;
import ru.ifmo.genetics.io.writers.DedicatedWriter;
import ru.ifmo.genetics.statistics.reporter.IgnoringReporter;
import ru.ifmo.genetics.statistics.reporter.Reporter;
import ru.ifmo.genetics.structures.debriujn.WeightedDeBruijnGraph;
import ru.ifmo.genetics.tools.rf.Orientation;

import java.io.File;
import java.util.List;

public class GlobalContext {
    public final int k;
    public final WeightedDeBruijnGraph graph;

    public final int minFragmentSize; // = averageFragmentSize - fragmentSizeDeviation;
    public final int maxFragmentSize; // = averageFragmentSize + fragmentSizeDeviation;

    public final int maxFrontSize;

    public final File outputDir;

    public final List<Orientation> orientationsToCheck;

    public final DedicatedWriter<LightDna> dnaWriter;
    public final int maxErrorsPerKmer;

    public final Reporter<FillingReport> reporter;

    public GlobalContext(int k, WeightedDeBruijnGraph graph, int minFragmentSize, int maxFragmentSize, int maxFrontSize, File outputDir, List<Orientation> orientationsToCheck, DedicatedWriter<LightDna> dnaWriter, int maxErrorsPerKmer) {
        this(k, graph, minFragmentSize, maxFragmentSize, maxFrontSize, outputDir, orientationsToCheck, dnaWriter, maxErrorsPerKmer, new IgnoringReporter<FillingReport>());
    }
    public GlobalContext(int k, WeightedDeBruijnGraph graph, int minFragmentSize, int maxFragmentSize, int maxFrontSize, File outputDir, List<Orientation> orientationsToCheck, DedicatedWriter<LightDna> dnaWriter, int maxErrorsPerKmer, Reporter<FillingReport> reporter) {
        this.k = k;
        this.graph = graph;
        this.minFragmentSize = minFragmentSize;
        this.maxFragmentSize = maxFragmentSize;
        this.maxFrontSize = maxFrontSize;
        this.outputDir = outputDir;
        this.orientationsToCheck = orientationsToCheck;
        this.dnaWriter = dnaWriter;
        this.maxErrorsPerKmer = maxErrorsPerKmer;
        this.reporter = reporter;
    }

    /*
    final Queue<List<DnaQ>> queue;
    final Queue<List<UniPair<DnaQ>>> failedQueue;
    final int k;

    final int maxFragmentSize; // = averageFragmentSize + fragmentSizeDeviation;
    final int minFragmentSize; // = averageFragmentSize - fragmentSizeDeviation;

    final long toVertexMask;
    final int dnaBuilderCapacity;

    final DeBruijnGraph graph;


    final Reporter reporter;

    public GlobalContext(Queue<List<DnaQ>> queue, Queue<List<UniPair<DnaQ>>> failedQueue,
                         int k, int minFragmentSize,
                         int maxFragmentSize, DeBruijnGraph graph) {
        this(queue, failedQueue, k, minFragmentSize, maxFragmentSize, graph, Arrays.asList(Orientation.FR), new IgnoringReporter());
    }

    public GlobalContext(Queue<List<DnaQ>> queue, Queue<List<UniPair<DnaQ>>> failedQueue,
                         int k, int minFragmentSize,
                         int maxFragmentSize, DeBruijnGraph graph,
                         List<Orientation> orientationsToCheck,
                         Reporter reporter
    ) {
        this.queue = queue;
        this.failedQueue = failedQueue;
        this.k = k;
        this.minFragmentSize = minFragmentSize;
        this.maxFragmentSize = maxFragmentSize;
        this.graph = graph;
        this.orientationsToCheck = new ArrayList(orientationsToCheck);

        toVertexMask = (1L << (2 * k)) - 1;
        dnaBuilderCapacity = maxFragmentSize;
        this.reporter = reporter;
    }
    */
}
