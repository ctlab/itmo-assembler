package ru.ifmo.genetics.tools.rf.task;

import org.apache.log4j.Logger;
import ru.ifmo.genetics.dna.*;
import ru.ifmo.genetics.dna.kmers.ImmutableBigKmer;
import ru.ifmo.genetics.dna.kmers.ShallowBigKmer;
import ru.ifmo.genetics.io.Sink;
import ru.ifmo.genetics.io.formats.Illumina;
import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.statistics.reporter.Reporter;
import ru.ifmo.genetics.tools.rf.Orientation;
import ru.ifmo.genetics.utils.pairs.UniPair;
import ru.ifmo.genetics.utils.tool.Tool;

import java.io.PrintWriter;
import java.util.*;

public class FillingTask implements Runnable {
    private Logger logger = Logger.getLogger(FillingTask.class);
    private static byte DEFINETELY_QUALITY = 62;
    private static byte POLYMORPHISM_QUALITY = 2;
    private static int MAX_POLYMORPHISM_NUMBER = 20;

    private static int MAX_LAYER_SIZE = (int) 1e3;

    private static Illumina illumina = new Illumina();

    private static final double PHRED_THRESHOLD = 0.90;
    private static final double RIGHTNESS_METRIC_THRESHOLD = 1e-6;

    GlobalContext context;
    List<? extends UniPair<? extends LightDnaQ>> task;

    public final FillingReport fillingReport = new FillingReport();

    public long getAmbiguous() {
        return fillingReport.ambiguous.value();
    }

    public long getOk() {
        return fillingReport.ok.value();
    }

    public long getProcessed() {
        return fillingReport.processed.value();
    }

    public long getNotFound() {
        return fillingReport.notFound.value();
    }

    public enum ResultType { OK, NOT_FOUND, NO_ANCHOR, AMBIGUOUS, TOO_BIG, TOO_SHORT, TOO_POLYMORPHIC, FAIL}

    PrintWriter graphOut = null;


    public static class FillingResult {
        public DnaQ dnaq;
        public int leftSkip;
        public int rightSkip;
        public ResultType type;

        public FillingResult(ResultType type) {
            this.type = type;
        }

        public FillingResult(DnaQ dnaq, int leftSkip, int rightSkip) {
            this.dnaq = dnaq;
            this.leftSkip = leftSkip;
            this.rightSkip = rightSkip;
            this.type = ResultType.OK;
        }
    }

    public FillingTask(GlobalContext context, List<? extends UniPair<? extends LightDnaQ>> task) {
        this.context = context;
        this.task = task;
    }

    public void runImpl() {
        Timer t = new Timer();
        t.start();

        Sink<LightDna> resultSink = context.dnaWriter.getLocalSink();

        List<UniPair<DnaQ>> failed = new ArrayList<UniPair<DnaQ>>(task.size());


        for (UniPair<? extends LightDnaQ> p : task) {
            //System.err.println(p.first);
            DnaQ dna = null;

            for (Orientation orientation: context.orientationsToCheck) {
                FillingResult res = fillRead(
                        p.first, p.second,
                        orientation.firstIsForward, orientation.secondIsForward);
                if (res.dnaq != null) {
                    dna = res.dnaq;
                    break;
                }
            }

            if (dna != null) {
                resultSink.put(dna);
                context.sumOutput.addAndGet(dna.length);
                if (context.sumOutput.get() >= context.maxSumOutput) {
                    break;
                }
            }
            // else if (noAnchor == 1) {
            // System.err.println("First no anchor");
            // failed.add(p);
            // System.exit(0);
            // }

            // System.err.print("\r" + dna);
            // System.err.println(i + ": ok = " + ok + ", notfound = " + notFound +
            // ", ambiguous = " + ambiguous + ", tooBig = " + tooBig + ", task.size = " + task.size());
        }
//        printStat();
        resultSink.close();
        context.failedQueue.add(failed);
    }

    @Override
    public void run() {
        runImpl();
        updateCounters();
    }

    public void printStat() {
        Tool.debug(logger, fillingReport);
    }

    public void updateCounters() {
        Reporter<FillingReport> reporter = context.reporter;
        reporter.mergeFrom(fillingReport);
        fillingReport.reset();
    }


