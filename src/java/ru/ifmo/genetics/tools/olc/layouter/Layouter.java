package ru.ifmo.genetics.tools.olc.layouter;

import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.tools.olc.overlaps.FullEdge;
import ru.ifmo.genetics.tools.olc.overlaps.Overlaps;
import ru.ifmo.genetics.tools.olc.overlaps.OverlapsList;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static ru.ifmo.genetics.tools.olc.overlaps.OverlapsList.Edge;
import static ru.ifmo.genetics.utils.NumUtils.groupDigits;

public class Layouter extends Tool {
    public static final String NAME = "layouter";
    public static final String DESCRIPTION = "layouts contigs";


    // input params
    public final Parameter<File> readsFile = addParameter(new FileParameterBuilder("reads-file")
            .mandatory()
            .withShortOpt("r")
            .withDescription("file with all reads")
            .create());

    public final Parameter<File> overlapsFile = addParameter(new FileParameterBuilder("overlaps-file")
            .mandatory()
            .withShortOpt("O")
            .withDescription("file with all reads")
            .withDescription("file with overlaps")
            .create());

    public final Parameter<File> layoutFile = addParameter(new FileParameterBuilder("layout-file")
            .optional()
            .withShortOpt("o")
            .withDefaultValue(workDir.append("layout"))
            .withDescription("file with resulting layout")
            .create());

    public final Parameter<Integer> mergeLength = addParameter(new IntParameterBuilder("merge-length")
            .optional()
            .withDefaultValue(10000)
            .withDescription("maximal merge length")
            .create());

    public final Parameter<Integer> tipsDepth = addParameter(new IntParameterBuilder("tips-depth")
            .optional()
            .withShortOpt("td")
            .withDefaultValue(5)
            .withDescription("maximal tips depth to remove")
            .create());

    public final Parameter<Integer> readsNumberParameter = addParameter(new IntParameterBuilder("reads-number")
            .optional()
            .withDefaultValue(-1)
            .withDescription("strange parameter (if -1 (default), then loads all reads, " +
                    "else adds reads-number empty dna instead of reads)")
            .create());

    public final Parameter<File> finalOverlapsFile = addParameter(new FileParameterBuilder("final-overlaps-file")
            .optional()
            .withDescription("file with resulting overlaps")
            .create());



    // internal variables
    private int readsNumber;
    private ArrayList<Dna> reads;
    protected Overlaps<Dna> overlaps;
    private int averageWeight;


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            load();
            sortOverlaps();
            averageWeight = overlaps.getAverageWeight();

            info("Graph simplification...");
            debug("Before simplification: " + groupDigits(overlaps.calculateRealReadsNumber()) + " real reads, " +
                    groupDigits(overlaps.calculateSize()) + " overlaps");

            removeTips("removeTips 1");
            mergeGraph("mergeGraph");
            removeTips("removeTips 2");
//            overlaps.printToFile("overlaps.before.indel");
            mergePathsWithIndel("mergePathsWithIndel 1");
//            overlaps.printToFile("overlaps.before.nonminimal");
            removeNonMinimalOverlaps("removeNonMinimalOverlaps(false)", false);
            removeTips("removeTips 3");
//            overlaps.printToFile("overlaps.before.indel2");
            mergePathsWithIndel("mergePathsWithIndel 2");
//            overlaps.printToFile("overlaps.after.indel2");
            removeTips("removeTips 4");
//            removeNonMinimalOverlaps(true);


            debug("After simplification: " + groupDigits(overlaps.calculateRealReadsNumber()) + " real reads, " +
                    groupDigits(overlaps.calculateSize()) + " overlaps");
            if (finalOverlapsFile.get() != null) {
                overlaps.printToFile(finalOverlapsFile.get());
            }

