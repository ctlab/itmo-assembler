package ru.ifmo.genetics.tools.irf;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.ifmo.genetics.dna.*;
import ru.ifmo.genetics.dna.kmers.ImmutableBigKmer;
import ru.ifmo.genetics.io.Sink;
import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.statistics.reporter.Reporter;
import ru.ifmo.genetics.structures.debriujn.WeightedDeBruijnGraph;
import ru.ifmo.genetics.tools.rf.Orientation;
import ru.ifmo.genetics.utils.pairs.UniPair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FillingTask implements Runnable {
    private final Logger logger = Logger.getLogger(FillingTask.class);
    public final FillingReport report = new FillingReport();

    private final GlobalContext context;
    private final List<? extends UniPair<? extends LightDnaQ>> task;

    public FillingTask(GlobalContext context) {
        this(context, new ArrayList<UniPair<? extends LightDnaQ>>());
    }

    public FillingTask(GlobalContext context, List<? extends UniPair<? extends LightDnaQ>> task) {
        this.context = context;
        this.task = task;
    }

    public FillingResult fill(@NotNull LightDnaQ left,
                              @NotNull LightDnaQ right,
                              @NotNull Orientation orientation) {
        final int k = context.k;

        report.processed.increment();
        if ((left.length() < k + 1) || (right.length() < k + 1)) {
            report.tooShort.increment();
            return FillingResult.fail(); // too short
        }


        if (!orientation.firstIsForward) {
            left = DnaQView.rcView(left);
        }

        if (orientation.secondIsForward) {
            right = DnaQView.rcView(right);
        }

        // at this point orientation is always directed towards the gap: ---> <---
        // i.e. first is forward and second is reversed

        right = DnaQView.complementView(right);

        int bestLeftShift = -1;
        int maxLeftWeight = 0;
        {
            int leftShift = 0;
            for (; leftShift < left.length() - k; ++leftShift) {
                ImmutableBigKmer leftEdge = new ImmutableBigKmer(new DnaView(left, leftShift, leftShift + k + 1));
                int edgeWeight = context.graph.getWeight(leftEdge);
                if (edgeWeight > maxLeftWeight) {
                    maxLeftWeight = edgeWeight;
                    bestLeftShift = leftShift;
                }
            }
        }

        if (maxLeftWeight < context.graph.minWeightToRealyAdd) {
            report.noAnchor.increment();
            return FillingResult.fail();
        }

        ImmutableBigKmer leftVertex = new ImmutableBigKmer(new DnaView(left, bestLeftShift, bestLeftShift + k));

        int bestRightShift = -1;
        int maxRightWeight = 0;
        {
            int rightShift = 0;
            for (; rightShift < right.length() - k; ++rightShift) {
                ImmutableBigKmer rightEdge = new ImmutableBigKmer(new DnaView(right, rightShift, rightShift + k + 1, true, false));

                int edgeWeight = context.graph.getWeight(rightEdge);
                if (edgeWeight > maxRightWeight) {
                    maxRightWeight = edgeWeight;
                    bestRightShift = rightShift;
                }
            }
        }

        if (maxRightWeight < context.graph.minWeightToRealyAdd) {
            report.noAnchor.increment();
            return FillingResult.fail();
        }

        ImmutableBigKmer rightVertex = new ImmutableBigKmer(new DnaView(right, bestRightShift, bestRightShift + k, true, false));

        DnaQView leftHint = new DnaQView(left, bestLeftShift + context.k, left.length());
        DnaQView rightHint = new DnaQView(right, bestRightShift + context.k, right.length());

        int fragmentSizeDelta = context.k + bestLeftShift + bestRightShift;
        DbfsGraph graph = doubleBfs(leftVertex, rightVertex,
                context.minFragmentSize - fragmentSizeDelta,
                context.maxFragmentSize - fragmentSizeDelta,
                leftHint,
                rightHint);

        if (graph == null) {
            report.tooBig.increment();
            return FillingResult.fail();
        }

//        Tool.debug(logger, true, "DBFS graph size: " + graph.size());
//        try {
//            graph.toDotFile(context.outputDir + File.separator + graph.id() + "_full.dot");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        if (!graph.hasLinks) {
//            if (context.outputDir != null && Math.random() < 0.001) {
//                graph.toDotFile(context.outputDir + File.separator + graph.id() + ".dot");
//            }
            report.notFound.increment();
            return FillingResult.fail();
        }

//        try {
//            graph.toDotFile(context.outputDir + File.separator + graph.id() + "_full.dot");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try {
            graph.analyse();
            if (graph.isCyclic) {
                report.cyclic.increment();
                return FillingResult.fail();
            }
        } catch (StackOverflowError e) {
//            Tool.warn(logger, true, "Stack overflow, while analysing graph for reads: " + left + " " + right);
            report.cyclic.increment();
            return FillingResult.fail();
        }

        if (graph.startNode().outEdges.size() == 1) {
            DbfsGraph.Edge e = graph.startNode().outEdges.iterator().next();
            if (e.to.equals(graph.endNode())) {
                report.ok.increment();
                return FillingResult.ok(new Dna(graph.startNode().mark, e.forwardDna), bestLeftShift, bestRightShift);
            }
        }

        {
            if (graph.startNode().outEdges.size() == 1) {
                DbfsGraph.Edge e = graph.startNode().outEdges.iterator().next();
                if (e.liesOnGoodWeightedPath() && e.length() > 100) {
//                    System.out.println(e.forwardDna);
                }
            }

            if (graph.endNode().inEdges.size() == 1) {
                DbfsGraph.Edge e = graph.endNode().inEdges.iterator().next();
                if (e.liesOnGoodWeightedPath() && e.length() > 100) {
//                    System.out.println(e.forwardDna);
                }
            }
//            System.out.println();
        }