    /**
     * Filling like paired-end
     */
    public FillingResult fillRead(IDnaQ left, IDnaQ right) {
        return fillRead(left, right, true, false);
    }

    public FillingResult fillRead(LightDnaQ left, LightDnaQ right, boolean pairEnds) {
        if (pairEnds) {
            return fillRead(left, right, true, false);
        } else {
            // mate pairs
            return fillRead(left, right, false, true);
        }
    }

    public FillingResult fillRead(LightDnaQ left, LightDnaQ right, boolean firstIsForward, boolean secondIsForward) {
        fillingReport.processed.increment();

        // System.err.println("from left:");
        // System.err.println(DnaTools.toString(left));
        // System.err.println("from right:");
        // System.err.println(DnaTools.toString(right));
        // System.err.println("to right:");
        // System.err.println(DnaTools.toString(new DnaView(right, 0, right.length(), true, true)));
//
        if ((left.length() < context.k) || (right.length() < context.k)) {
            fillingReport.tooShort.increment();
            return new FillingResult(ResultType.TOO_SHORT);
        }

        if (!firstIsForward) {
            left = DnaQView.rcView(left);
        }

        if (secondIsForward) {
            right = DnaQView.rcView(right);
        }

        // at this point orientation is always directed into gap: ---> <---
        // that is first is forward and second is reversed

        int oldLength = right.length();
        right = DnaQView.complementView(right);
        int newLength = right.length();
        assert oldLength == newLength : oldLength + " " + newLength;

        ImmutableBigKmer leftVertex = new ImmutableBigKmer(new DnaView(left, 0, context.k));
        ImmutableBigKmer rightVertex = new ImmutableBigKmer(new DnaView(right, 0, context.k, true, false));

        DnaQView leftHint = new DnaQView(left, context.k, left.length());
        DnaQView rightHint = new DnaQView(right, context.k, right.length());

        FillingResult res = doubleBfs(leftVertex, rightVertex,
                context.minFragmentSize - context.k,
                context.maxFragmentSize - context.k,
                leftHint, rightHint);
//        Tool.debug(logger, true, "visited = " + visited);

        if (res.type == ResultType.OK) {
            int polymorphismNumber = 0;
            for (int i = 0; i < res.dnaq.length(); ++i) {
                if (res.dnaq.phredAt(i) == POLYMORPHISM_QUALITY) {
                    ++polymorphismNumber;
                }
            }
            if (polymorphismNumber > MAX_POLYMORPHISM_NUMBER) {
                fillingReport.tooPolymorphic.increment();
                return new FillingResult(ResultType.TOO_POLYMORPHIC);
            }
            fillingReport.ok.increment();
        }

        return res;
    }
    private int visited;

    private boolean buildReversePath(DnaQBuilder builder, Set<BfsTreeNode> layer) {
        boolean[] possibleBases = new boolean[4];
        for (BfsTreeNode n : layer) {
            possibleBases[n.v.lastNuc()] = true;
        }

        int basesCount = 0;
        byte base = -1;

        for (byte i = 0; i < 4; ++i) {
            if (possibleBases[i]) {
                ++basesCount;
                if (base == -1) {
                    base = i;
                }
            }
        }

        /*
        if (basesCount > 2) {
            ++ambiguous;
            return false;
        }
        */

        Set<BfsTreeNode> nextLayer = new HashSet<BfsTreeNode>(layer.size());

        for (BfsTreeNode n : layer) {
            for (int i = 0; i < 4; ++i) {
                if (n.prev[i] != null) {
                    nextLayer.add(n.prev[i]);
                    /*
                    if (graphOut != null) {
                        graphOut.printf(
                                "%s -> %s [ label = \"%s\"];\n",
                                dotNodeId(n.prev[i]),
                                dotNodeId(n),
                                DnaTools.toChar((byte) (n.v & 3)));
                    }
                    */
                }
            }
        }

        if (nextLayer.isEmpty()) {
            ImmutableBigKmer vertex = layer.iterator().next().v;
            for (int j = 0; j < vertex.length(); ++j) {
                builder.append(vertex.nucAt(j), DEFINETELY_QUALITY);
            }

            return true;
        }

        if (!buildReversePath(builder, nextLayer)) {
            return false;
        }

        builder.append(base, (basesCount == 1) ? DEFINETELY_QUALITY : POLYMORPHISM_QUALITY);
        return true;
    }