            info("Making layout...");
            makeLayout(new SimpleLayoutWriter(new PrintWriter(layoutFile.get())));

        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            throw new ExecutionFailedException(e);
        }
    }

    protected void load() throws IOException, InterruptedException {
        info("Loading...");
        readsNumber = readsNumberParameter.get();
        if (readsNumber == -1) {
            reads = ReadersUtils.loadDnasAndAddRC(readsFile.get());
            readsNumber = reads.size();
        } else {
            Dna emptyDna = new Dna("");
            reads = new ArrayList<Dna>(readsNumber);
            for (int i = 0; i < readsNumber; ++i) {
                reads.add(emptyDna);
            }
        }

        overlaps = new Overlaps(reads, new File[]{overlapsFile.get()}, availableProcessors.get());
    }

    protected void sortOverlaps() throws InterruptedException {
        overlaps.sortAll();
    }



    // ================================ remove tips ====================================

    /**
     * Removes tips with tip depth <= tipsDepth parameter.
     * Tip is a branch with small length (length of the other branch should be at least two times longer,
     * if not - this is a fork).
     *
     */
    protected int removeTips(String stageName) {
        depth = new HashMap<Integer, Integer>();
        toRemove = new HashSet<FullEdge>();
        removedEdges = 0;

        DfsAlgo2 dfs = new DfsAlgo2(overlaps);
        dfs.run();

        // Actual removing...
        for (FullEdge e : toRemove) {
            removedEdges += overlaps.removeOverlap(e.from, e.to, e.centerShift);
        }
        debug(stageName + ": removed " + groupDigits(removedEdges) + " edges");

        depth = null;
        toRemove = null;
        return removedEdges;
    }
    protected int removeTips() {
        return removeTips("removeTips");
    }

    private HashMap<Integer, Integer> depth;
    private HashSet<FullEdge> toRemove;
    private int removedEdges;



    private class DfsAlgo2 extends DfsAlgo {
        public DfsAlgo2(Overlaps overlaps) {
            super(overlaps);
        }

        @Override
        protected boolean checkAndWalk(int i) {
            if (overlaps.getList(i).size() > 1) {
                return super.checkAndWalk(i);
            }
            return false;
        }

        @Override
        protected int getTo(OverlapsList forward, OverlapsList backward, int index) {
            if (index < forward.size()) {
                return forward.getTo(index);
            }
            return -1;
        }

        @Override
        protected void addToStack(int to) {
            super.addToStack(to);
            depth.put(to, -1);    // vertex is in process
        }

        @Override
        protected boolean checkGoForward(VInfo vi, int to) {
            Integer md = depth.get(to);
            if (md != null) {
                if (md == -1) {     // we found circle
                    depth.put(to, tipsDepth.get() * 2); // max value
                }
                return false;
            }
            return true;
        }

        @Override
        protected void updateGoingBackward(VInfo vi, VInfo prev) {
            int maxDepth = 0;       // currently only this vertex
            int minDepth = 0;

            OverlapsList edges = overlaps.getList(vi.v);
            for (int i = 0; i < edges.size(); i++) {
                int to = edges.getTo(i);
                assert depth.get(to) >= 0;
                int curDepth = depth.get(to) + 1;
                maxDepth = Math.max(maxDepth, curDepth);
                minDepth = Math.min(minDepth, curDepth);
            }
            depth.put(vi.v, maxDepth);

            if (edges.size() > 1 && minDepth <= tipsDepth.get() && maxDepth >= minDepth * 2) {
                // removing...
                for (int i = 0; i < edges.size(); i++) {
                    int to = edges.getTo(i);
                    int curDepth = depth.get(to) + 1;
                    if (curDepth <= tipsDepth.get() && maxDepth >= curDepth * 2) {
                        toRemove.add(new FullEdge(vi.v, to, edges.getCenterShift(i)));
//                        removedEdges += overlaps.removeOverlap(vi.v, to, edges.getCenterShift(i));
                    }
                }
            }
        }
    }




    // ================================ merge graph ======================================

    protected int merges;

    protected void mergeGraph(String stageName) {
        merges = 0;

        OverlapsList tempList = new OverlapsList(overlaps.withWeights);
        int tries = 0;
        for (int i = 0; i < readsNumber; ++i) {
//            if ((i & (i - 1)) == 0) {
//                System.err.println(i + "/" + readsCount);
//            }
            if (overlaps.isReadRemoved(i)) {
                continue;
            }
            if (overlaps.getForwardOverlaps(i, tempList).size() > 1) {
                tryMergePaths(i, mergeLength.get());
                tries++;
//                if ((tries & (tries - 1)) == 0) {
//                    System.err.println(tries + " tries");
//                }
            }
        }
        debug(stageName + ": " + merges + " merges");
    }
    protected void mergeGraph() {
        mergeGraph("mergeGraph");
    }


    private void tryMergePaths(int from, int depth) {
//        System.out.print("Trying to merge from vertex " + from + "... ");

        PriorityQueue<Edge> queue = new PriorityQueue<Edge>();
        HashMap<Edge, Edge> prevs = new HashMap<Edge, Edge>();
        HashSet<Integer> visited = new HashSet<Integer>();
        Edge fromEdge = new Edge(from, 0);
        queue.add(fromEdge);
        prevs.put(fromEdge, null);
        boolean firstly = true;
        int merges = 0;
        OverlapsList tempList = new OverlapsList(overlaps.withWeights);
        while (true) {
            if (!firstly && queue.size() == 1) {
                break;
            }

            if (queue.size() > 20) {
                break;
            }

            firstly = false;


            Edge u = queue.poll();

            if (u.centerShift > depth) {
                continue;
            }

            if (!visited.add(u.to)) {
                continue;
            }

            {
                int visitedSize = prevs.size();
                if ((visitedSize & 65536) != 0) {
                    break;
                }
            }

            overlaps.getForwardOverlaps(u.to, tempList);

            for (int i = 0 ; i < tempList.size(); ++i) {
                Edge v = tempList.get(i);
                v.centerShift += u.centerShift;

                if (prevs.containsKey(v)) {
                    Edge u2 = prevs.get(v);
                    if (u2 == null) {
                        continue;
                    }
                    Consensus path1consensus = getPathConsensus(fromEdge, u2, prevs);
                    Consensus path2consensus = getPathConsensus(fromEdge, u, prevs);
                    int numberOfMismatches = 0;
                    int minPositiveSize = Math.min(path1consensus.positiveSize(), path2consensus.positiveSize());
                    int minNegativeSize = Math.min(path1consensus.negativeSize(), path2consensus.negativeSize());

                    for (int j = -minNegativeSize; j < minPositiveSize; ++j) {
                        NucleotideConsensus c1 = path1consensus.getNucleotideConsensus(j);
                        NucleotideConsensus c2 = path2consensus.getNucleotideConsensus(j);
                        if (c1.size() == 0 || c2.size() == 0) {
                            continue;
                        }

                        if (c1.get() != c2.get()) {
                            numberOfMismatches++;
                        }
                    }
                    if (numberOfMismatches <= (minPositiveSize + minNegativeSize) / 10) {
                        mergeBackPaths(v, prevs.get(v), u, prevs);

                        merges++;
                    } else {
                        visited.add(v.to);
                    }
                    continue;

                }

                prevs.put(v, u);
                queue.add(v);
            }
        }
//        System.err.println(merges + " merges");
        this.merges += merges;
    }


    private Consensus getPathConsensus(Edge from, Edge to, HashMap<Edge, Edge> prevs) {
        Consensus consensus = new Consensus(overlaps.reads, 0.7, 1);
        Edge originalTo = to;
        while (to != null) {
            int shift = overlaps.centerShiftToBeginShift(from.to, to.to, to.centerShift);
            consensus.addDna(reads.get(to.to), shift);
            to = prevs.get(to);
        }
        return consensus;

    }

    private void mergeBackPaths(Edge end, Edge to1, Edge to2, HashMap<Edge, Edge> prevs) {
//        System.err.println(end + " " + to1 + " " + to2);
        // to1 and to2 are both connected to end
        if (to1.equals(to2)) {
            prevs.put(end, to1);
            return;
        }

        try {
            if (!overlaps.isWellOriented(to2.to, to1.to, to1.centerShift - to2.centerShift)) {
                Edge t = to2; to2 = to1; to1 = t;
            }
            Edge newTo2 = prevs.get(to1);
            int w;
            try {
                w = getOverlapsWeight(to2, end);
            } catch (IndexOutOfBoundsException e) {
                // :ToDo: change exception to something more specific
                // we have already removed this edge
                /**
                 *   x--->y--->z--->t--->e
                 *                      /
                 *     a--->b--->Y--->X
                 */
                // :ToDo: change handling of this situation
                w = averageWeight;
            }

            removeOverlap(to2, end);
            addOverlap(to2, to1, w);
            prevs.put(end, to1);
            mergeBackPaths(to1, to2, newTo2, prevs);

        } catch (StackOverflowError e) {
            System.err.println(end + " " + to1 + " " + to2);
            throw e;
        }
    }

    private void addOverlap(Edge from, Edge to, int weight) {
        overlaps.addOverlap(from.to, to.to, to.centerShift - from.centerShift, weight);
    }

    private int removeOverlap(Edge from, Edge to) {
        return overlaps.removeOverlap(from.to, to.to, to.centerShift - from.centerShift);
    }

    private int getOverlapsWeight(Edge from, Edge to) {
        return overlaps.getWeight(from.to, to.to, to.centerShift - from.centerShift);
    }



    // ================================== merge paths with indel ================================

    /**
     * Actually not merges but removes one of the paths
     */
    protected void mergePathsWithIndel(String stageName) {
        merges = 0;

        OverlapsList tempList = new OverlapsList(overlaps.withWeights);
        int tries = 0;
        for (int i = 0; i < readsNumber; ++i) {
            if (overlaps.isReadRemoved(i)) {
                continue;
            }
            if (overlaps.getForwardOverlaps(i, tempList).size() > 1) {
                mergePathsWithIndelStartingFrom(i, mergeLength.get());
                tries++;
            }
        }

        debug(stageName + ": " + merges + " merges");
    }
    protected void mergePathsWithIndel() {
        mergePathsWithIndel("mergePathsWithIndel");
    }

    private void mergePathsWithIndelStartingFrom(int from, int depth) {
        PriorityQueue<Edge> queue = new PriorityQueue<Edge>();
        HashMap<Edge, Edge> prevs = new HashMap<Edge, Edge>();
        HashSet<Integer> visited = new HashSet<Integer>();
        queue.add(new Edge(from, 0));
        prevs.put(new Edge(from, 0), null);
        boolean firstly = true;
        int merges = 0;
        OverlapsList tempList = new OverlapsList(overlaps.withWeights);
        while (!queue.isEmpty()) {
            if (!firstly && queue.size() == 1) {
                break;
            }

            if (queue.size() > 20) {
                break;
            }

            firstly = false;


            Edge u = queue.poll();

            if (u.centerShift > depth) {
                continue;
            }

            if (!visited.add(u.to)) {
                continue;
            }

            overlaps.getForwardOverlaps(u.to, tempList);

            for (int i = 0 ; i < tempList.size(); ++i) {
                Edge v = tempList.get(i);
                v.centerShift += u.centerShift;

                Edge v2 = new Edge(v);
                boolean removed = false;
                int maxDeviation = Math.max(4, 2 * (int)(v.centerShift * 0.01));
                for (int d = -maxDeviation; d <= maxDeviation; d += 2) {
                    v2.centerShift = v.centerShift + d;
                    if (v2.centerShift <= 0) {
                        if (prevs.containsKey(v2)) {
                            Tool.warn(logger, "Short cycle found in vertex " + v2.to);
                        }
                        continue;
                    }

                    Edge u2 = prevs.get(v2);
                    if (u2 == null || overlaps.isReadRemoved(u2.to)) {
                        continue;
                    }
                    boolean pathToVIsSimple = pathIsSimple(u, prevs);
                    boolean pathToV2IsSimple = pathIsSimple(u2, prevs);
                    // pathToV is longer than pathToV2

                    if (pathToV2IsSimple) {
                        removePath(u2, prevs);
                        overlaps.removeOverlapsWithNull(from);
                        if (removePathLastV >= 0) {
                            overlaps.removeOverlapsWithNull(removePathLastV);
                        }
                        overlaps.removeOverlapsWithNull(v2.to ^ 1);
                        queue.remove(v2);
                        prevs.remove(v2);
                        merges++;
                    } else if (pathToVIsSimple) {
                        removePath(u, prevs);
                        overlaps.removeOverlapsWithNull(from);
                        if (removePathLastV >= 0) {
                            overlaps.removeOverlapsWithNull(removePathLastV);
                        }
                        overlaps.removeOverlapsWithNull(v.to ^ 1);
                        removed = true;
                        merges++;
                        break;
                    }
                }

                if (!removed) {
                    prevs.put(v, u);
                    queue.add(v);
//                    System.err.println("added " + v + " to queue");
                } else {
                    break;
                }
            }
        }
//        System.err.println(merges + " merges");
        this.merges += merges;
    }

    private int removePathLastV = -1;

    private void removePath(Edge to, HashMap<Edge, Edge> prevs) {
        if (overlaps.isReadRemoved(to.to)) {
            // s -> x ->y -> yrc -> xrc -> src
            // Everything before should be already removed.
            Edge prev = prevs.get(to);
            while (prev != null) {
                assert overlaps.isReadRemoved(to.to);
                to = prev;
                prev = prevs.get(to);
            }
            removePathLastV = to.to;
            return;
        }
        assert !overlaps.isReadRemoved(to.to) : "Expected read " + to.to + " not to be removed";
        Edge prev = prevs.get(to);
        if (prev == null) {
            removePathLastV = to.to;
            return;
        }

        overlaps.markReadRemoved(to.to);
        removePath(prev, prevs);
    }

    private boolean pathIsSimple(Edge to, HashMap<Edge, Edge> prevs) {
        assert !overlaps.isReadRemoved(to.to) : "Expected read " + to.to + " not to be removed";
        Edge prev = prevs.get(to);
        if (prev == null) {
            return true;
        }

        return overlaps.getInDegree(to.to) == 1 && overlaps.getOutDegree(to.to) == 1 && pathIsSimple(prev, prevs);
    }



    // ============================== remove non minimal overlap ===============================

    static long ok = 0, bad = 0;

    private void removeNonMinimalOverlaps(String stageName, boolean checkBackwards) {
        Overlaps<Dna> newOverlaps = new Overlaps<Dna>(reads, availableProcessors.get(), overlaps.withWeights);

        OverlapsList tempList = new OverlapsList(overlaps.withWeights);
        for (int i = 0; i < readsNumber; ++i) {
            if (overlaps.isReadRemoved(i)) {
                newOverlaps.markReadRemoved(i);
                continue;
            }
            overlaps.removeOverlapsWithNull(i);
            overlaps.getForwardOverlaps(i, tempList);

            int maxJ = getMaxWeightIndex(tempList);
//            if (maxJ != -1 && checkBackwards && c < 50) {
//                System.err.println("from = " + i + ", maxJ = " + maxJ + ", maxWeight = " + tempList.getWeight(maxJ));
//                printList(tempList);
//            }

            if (maxJ != -1) {
                int j = tempList.getTo(maxJ);
                int cs = tempList.getCenterShift(maxJ);
                int ww = tempList.getWeight(maxJ);

                if (checkBackwards) {
//                    c++;
                    if (j > i) {
                        overlaps.removeOverlapsWithNull(j);
                    }
                    overlaps.getBackwardOverlaps(j, tempList);

                    int maxI = getMaxWeightIndex(tempList);
//                    System.err.println("from = " + j + ", maxI = " + maxI + ", maxWeight = " + tempList.getWeight(maxI));
//                    printList(tempList);
//                    System.err.println();

                    if (tempList.getTo(maxI) == i) {
                        ok++;
                    } else {
                        bad++;
                        continue;
                    }
                }

//                System.err.println("read " + i + ", adding overlap(" + i + ", " + tempList.getTo(maxJ) + ", " +
//                        tempList.getCenterShift(maxJ) + ", " + tempList.getWeight(maxJ) + ")");
                newOverlaps.addOverlap(i, j, cs, ww);
            }

        }
        debug(stageName + ": before overlaps = " + overlaps.calculateSize() + ", after = " + newOverlaps.calculateSize());
        debug(stageName + ": ok = " + ok + ", bad = " + bad);
        overlaps = newOverlaps;
    }

    void printList(OverlapsList list) {
        System.err.print("[");
        for (int j = 0; j < list.size(); j++) {
            System.err.print("(" + list.getTo(j) + ", " + list.getCenterShift(j) + ", " + list.getWeight(j) + "), ");
        }
        System.err.println("]");
        System.err.println("--------------------------------------------------------------------------------------");
    }

    private int getMaxWeightIndex(OverlapsList list) {
        int maxWeight = Integer.MIN_VALUE;
        int maxJ = -1;
        for (int j = 0; j < list.size(); ++j) {
            int jWeight = list.getWeight(j);

            if (j == 0 || jWeight > maxWeight) {
                maxWeight = jWeight;
                maxJ = j;
            }
        }
        return maxJ;
    }



    // =================================== make layout =====================================


    private void makeLayout(LayoutWriter writer) throws IOException {

        boolean[] was = new boolean[readsNumber];
        OverlapsList edges = new OverlapsList(overlaps.withWeights);

        for (int i = 0; i < readsNumber; ++i) {
            if (overlaps.isReadRemoved(i) || was[i]) {
                continue;
            }

            ArrayList<Integer> readsInCurrentContig = new ArrayList<Integer>();

//          Going backward from vertex i
            int startVertex = i;
            while (true) {
                overlaps.getBackwardOverlaps(startVertex, edges);
                if (edges.size() != 1) {
                    break;
                }
                int newStart = edges.getTo(0);
                if (overlaps.getForwardOverlaps(newStart, edges).size() != 1) {
                    break;
                }

                startVertex = newStart;
                if (startVertex == i) {
                    break;
                }
            }

//          Going forward from start vertex
            int curVertex = startVertex;
            int curShift = 0;
            while (!was[curVertex]) {
                writer.addLayout(curVertex, curShift);
                readsInCurrentContig.add(curVertex);
                was[curVertex] = true;

                overlaps.getForwardOverlaps(curVertex, edges);

                if (edges.size() != 1) {
                    break;
                }
                int newCur = edges.getTo(0);
                int cShift = edges.getCenterShift(0);
                if (overlaps.getBackwardOverlaps(newCur, edges).size() != 1) {
                    break;
                }

                curShift += overlaps.centerShiftToBeginShift(curVertex, newCur, cShift);
                curVertex = newCur;
            }

            writer.flush();
            for (int r: readsInCurrentContig) {
                was[r ^ 1] = true;
            }
        }

        writer.close();
    }



    @Override
    protected void cleanImpl() {
        reads = null;
        overlaps = null;
    }

    public Layouter() {
        super(NAME, DESCRIPTION);
    }

}
