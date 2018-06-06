package ru.ifmo.genetics.tools.olc.simplification;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.kmers.Kmer;
import ru.ifmo.genetics.dna.kmers.KmerIteratorFactory;
import ru.ifmo.genetics.dna.kmers.ShortKmerIteratorFactory;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.statistics.QuantitativeStatistics;
import ru.ifmo.genetics.structures.map.ArrayLong2IntHashMap;
import ru.ifmo.genetics.tools.olc.CheckerFromRef;
import ru.ifmo.genetics.tools.olc.ReadsGenerator;
import ru.ifmo.genetics.tools.olc.overlaps.Overlaps;
import ru.ifmo.genetics.tools.olc.overlaps.OverlapsList;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ru.ifmo.genetics.utils.NumUtils.*;

public class GraphSimplification extends Tool {
    public static final String NAME = "overlap-graph-simplification";
    public static final String DESCRIPTION = "simplifies overlap graph using coverage and repeat model";


    // input parameters
    public final Parameter<File[]> initialReads = addParameter(new FileMVParameterBuilder("initial-reads")
            .mandatory()
            .withShortOpt("i")
            .withDescription("initial paired-end binq files")
            .create());

    public final Parameter<Integer> k = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size")
            .create());

    public final Parameter<File> readsFile = addParameter(new FileParameterBuilder("reads-file")
            .mandatory()
            .withShortOpt("r")
            .withDescription("file with quasi-contigs")
            .create());

    public final Parameter<File> overlapsFile = addParameter(new FileParameterBuilder("overlaps-file")
            .mandatory()
            .withShortOpt("o")
            .withDescription("file with overlaps")
            .create());

    public final Parameter<File> simplifiedOverlapsFile = addParameter(new FileParameterBuilder("simplified-overlaps-file")
            .optional()
            .withDefaultValue(workDir.append("overlaps.simplified"))
            .withDescription("file with simplified overlaps with weight")
            .create());

    public final Parameter<Integer> unreliableThreshold = addParameter(new IntParameterBuilder("unreliable-threshold")
            .optional()
            .withDefaultValue(30)
            .withDescription("unreliable threshold in percent")
            .create());



    // internal variables
    public KmerStatisticsGatherer gatherer = new KmerStatisticsGatherer();
    {
        setFix(gatherer.k, k);
        setFix(gatherer.inputFiles, initialReads);
        addSubTool(gatherer);
    }
    private ArrayLong2IntHashMap hm;

    private QuantitativeStatistics<Integer> hmDistr;
    private QuantitativeStatistics<Integer> coverageDistr;

    private int readsNumber;
    private ArrayList<Dna> reads;
    private Overlaps overlaps;
//    private CheckerFromRef checker;


    // output parameters
    private final InMemoryValue<Integer> thresholdOutValue = new InMemoryValue<Integer>();
    public final InValue<Integer> thresholdOut = addOutput("threshold", thresholdOutValue, Integer.class);



    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            gatherer.simpleRun();

            hm = gatherer.hmOut.get();
            System.out.println("hm.size = " + hm.size());

            hmDistr = getQSFromHM(hm);
            hmDistr.printToFile(workDir.append("hm-distribution").toString());


            load();

            calculateOverlapsProbabilities();
            removeUnreliableOverlaps();

//            System.err.println();
//            System.err.println("Checking via countWrongOverlaps:");
//            countWrongOverlaps(overlaps, checker, null);


//            calculateStatistics();

            overlaps.printToFile(simplifiedOverlapsFile.get());


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