    private boolean buildPath(DnaQBuilder builder, Set<BfsTreeNode> layer) {
        Set<BfsTreeNode> currentLayer = new HashSet<BfsTreeNode>();
        Set<BfsTreeNode> nextLayer = new HashSet<BfsTreeNode>();

        currentLayer.addAll(layer);

        boolean[] possibleBases = new boolean[4];

        while (true) {
            Arrays.fill(possibleBases, false);

            for (BfsTreeNode n : currentLayer) {
                for (int i = 0; i < 4; ++i) {
                    possibleBases[i] |= (n.prev[i] != null);
                    /*
                    if (graphOut != null && n.prev[i] != null) {
                        graphOut.println(dotNodeId(n) + " -> " + dotNodeId(n.prev[i]));
                        graphOut.printf(
                                "%s -> %s [ label = \"%s\"];\n",
                                dotNodeId(n),
                                dotNodeId(n.prev[i]),
                                DnaTools.toChar((byte) (n.prev[i].v & 3)));
                    }
                    */
                }
            }

            int basesCount = 0;
            byte base = -1;

            for (byte i = 0; i < 4; ++i) {
                if (possibleBases[i]) {
                    ++basesCount;
                    if (base == -1) {
                        base = i;
                    }
                }
            }

            /*
            if (basesCount > 2) {
                ++ambiguous;
                return false;
            }
            */

            if (basesCount == 0) {
                break;
            }

            builder.append(base, (basesCount == 1) ? DEFINETELY_QUALITY : POLYMORPHISM_QUALITY);

            for (BfsTreeNode n : currentLayer) {
                for (int i = 0; i < 4; ++i) {
                    if (n.prev[i] != null) {
                        nextLayer.add(n.prev[i]);
                    }
                }
            }

            Set<BfsTreeNode> temp = currentLayer;
            currentLayer = nextLayer;
            nextLayer = temp;
            nextLayer.clear();
        }
        return true;
    }


    private void leftBfsTurn(Map<ImmutableBigKmer, BfsTreeNode> currentQueue,
                             Map<ImmutableBigKmer, BfsTreeNode> nextQueue, byte nuc, double rightnessFactor) {
        boolean useHint = (rightnessFactor > PHRED_THRESHOLD);
        for (Map.Entry<ImmutableBigKmer, BfsTreeNode> entry : currentQueue.entrySet()) {
            ImmutableBigKmer v = entry.getKey();
            ShallowBigKmer e = new ShallowBigKmer(v);
            e.appendRight((byte) 0);
            for (int i = 0; i < 4; e.updateAt(context.k, (byte) i, (byte)0), ++i) {
                e.updateAt(context.k, (byte) 0, (byte)i);
                double localRightnessFactor = 1;
                if (useHint) {
                    if (i == nuc) {
                        localRightnessFactor = rightnessFactor;
                    } else {
                        localRightnessFactor = (1 - rightnessFactor) / 3;
                    }
                }
                boolean shouldBeAdded = context.graph.containsEdge(e);
                if (shouldBeAdded) {
                    ImmutableBigKmer newVertex = v.shiftRight((byte)i);

                    BfsTreeNode node = nextQueue.get(newVertex);
                    if (node == null) {
                        node = new BfsTreeNode(newVertex);
                        nextQueue.put(newVertex, node);
                    }
                    node.prev[v.nucAt(0)] = entry.getValue();
                    node.rightnessMetric += entry.getValue().rightnessMetric * localRightnessFactor;
                }
            }
        }
    }

