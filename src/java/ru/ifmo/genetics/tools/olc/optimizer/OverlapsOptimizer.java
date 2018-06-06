package ru.ifmo.genetics.tools.olc.optimizer;


import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.executors.BlockingThreadPoolExecutor;
import ru.ifmo.genetics.tools.olc.layouter.DfsAlgo;
import ru.ifmo.genetics.tools.olc.overlaps.OptimizingTask;
import ru.ifmo.genetics.tools.olc.overlaps.Overlaps;
import ru.ifmo.genetics.tools.olc.overlaps.OverlapsList;
import ru.ifmo.genetics.utils.TextUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static ru.ifmo.genetics.utils.NumUtils.groupDigits;

public class OverlapsOptimizer extends Tool {
    public static final String NAME = "overlaps-optimizer";
    public static final String DESCRIPTION = "optimizes overlaps by removing transitive overlaps";


    // input params
    public final Parameter<File> readsFile = addParameter(new FileParameterBuilder("reads-file")
            .mandatory()
            .withDescription("file with all reads")
            .create());

    public final Parameter<File> overlapsFile = addParameter(new FileParameterBuilder("overlaps-file")
            .mandatory()
            .withDescription("file with all overlaps")
            .create());

    public final Parameter<File> optimizedOverlapsFile = addParameter(new FileParameterBuilder("optimized-overlaps-file")
            .optional()
            .withDefaultValue(workDir.append("overlaps.optimized"))
            .withDescription("file with optimized overlaps with weight")
            .create());


    // internal variables
    private int readsNumber;
    private ArrayList<Dna> reads;
    private Overlaps overlaps;
    private Overlaps newOverlaps;


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            load();
            sortOverlaps();
            optimizeOverlaps();
            newOverlaps.printToFile(optimizedOverlapsFile.get().toString());
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            throw new ExecutionFailedException(e);
        }
    }

    private void load() throws IOException, InterruptedException {
        info("Loading reads...");
        reads = ReadersUtils.loadDnasAndAddRC(readsFile.get());
        readsNumber = reads.size();

        info("Loading overlaps...");
        overlaps = new Overlaps(reads, new File[]{overlapsFile.get()}, availableProcessors.get());
    }
    
    private void sortOverlaps() throws InterruptedException {
        info("Sorting overlaps...");
        overlaps.sortAll();
    }

    private void optimizeOverlaps() throws InterruptedException {
        info("Optimizing overlaps...");
        Timer timer = new Timer();
        long bs = overlaps.calculateSize();

        newOverlaps = new Overlaps(overlaps, false, true);

        BlockingThreadPoolExecutor executor = new BlockingThreadPoolExecutor(availableProcessors.get());
        int taskSize = readsNumber / availableProcessors.get() + 1;
//        int taskSize = readsNumber / 1 + 1;
        for (int i = 0; i < readsNumber; i += taskSize) {
            executor.blockingExecute(
                    new OptimizingTask(overlaps, newOverlaps, i, Math.min(i + taskSize, readsNumber)));
        }
        executor.shutdownAndAwaitTermination();

        calculateWeight();


        long os = newOverlaps.calculateSize();
        String s = "After optimizing " + groupDigits(os) + " overlaps";
        if (bs != 0) {
            s += String.format(" (%.1f%% of all)", 100.0 * os / bs);
        }
        info(s);
        if (bs != 0) {
            int rrn = newOverlaps.calculateRealReadsNumber();
            debug("real reads number = " + groupDigits(rrn) + ", " +
                    String.format("%.2f overlaps per read", os * 2 / (double) rrn));

            // printing additional statistics
            int cnt = 0;
            int[] ov = new int[6];
            for (int i = 0; i < newOverlaps.readsNumber; i++) {
                if (!newOverlaps.isReadRemoved(i)) {
                    int deg = newOverlaps.getInDegree(i) + newOverlaps.getOutDegree(i);
                    if (deg > 5) deg = 5;
                    ov[deg]++;
                    cnt++;
                }
            }

            debug("Overlaps' statistics (" + cnt + " good reads):");
            debug("# sum deg -> \t0\t\t1\t\t2\t\t3\t\t4\t\t5+");
            debug("count (%)\t" + ov[0] + " (" + (ov[0]*100/cnt) + "%)  \t" +
                            ov[1] + " (" + (ov[1]*100/cnt) + "%)  \t" +
                            ov[2] + " (" + (ov[2]*100/cnt) + "%)  \t" +
                            ov[3] + " (" + (ov[3]*100/cnt) + "%)  \t" +
                            ov[4] + " (" + (ov[4]*100/cnt) + "%)  \t" +
                            ov[5] + " (" + (ov[5]*100/cnt) + "%)"
            );


            DfsAlgo dfs = new DfsAlgo(newOverlaps);
            dfs.run();

            info("There are " + groupDigits(dfs.compNumber) + " components");
            info("Max sizes = " + Arrays.toString(dfs.compSizes));

        }
        debug("Optimizing overlaps took " + timer);
    }

    private int walk(int from, Overlaps overlaps, HashSet<Integer> visited) {
        visited.add(from);
        int ans = 1;

        OverlapsList edges = new OverlapsList(overlaps.withWeights);
        overlaps.getAllOverlaps(from, edges);
        for (int j = 0; j < edges.size(); ++j) {
            int to = edges.getTo(j);
            if (!visited.contains(to)) {
                System.err.println("moved to " + to);
                ans += walk(to, overlaps, visited);
                System.err.println("returned to " + from);
            }
        }
        return ans;
    }


    private void calculateWeight() {
        info("Calculating weight...");
//        QuantitativeStatistics<Integer> weightStat = new QuantitativeStatistics<Integer>();
//        QuantitativeStatistics<Integer> weightGoodStat = new QuantitativeStatistics<Integer>();
//        QuantitativeStatistics<Integer> weightBadStat = new QuantitativeStatistics<Integer>();
        for (int i = 0; i < newOverlaps.readsNumber; i++) {
            if (newOverlaps.getList(i) != null) {
                OverlapsList list = newOverlaps.getList(i);
                for (int j = 0; j < list.size(); j++) {
                    int to = list.getTo(j);
                    int centerShift = list.getCenterShift(j);
                    int overlap = newOverlaps.calculateOverlapLen(i, to, centerShift);

                    list.setWeight(j, overlap);

//                    weightStat.add(weight);
//                    if (checker.checkOverlap(i, to, centerShift)) {
//                        weightGoodStat.add(weight);
//                    } else {
//                        weightBadStat.add(weight);
//                    }
                }
            }
        }
//        weightStat.printToFile("work/weight.stat", null);
//        weightGoodStat.printToFile("work/weight_good.stat", null);
//        weightBadStat.printToFile("work/weight_bad.stat", null);
    }




    @Override
    protected void cleanImpl() {
        reads = null;
        overlaps = null;
        newOverlaps = null;
    }

    public OverlapsOptimizer() {
        super(NAME, DESCRIPTION);
    }

}