//        info("Loading reads' info...");
//        checker = new CheckerFromRef();

        info("Loading overlaps...");
        overlaps = new Overlaps(reads, new File[]{overlapsFile.get()}, availableProcessors.get());
    }


    public static int getDebugP(int vertex) {
//        int[] debugVertexes = {729289, 1520337};
        int[] debugVertexes = {};
        for (int i = 0; i < debugVertexes.length; i++) {
            int debugVertex = debugVertexes[i];
            if (vertex == debugVertex) {
                return i;
            }
        }
        return -1;
    }
    public static boolean shouldDebugOutput(int vertex) {
        return getDebugP(vertex) != -1;
    }
    public static boolean shouldDebugOutput(int from, int to) {
        return getDebugP(from) == 0 && getDebugP(to) == 1;
//        return true;
    }

    public final double A = 0.8;
    public final double B = 0.2;

    private void calculateOverlapsProbabilities() throws FileNotFoundException {
        info("Calculating overlaps probabilities...");

        QuantitativeStatistics<Integer> repeatPStat = new QuantitativeStatistics<Integer>();
        QuantitativeStatistics<Integer> repeatP1Stat = new QuantitativeStatistics<Integer>();
        QuantitativeStatistics<Integer> repeatP2Stat = new QuantitativeStatistics<Integer>();
        QuantitativeStatistics<Integer> repeatPBadStat = new QuantitativeStatistics<Integer>();
        QuantitativeStatistics<Integer> repeatP1BadStat = new QuantitativeStatistics<Integer>();
        QuantitativeStatistics<Integer> repeatP2BadStat = new QuantitativeStatistics<Integer>();

        RepeatProbabilityEvaluator1 evaluator1 = new RepeatProbabilityEvaluator1(reads, 4639675);
        RepeatProbabilityEvaluator2 evaluator2 = new RepeatProbabilityEvaluator2(reads, hm, k.get());

//        System.err.println();
//        System.err.println("Checking via countWrongOverlaps:");
//        countWrongOverlaps(overlaps, checker, null);

        progress.setTotalTasks(reads.size());
        progress.createProgressBar();
        int chs = 0, badCh1 = 0, badCh2 = 0;
        for (int i = 0; i < reads.size(); i++) {
            if (overlaps.isReadRemoved(i)) {
                continue;
            }

            int mK = -1;
            double minP = 10000;
            OverlapsList ovs = overlaps.getList(i);
            for (int k = 0; k < ovs.size(); k++) {
                int from = i;
                int to = ovs.getTo(k);
                int cs = ovs.getCenterShift(k);
                int bs = overlaps.centerShiftToBeginShift(from, to, cs);
//                int ovLen = overlaps.calculateOverlapLen(from, to, cs);

                if (overlaps.isReadRemoved(to)) {
                    throw new RuntimeException("Read is removed!");
                }

                double pr1 = evaluator1.calculateRepeatProbability(from, to, bs);
                double pr2 = evaluator2.calculateRepeatProbability(from, to, bs);

                double pr = A * pr1 + B * pr2;
                double p = 1 - pr;

//                if (pr < minP) {
//                    minP = pr;
//                    mK = k;
//                }
                
                int w = (int) Math.round(10000 * p);
                ovs.setWeight(k, w);

                int percent = (int) Math.round(pr * 100);
                int percent1 = (int) Math.round(pr1 * 100);
                int percent2 = (int) Math.round(pr2 * 100);
                repeatPStat.add(percent);
                repeatP1Stat.add(percent1);
                repeatP2Stat.add(percent2);
//                if (!checker.checkOverlap(from, to, cs)) {
//                    repeatPBadStat.add(percent);
//                    repeatP1BadStat.add(percent1);
//                    repeatP2BadStat.add(percent2);
//                }

//                if (shouldDebugOutput(from, to)) {
//                    System.out.println("P1 = " + String.format("%.5f%%", p1 * 100));
//                    System.out.println("P2 = " + String.format("%.5f%%", p2 * 100));
//                    System.out.println("P = " + String.format("%.5f%%", p * 100));
//                    System.out.println();
//                }
            }

//            if (ovs.size() > 0) {
//                int from = i;
//                int to = ovs.getTo(mK);
//                int cs = ovs.getCenterShift(mK);
//                chs++;
//                newOverlaps.addOverlap(from, to, cs, ovs.getWeight(mK));
//                if (!checker.checkOverlap(from, to, cs)) {
//                    if (ovs.size() >= 2) {
//                        badCh2++;
//                        System.err.println("Bad ov >=2: i = " + i);
//                    } else {
//                        badCh1++;
//                        System.err.println("Bad ov 1: i = " + i);
//                    }
//                }
//            }
            
            progress.updateDoneTasks(i + 1);
        }
        progress.destroyProgressBar();

//        System.err.println();
//        System.err.println("Bad choice (overlaps size = 1) = " + badCh1 + " = " +
//                String.format("%.2f", badCh1 * 100.0 / chs) + "% of all");
//        System.err.println("Bad choice (overlaps size >= 2) = " + badCh2 + " = " +
//                String.format("%.2f", badCh2 * 100.0 / chs) + "% of all");
//        System.err.println("All choice count = " + chs);

        
        repeatPStat.printToFile(workDir.append("repeat-probability-distribution").toString(), true);
        repeatP1Stat.printToFile(workDir.append("repeat-probability-1-distribution").toString(), true);
        repeatP2Stat.printToFile(workDir.append("repeat-probability-2-distribution").toString(), true);
//        repeatPBadStat.printToFile(workDir.append("repeat-probability-bad-distribution").toString(), true);
//        repeatP1BadStat.printToFile(workDir.append("repeat-probability-1-bad-distribution").toString(), true);
//        repeatP2BadStat.printToFile(workDir.append("repeat-probability-2-bad-distribution").toString(), true);
    }


    private void removeUnreliableOverlaps() {
        info("Removing unreliable overlaps...");

        Overlaps newOverlaps = new Overlaps(reads, true);
        long allOvs = 0, rmOvs = 0;
        int rmGood = 0, rmBad = 0;

        progress.setTotalTasks(reads.size());
        progress.createProgressBar();
        for (int i = 0; i < reads.size(); i++) {
            if (overlaps.isReadRemoved(i)) {
                newOverlaps.markReadRemoved(i);
                continue;
            }

            OverlapsList ovs = overlaps.getList(i);
            for (int k = 0; k < ovs.size(); k++) {
                int from = i;
                int to = ovs.getTo(k);
                int cs = ovs.getCenterShift(k);
                int w = ovs.getWeight(k);
                double p = w / 100.0;
                int bs = overlaps.centerShiftToBeginShift(from, to, cs);
//                int ovLen = overlaps.calculateOverlapLen(from, to, cs);

                if (p <= unreliableThreshold.get()) {
                    // should remove this overlap
                    // no adding to newOverlaps
                    rmOvs++;
//                    if (checker.checkOverlap(from, to, cs)) {
//                        rmGood++;
//                    } else {
//                        rmBad++;
//                    }
                } else {
                    newOverlaps.addOverlap(from, to, cs, w);
                }
                
                allOvs++;
            }

            progress.updateDoneTasks(i + 1);
        }
        progress.destroyProgressBar();

        System.err.println("Removed " + groupDigits(rmOvs) + " overlaps = " +
                String.format("%.2f", rmOvs * 100.0 / allOvs) + " % of all overlaps");
        System.err.println("Remaining overlaps = " + groupDigits(allOvs - rmOvs));

//        System.err.println("Removed " + rmGood + " good overlaps and " + rmBad + " bad overlaps");
        
        overlaps = newOverlaps;
    }



    QuantitativeStatistics<Integer> getQSFromHM(ArrayLong2IntHashMap hm) {
        QuantitativeStatistics<Integer> stat = new QuantitativeStatistics<Integer>();
        for (Long2IntOpenHashMap lm : hm.hm) {
            for (int v : lm.values()) {
                stat.add(v);
            }
        }
        return stat;
    }
    
    int getP(ReadsGenerator.ReadInfo info) {
        int[] genomePos = {2815793, 2816068};
        for (int i = 0; i < genomePos.length; i++) {
            int pos = genomePos[i];
            if (info.beginPos <= pos && info.beginPos + info.len > pos) {
                return i;
            }
            pos += 99 - 1;
            if (info.beginPos <= pos && info.beginPos + info.len > pos) {
                return i;
            }
        }
        return -1;
    }

    boolean shouldOutput(ReadsGenerator.ReadInfo info) {
        return getP(info) != -1;
    }

    private void calculateStatistics() throws FileNotFoundException {
        info("Calculating various statistics...");

        KmerIteratorFactory<Kmer> factory = new ShortKmerIteratorFactory();

        QuantitativeStatistics<Integer> meanDistr = new QuantitativeStatistics<Integer>();
        QuantitativeStatistics<Integer> varDistr = new QuantitativeStatistics<Integer>();

//        PrintWriter out = new PrintWriter(workDir.append("ordering.txt").get());

        List<Integer>[] ris = new List[2];
        ris[0] = new ArrayList<Integer>();
        ris[1] = new ArrayList<Integer>();

        long wrongOvs = 0, allOvs = 0;
        long vs = 0, allRs = 0, minSR = 0;

//        progress.progress.createProgressBar(reads.size());
        for (int i = 0; i < reads.size(); i++) {
            Dna d = reads.get(i);
//            ReadsGenerator.ReadInfo info = checker.getReadInfo(i);
            QuantitativeStatistics<Integer> coverage = new QuantitativeStatistics<Integer>();

            if (overlaps.isReadRemoved(i)) {
//                if (shouldOutput(info)) {
//                    System.out.println("read # " + i + " <= REMOVED");
//                }
                continue;
            }

//            if (shouldOutput(info)) {
//                if (!info.rc) {

//                    System.out.println("read # " + i + (info.rc ? " RC" : "   ") + "  \toverlaps.size = " + list.size());

//                    ris[getP(info)].add(i);
//                }

                /*
                out.println();
                out.println("Stat for read # " + i);
                out.println("dna = " + d);
//                out.println("RC_dna = " + d.reverseComplement());
                out.println("dna.len = " + d.length());
                out.println("begin = " + info.beginPos + ", end = " + (info.beginPos + info.len) + ", rc = " + info.rc);
                out.println("Kmers coverage:");
                */
//            }

            int c = 0;
            boolean bad = false;
            for (Kmer kmer : factory.kmersOf(d, k.get())) {

                int val = hm.get(kmer.toLong());


//                if (shouldOutput(info)) {
//                    out.print((c / 100 % 10) + "" + (c / 10 % 10) + "" + c%10 + " = ");
//                    for (int cc = 0; cc < c; cc++) {
//                        out.print(' ');
//                    }
//                    out.println(DnaTools.toString(kmer));
//                    c++;
//                    out.print(" " + val);

//                    if (Math.random() < 0.1) {
//                        System.err.print(c + ":\t val = " + val + ", \tkmer = " + DnaTools.toString(kmer) + ", ");
//                        long rcKmerL = ((ShortKmer) kmer).rcKmer();
//                        ShortKmer rcKmer = new ShortKmer(rcKmerL, kmer.length());
//                        int rcVal = hm.get(rcKmerL);
//                        System.err.println("RC: val = " + rcVal + ", \tkmer = " + DnaTools.toString(rcKmer));
//                    }
//                }

                if (val == 0) {
                    bad = true;
                }
                
                coverage.add(val);
            }


            double cov = coverage.calculateMean();
            meanDistr.add((int) cov);
            double var = coverage.calculateVariance();
            varDistr.add((int) var);


            // trying to remove all overlaps with not maximal overlap len
            OverlapsList ovs = overlaps.getList(i);
            int maxOvLen = -1, maxK = -1;
            if (ovs.size() > 1) {
                vs++;
                minSR += (ovs.size() - 1);
            }
            allRs++;
            for (int k = 0; k < ovs.size(); k++) {
                int from = i;
                int to = ovs.getTo(k);
                int cs = ovs.getCenterShift(k);
                int ovLen = overlaps.calculateOverlapLen(from, to, cs);

                allOvs++;
//                if (!checker.checkOverlap(from, to, cs)) {
//                    wrongOvs++;
//                }

                if (ovLen > maxOvLen) {
                    maxK = k;
                    maxOvLen = ovLen;
                }
//                    out.println(from + " " + to + " " + ovLen + " " + label);
            }

            if (ovs.size() != 0) {
                int from = i;
                int to = ovs.getTo(maxK);
                int cs = ovs.getCenterShift(maxK);

//                if (!checker.checkOverlap(from, to, cs)) {
//                }
            }



//            if (shouldOutput(info)) {
                /*
                System.out.println("read # " + i + (info.rc ? " RC" : "   ") + " " //", overlaps.size = " + list.size());
                        + "\tbegin = " + info.beginPos + "  \tend = " + (info.beginPos + info.len) +
                        "  \tcov = " + String.format("%.2f", cov) + "  \tvar = " + String.format("%.2f", var));
                out.println();
                out.println("cov = " + cov);
                out.println("var = " + var);
                out.println();
                */
//            }

//            progress.progress.updateProgressBar(i + 1);
        }
//        progress.progress.destroyProgressBar();

        System.err.println("Done, dumping...");
        
//        System.err.println("Wrong overlaps = " + wrongOvs + " = " +
//                String.format("%.2f", wrongOvs * 100.0 / allOvs) + " % of all overlaps");
        System.err.println("All overlaps = " + allOvs);
        System.err.println("Vs >=2 = " + vs + " = " +
                String.format("%.1f", vs * 100.0 / allRs) + " % of all vertexes");
        System.err.println("All vs = " + allRs);
        System.err.println("minSR = " + minSR + " = " +
                String.format("%.2f", minSR * 100.0 / allOvs) + " % of all overlaps");



        meanDistr.printToFile(workDir.append("coverage-distribution").toString());
        varDistr.printToFile(workDir.append("coverage-var-distribution").toString());

        /*
        Comparator<Integer> comparator = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                ReadsGenerator.ReadInfo ri1 = checker.getReadInfo(o1);
                ReadsGenerator.ReadInfo ri2 = checker.getReadInfo(o2);
                int c1 = ri1.beginPos + (ri1.beginPos + ri1.len - 1);
                int c2 = ri2.beginPos + (ri2.beginPos + ri2.len - 1);
                if (c1 != c2) {
                    return c1 - c2;
                }
                return o1 - o2;
            }
        };
        Collections.sort(ris[0], comparator);
        Collections.sort(ris[1], comparator);

//        int fromX = 388604;
//        ReadsGenerator.ReadInfo ri = checker.getReadInfo(fromX);
//        out.println(fit("Genome:", 20) + multiply(' ', 50) + checker.getGoodDNAString(ri));

//        OverlapsList ovs = overlaps.getList(fromX);
//        for (int k = 0; k < ovs.size(); k++) {
//            int from = fromX;
//            int to = ovs.getTo(k);
//            int cs = ovs.getCenterShift(k);
//            int bs = overlaps.centerShiftToBeginShift(from, to, cs);
//            int ovLen = overlaps.calculateOverlapLen(from, to, cs);
//            ReadsGenerator.ReadInfo riTo = checker.getReadInfo(to);
//
//            int cl = 50 + bs;
//            out.println(fit("" + to, 20) + multiply(' ', cl) + checker.getGoodDNAString(riTo));
//        }


        for (int i = 0; i < 2; i++) {
//            System.out.println();
//            System.out.println(i + ":");
            for (int n : ris[i]) {
                ReadsGenerator.ReadInfo info = checker.getReadInfo(n);

                int c = info.beginPos + (info.beginPos + info.len - 1);
//                System.out.println("read # " + n + (info.rc ? " RC" : "   ") + " " //", overlaps.size = " + list.size());
//                        + "\tbegin = " + info.beginPos + "  \tend = " + (info.beginPos + info.len));


                
                
                /*
                int c = info.beginPos + (info.beginPos + info.len - 1);
                int cl = 0;
                if (getP(info) == 0) {
                    cl = 250 - (2815793 - info.beginPos);
                } else {
                    cl = 250 - (2816068 - info.beginPos);
                }
                out.println(fit("" + n, 20) + multiply(' ', cl) +
                        checker.genome.substring(info.beginPos, info.beginPos + info.len));
                        */
        /*    }
//            System.out.println();
        }
        */
        
//        out.close();

        coverageDistr = meanDistr;
    }

    public static void countWrongOverlaps(Overlaps overlaps, CheckerFromRef checker, boolean[] goodVs) {
        int vc = 0, vcOvs2 = 0;
        int allOvs = 0, badOvs = 0;
        int badOvs1 = 0;
        int badOvs2C = 0, badOvs2CV = 0;
        int badOvs2CN = 0, badOvs2CNV = 0;
        for (int i = 0; i < overlaps.readsNumber; i++) {
            if (overlaps.isReadRemoved(i) || (goodVs != null && !goodVs[i])) {
                continue;
            }

            OverlapsList ovs = overlaps.getList(i);

            int allOvsH = 0, goodOvsH = 0, badOvsH = 0;
            for (int j = 0; j < ovs.size(); j++) {
                int from = i;
                int to = ovs.getTo(j);
                int cs = ovs.getCenterShift(j);

                if (overlaps.isReadRemoved(to) || (goodVs != null && !goodVs[to])) {
                    continue;
                }

                allOvsH++;
                if (checker.checkOverlap(from, to, cs)) {
                    goodOvsH++;
                } else {
                    badOvsH++;
                }
            }

            allOvs += allOvsH;
            badOvs += badOvsH;
            if (badOvsH > 0) {
                if (allOvsH == 1) {
                    badOvs1++;
//                    System.err.println("Bad ov 1: i = " + i);
                } else {
                    if (goodOvsH > 0) {
                        badOvs2C += badOvsH;
                        badOvs2CV++;
//                        System.err.println("Bad ov 2 CAN: i = " + i);
                    } else {
                        badOvs2CN += badOvsH;
                        badOvs2CNV++;
                    }
                }
            }

            vc++;
            if (allOvsH >= 2) {
                vcOvs2++;
            }
        }
        System.err.println("Bad overlaps = " + badOvs + " = " +
                String.format("%.1f", badOvs * 100.0 / allOvs) + " % of all overlaps");
        System.err.println("Bad overlaps 1 = " + badOvs1);
        System.err.println("Bad overlaps >= 2 CAN = " + badOvs2C + ", vertexes = " + badOvs2CV);
        System.err.println("Bad overlaps >= 2 CAN'T = " + badOvs2CN + ", vertexes = " + badOvs2CNV);
        System.err.println("All overlaps = " + allOvs);
        System.err.println("Vertexes = " + vc + ", vertexes with ovs >= 2 = " + vcOvs2);
    }




    @Override
    protected void cleanImpl() {
        gatherer = null;
        hm = null;
        reads = null;
    }

    public GraphSimplification() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new GraphSimplification().mainImpl(args);
    }
}