    private void rightBfsTurn(Map<ImmutableBigKmer, BfsTreeNode> currentQueue,
                              Map<ImmutableBigKmer, BfsTreeNode> nextQueue, byte nuc, double rightnessFactor) {
        boolean useHint = (rightnessFactor > PHRED_THRESHOLD);
        for (Map.Entry<ImmutableBigKmer, BfsTreeNode> entry : currentQueue.entrySet()) {
            ImmutableBigKmer v = entry.getKey();
            ShallowBigKmer e = new ShallowBigKmer(v);
            e.appendLeft((byte) 0);
            for (int i = 0; i < 4; e.updateAt(0, (byte) i, (byte)0), ++i) {
                e.updateAt(0, (byte) 0, (byte)i);
                double localRightnessFactor = 1;
                if (useHint) {
                    if (i == nuc) {
                        localRightnessFactor = rightnessFactor;
                    } else {
                        localRightnessFactor = (1 - rightnessFactor) / 3;
                    }
                }
                boolean shouldBeAdded = context.graph.containsEdge(e);
                if (shouldBeAdded) {
                    ImmutableBigKmer newVertex = v.shiftLeft((byte)i);

                    BfsTreeNode node = nextQueue.get(newVertex);
                    if (node == null) {
                        node = new BfsTreeNode(newVertex);
                        nextQueue.put(newVertex, node);
                    }
                    node.prev[v.nucAt(context.k - 1)] = entry.getValue();
                    node.rightnessMetric += entry.getValue().rightnessMetric * localRightnessFactor;
                }
            }
        }
    }

    private void removeIncorrectNodes(Map<ImmutableBigKmer, BfsTreeNode> queue) {
        Iterator<BfsTreeNode> it = queue.values().iterator();
        while (it.hasNext()) {
            BfsTreeNode node = it.next();
            if (node.rightnessMetric < RIGHTNESS_METRIC_THRESHOLD) {
                fillingReport.dropped.increment();
                it.remove();
            }
        }
    }