//        semifails++;
//        if (context.outputDir != null && Math.random() < 1.01) {
//            graph.toDotFile(context.outputDir + File.separator + graph.id() + ".dot");
//        }
        report.ambiguous.increment();
        return FillingResult.ambiguous();


    }

    private DbfsGraph doubleBfs(
            ImmutableBigKmer leftVertex, ImmutableBigKmer rightVertex,
            int minPathLength, int maxPathLength,
            LightDnaQ leftHint, LightDnaQ rightHint) {

        DbfsGraph dbfsGraph = new DbfsGraph(context.k,
                leftVertex, rightVertex, context.maxErrorsPerKmer);

        DbfsGraph.Part lastTurn = DbfsGraph.Part.RIGHT;
        // length is the length of a connecting path that can be found in the _end_ of iteration
        int leftLength = 0;
        int rightLength = 0;
        for (int length = 1; length <= maxPathLength; ++length) {
            HashMap<ImmutableBigKmer,DbfsGraph.Node> leftFront = dbfsGraph.getLeftFront();
            HashMap<ImmutableBigKmer,DbfsGraph.Node> rightFront = dbfsGraph.getRightFront();
            if (leftFront.size() < rightFront.size() ||
                leftFront.size() == rightFront.size() &&  lastTurn == DbfsGraph.Part.RIGHT)  {
                leftBfsTurn(dbfsGraph);
                leftFront = dbfsGraph.getLeftFront();

                /*
                // Explicit adding k-mers from reads
                if (leftLength < leftHint.length()) {
                    long edge = context.graph.getOutEdge(leftVertex, leftHint.nucAt(leftLength));
                    leftVertex = context.graph.getEdgeEnd(edge);
                    leftFront.put(leftVertex, dbfsGraph.getNode(leftVertex));
                    dbfsGraph.setLeftFront(leftFront);
                }
                */

                leftLength++;
                lastTurn = DbfsGraph.Part.LEFT;
            } else {
                rightBfsTurn(dbfsGraph);
                rightFront = dbfsGraph.getRightFront();

                /*
                // Explicit adding k-mers from reads
                if (rightLength < rightHint.length()) {
                    long edge = context.graph.getInEdge(rightHint.nucAt(rightLength), rightVertex);
                    rightVertex = context.graph.getEdgeStart(edge);
                    rightFront.put(rightVertex, dbfsGraph.getNode(rightVertex));
                    dbfsGraph.setRightFront(rightFront);
                }
                */

                rightLength++;
                lastTurn = DbfsGraph.Part.RIGHT;
            }

            if (leftFront.size() + rightFront.size() > context.maxFrontSize) {
//                Tool.debug(logger, true, "Front is too big after " + length + " steps");
//                break;
                return null;
            }
        }

        return dbfsGraph;
    }

    private void leftBfsTurn(
            @NotNull DbfsGraph dbfsGraph) {
        HashMap<ImmutableBigKmer,DbfsGraph.Node> prevFront = dbfsGraph.getLeftFront();
        HashMap<ImmutableBigKmer, DbfsGraph.Node> newFront = new HashMap<ImmutableBigKmer, DbfsGraph.Node>();

        for (@NotNull DbfsGraph.Node node: prevFront.values()) {
            if (node.isMarkedInLeftBfs) {
                continue;
            }
            node.isMarkedInLeftBfs = true;

            if (node.isMarkedInRightBfs) {
                dbfsGraph.hasLinks = true;
            }

            ImmutableBigKmer v = node.mark;

            for (WeightedDeBruijnGraph.OutEdge edge: context.graph.outEdges(v)) {

                @NotNull DbfsGraph.Node newNode = dbfsGraph.getNode(edge.to);

                dbfsGraph.connect(
                        node,
                        newNode,
                        edge.weight, DbfsGraph.EdgeType.TRAVERSED
                );
                newFront.put(edge.to, newNode);
            }
        }

        dbfsGraph.setLeftFront(newFront);

    }

    private void rightBfsTurn(
            @NotNull DbfsGraph dbfsGraph) {

        HashMap<ImmutableBigKmer,DbfsGraph.Node> prevFront = dbfsGraph.getRightFront();
        HashMap<ImmutableBigKmer, DbfsGraph.Node> newFront = new HashMap<ImmutableBigKmer, DbfsGraph.Node>();

        for (DbfsGraph.Node node: prevFront.values()) {
            if (node.isMarkedInRightBfs) {
                continue;
            }
            node.isMarkedInRightBfs = true;

            if (node.isMarkedInLeftBfs) {
                dbfsGraph.hasLinks = true;
            }

            ImmutableBigKmer v = node.mark;

            for (WeightedDeBruijnGraph.InEdge edge: context.graph.inEdges(v)) {
                DbfsGraph.Node newNode = dbfsGraph.getNode(edge.from);

                dbfsGraph.connect(
                        newNode,
                        node,
                        edge.weight, DbfsGraph.EdgeType.TRAVERSED
                );

                newFront.put(edge.from, newNode);
            }
        }
        dbfsGraph.setRightFront(newFront);
    }

    @Override
    public void run() {
        runImpl();
        updateCounters();
    }

    private void updateCounters() {
        Reporter<FillingReport> reporter = context.reporter;
        reporter.mergeFrom(report);
        report.reset();
    }

    public void runImpl() {
        Timer timer = new Timer();
        Sink<LightDna> resultSink = context.dnaWriter.getLocalSink();

        for (UniPair<? extends LightDnaQ> p : task) {
            LightDna dna = null;

            for (Orientation orientation: context.orientationsToCheck) {
                FillingResult res = fill(p.first, p.second, orientation);
                if (res.dna != null) {
                    dna = res.dna;
                    break;
                }
            }

            if (dna != null) {
                resultSink.put(dna);
            }
        }
        resultSink.close();
        int n = numberOfCalls.incrementAndGet();
        long t = totalTime.addAndGet(timer.getTime());
//        if ((n & 15) == 0) {
//            Tool.debug(logger, true, "Average time per task (ms): " + t / n + " ( " + t + " / " + n + ")");
//        }
    }

    private static AtomicLong totalTime = new AtomicLong();
    private static AtomicInteger numberOfCalls = new AtomicInteger();

}