    private FillingResult doubleBfs(ImmutableBigKmer leftVertex,
                                    ImmutableBigKmer rightVertex, long minPathLength, int maxPathLength,
                                    LightDnaQ leftHint, LightDnaQ rightHint) {
        DnaQBuilder leftDnaBuilder = new DnaQBuilder(context.dnaBuilderCapacity);
        Map<ImmutableBigKmer, BfsTreeNode> leftQueue = new HashMap<ImmutableBigKmer, BfsTreeNode>();
        Map<ImmutableBigKmer, BfsTreeNode> leftNextQueue = new HashMap<ImmutableBigKmer, BfsTreeNode>();
        Map<ImmutableBigKmer, BfsTreeNode> rightQueue = new HashMap<ImmutableBigKmer, BfsTreeNode>();
        Map<ImmutableBigKmer, BfsTreeNode> rightNextQueue = new HashMap<ImmutableBigKmer, BfsTreeNode>();

        leftQueue.put(leftVertex, new BfsTreeNode(leftVertex, 1));
        rightQueue.put(rightVertex, new BfsTreeNode(rightVertex, 1));

        /*
        try {
            if (KmerUtils.kmer2String(leftVertex, context.k).equals("ACTGATCGCGCGTCTGACGAACGAAGGTC".substring(0, context.k))) {
                graphOut = new PrintWriter("graph" + hashCode());
            } else {
                return new FillingResult(ResultType.NOT_FOUND);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        */

        if (graphOut != null) {
            graphOut.println("digraph G" + hashCode() + "_" + fillingReport.processed + "{");
            graphOut.println("center=true; rankdir=LR;");
        }

        DnaQ res = null;
        int leftSkipped = -1;
        int rightSkipped = -1;

        int leftLength = 0;
        int rightLength = 0;

        int maxSize = 2;

        boolean isAmbiguous = false;

        // printLeftLayer(leftQueue.values(), 0);
        // printRightLayer(rightQueue.values(), (int) (maxPathLength + 1));
        boolean noLeftAnchor = true;
        boolean noRightAnchor = true;

        visited = 2;

        for (int length = 1; length <= maxPathLength; ++length) {

            /*
            System.err.println("iteration: " + length);
            for (Long l : leftQueue.keySet()) {
                System.err.print("L");
                System.err.print(TextUtils.multiply(" ", leftLength));
                System.err.println(KmerUtils.kmer2String(l, context.k));
                System.err.print(TextUtils.multiply(" ", maxPathLength + context.k));
            }
            for (Long l : rightQueue.keySet()) {
                System.err.print("R");
                System.err.print(TextUtils.multiply(" ", maxPathLength - rightLength));
                System.err.println(KmerUtils.kmer2String(l, context.k));
            }
            */


            byte nuc = 0;
            byte phred = 2;
            if (leftQueue.size() < rightQueue.size() || (leftQueue.size() == rightQueue.size() && (length & 1) == 0)) { // left
                // System.err.println("left");
                if (leftLength < leftHint.length()) {
                    nuc = leftHint.nucAt(leftLength);
                    phred = leftHint.phredAt(leftLength);
                }
                leftBfsTurn(leftQueue, leftNextQueue, nuc, 1 - illumina.getProbability(phred));

                Map<ImmutableBigKmer, BfsTreeNode> oldQueue = leftQueue;
                leftQueue = leftNextQueue;
                leftNextQueue = oldQueue;

                leftNextQueue.clear();


                if (!leftQueue.isEmpty()) {
                    noLeftAnchor = false;
                }

                if (leftLength < leftHint.length()) {
                    leftVertex = leftVertex.shiftRight(nuc);
                    BfsTreeNode oldNode = leftQueue.get(leftVertex);
                    if (oldNode != null) {
                        oldNode.rightnessMetric = 1;
                    } else {
                        leftQueue.put(leftVertex, new BfsTreeNode(leftVertex, 1));
                    }
                }

                leftLength++;
                visited += leftQueue.size();

                if (leftQueue.isEmpty()) {
                    break;
                }

//                printLeftLayer(leftQueue.values(), leftLength);

                if (leftLength < leftHint.length()) {
                    removeIncorrectNodes(leftQueue);
                }

            } else { // right
                // System.err.println("right");
                if (rightLength < rightHint.length()) {
                        nuc = rightHint.nucAt(rightLength);
                        phred = rightHint.phredAt(rightLength);
                }
                rightBfsTurn(rightQueue, rightNextQueue, nuc, 1 - illumina.getProbability(phred));

                Map<ImmutableBigKmer, BfsTreeNode> oldQueue = rightQueue;
                rightQueue = rightNextQueue;
                rightNextQueue = oldQueue;

                rightNextQueue.clear();


                if (!rightQueue.isEmpty()) {
                    noRightAnchor = false;
                }

                if (rightLength < rightHint.length()) {
                    rightVertex = rightVertex.shiftLeft(nuc);
                    BfsTreeNode oldNode = rightQueue.get(rightVertex);
                    if (oldNode != null) {
                        oldNode.rightnessMetric = 1;
                    } else {
                        rightQueue.put(rightVertex, new BfsTreeNode(rightVertex, 1));
                    }
                }

                rightLength++;
                visited += rightQueue.size();

                if (rightQueue.isEmpty()) {
//                    System.err.println("empty");
                    break;
                }
//                printRightLayer(rightQueue.values(), (int)(maxPathLength + 1 - rightLength));
                if (rightLength < rightHint.length()) {
                    removeIncorrectNodes(rightQueue);
                }
            }

            // System.err.println(length + ": " + leftQueue.size() + " + " + rightQueue.size());

            if (length >= minPathLength) {
                Set<BfsTreeNode> leftHalfOfIntersection = new HashSet<BfsTreeNode>();
                Set<BfsTreeNode> rightHalfOfIntersection = new HashSet<BfsTreeNode>();
                for (Map.Entry<ImmutableBigKmer, BfsTreeNode> e : leftQueue.entrySet()) {
                    if (rightQueue.containsKey(e.getKey())) {
                        leftHalfOfIntersection.add(e.getValue());
                        rightHalfOfIntersection.add(rightQueue.get(e.getKey()));

                        if (graphOut != null) {
                            graphOut.println(
                                    dotNodeId(e.getValue(), leftLength) + " -> " +
                                    dotNodeId(rightQueue.get(e.getKey()), (int)(maxPathLength + 1 - rightLength)) +
                                    "[ color = \"green\", arrowhead=none ];");
                        }

                    }
                }

                if (!leftHalfOfIntersection.isEmpty()) {
                    if (res != null) {
                        // isAmbiguous = true;
                        fillingReport.ambiguous.increment();
//                        System.err.println("ambiguous");
//                        System.err.println("ambiguous " + res.length() + " " + (length + context.k));
                        if (graphOut != null) {
                            graphOut.println("}");
                            graphOut.close();
                        }
                        return new FillingResult(ResultType.AMBIGUOUS);
                    }

                    if (!buildReversePath(leftDnaBuilder, leftHalfOfIntersection)) {
//                        System.err.println("fail1");
                        return new FillingResult(ResultType.FAIL);
                    }

                    leftSkipped = leftLength + context.k - leftDnaBuilder.length();

                    int oldLength = leftDnaBuilder.length();

                    if (!buildPath(leftDnaBuilder, rightHalfOfIntersection)) {
//                        System.err.println("fail2");
                        return new FillingResult(ResultType.FAIL);
                    }

                    rightSkipped = rightLength - (leftDnaBuilder.length() - oldLength);

                    res = leftDnaBuilder.build();
                    // System.err.println("built:");
                    // System.err.println(res);
                    leftDnaBuilder = new DnaQBuilder(context.dnaBuilderCapacity);
                }
            }

            maxSize = Math.max(maxSize, leftQueue.size() + rightQueue.size());

            if (maxSize > MAX_LAYER_SIZE) {
                if (graphOut != null) {
                    graphOut.println("}");
                    graphOut.close();
                }
                fillingReport.tooBig.increment();
//                System.err.println("toobig");
                return new FillingResult(ResultType.TOO_BIG);
            }


        }

        if (graphOut != null) {
            graphOut.println("}");
            graphOut.close();
        }

        if (isAmbiguous) {
            fillingReport.ambiguous.increment();
            return new FillingResult(ResultType.AMBIGUOUS);
        }

        if (leftLength > 0 && noLeftAnchor || rightLength > 0 && noRightAnchor) {
            fillingReport.noAnchor.increment();
            return new FillingResult(ResultType.NO_ANCHOR);
        }

        if (res == null) {
            fillingReport.notFound.increment();
            return new FillingResult(ResultType.NOT_FOUND);
        }

        return new FillingResult(res, leftSkipped, rightSkipped);
    }

    private static class BfsTreeNode {
        public ImmutableBigKmer v;
        public BfsTreeNode[] prev = new BfsTreeNode[4];
        public double rightnessMetric;

        private BfsTreeNode(ImmutableBigKmer v, double rightnessMetric) {
            this.v = v;
            this.rightnessMetric = rightnessMetric;
        }

        public BfsTreeNode(ImmutableBigKmer v) {
            this(v, 0);
        }

        public String toString(int len) {
            return v.toString();
        }
    }

    private String dotNodeId(BfsTreeNode node, int level) {
        return "node" + node.toString(context.k) + "_" + level;
    }

    private String dotNodeId(BfsTreeNode node) {
        return node.toString(context.k);
    }

    private void printLayer(Collection<BfsTreeNode> layer, int i) {
        for (BfsTreeNode node : layer) {
            graphOut.printf(
                    "%s [ label = \"%s\", shape=point];\n",
                    dotNodeId(node, i),
                    node.toString(context.k)
            );
        }

        graphOut.print("{ rank=same; ");
        for (BfsTreeNode node : layer) {
            graphOut.print(dotNodeId(node, i));
            graphOut.print("; ");
        }
        graphOut.println("}");

    }

    private void printLeftLayer(Collection<BfsTreeNode> layer, int i) {
        if (graphOut == null)
            return;
        printLayer(layer, i);
        for (BfsTreeNode node : layer) {
            for (int j = 0; j < 4; ++j) {
                if (node.prev[j] != null) {
                    graphOut.printf(
                            "%s -> %s [ label = \"%s\", color=blue];\n",
                            dotNodeId(node.prev[j], i - 1),
                            dotNodeId(node, i),
                            DnaTools.toChar(node.v.lastNuc()));
                }
            }
        }
    }

    private void printRightLayer(Collection<BfsTreeNode> layer, int i) {
        if (graphOut == null)
            return;
        printLayer(layer, i);
        for (BfsTreeNode node : layer) {
            for (int j = 0; j < 4; ++j) {
                if (node.prev[j] != null) {
                    graphOut.printf(
                            "%s -> %s [ label = \"%s\", color=red];\n",
                            dotNodeId(node, i),
                            dotNodeId(node.prev[j], i + 1),
                            DnaTools.toChar(node.v.lastNuc()));
                }
            }
        }
    }
}

