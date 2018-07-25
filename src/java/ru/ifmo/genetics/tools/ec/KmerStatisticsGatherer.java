package ru.ifmo.genetics.tools.ec;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import org.apache.hadoop.io.IOUtils;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.kmers.KmerIteratorFactory;
import ru.ifmo.genetics.dna.kmers.ShortKmerIteratorFactory;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.sources.Source;
import ru.ifmo.genetics.structures.map.ArrayLong2IntHashMap;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.NumUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.*;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.*;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class KmerStatisticsGatherer extends Tool {
    public static final String NAME = "kmer-statistics-gatherer";
    public static final String DESCRIPTION = "differentiates good kmers from bad ones";

    static final int LOAD_TASK_SIZE = 1 << 15;

    public final Parameter<Integer> maximalBadFrequency = addParameter(new IntParameterBuilder("maximal-bad-frequency")
            .withShortOpt("b")
            .withDefaultValue(-1)
//            .withDefaultComment("-1 (auto)")
            .withDescriptionShort("Maximal bad frequency")
            .withDescription("maximal frequency for a k-mer to be assumed erroneous (-1 = auto, 0 = all k-mers are good)")
            .withDescriptionRuShort("Макс. частота ошибочного k-mer'а")
            .withDescriptionRu("Максимальная частота k-mer'a для принятия его как ошибочного (в исправлении ошибок)")
            .create());

    public final Parameter<Integer> k = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size")
            .create());

    public final Parameter<Long> maxSize = addParameter(new LongParameterBuilder("max-size")
            .withDescription("maximal hashset size")
            .withDefaultValue(NumUtils.highestBits(Misc.availableMemory() / 52, 4))
            .memoryParameter()
            .create());

    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("reads")
            .mandatory()
            .withShortOpt("i")
            .withDescription("list of input files")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .withShortOpt("o")
            .withDescription("directory to place output files")
            .withDefaultValue(workDir.append("kmers"))
            .create());

    public final Parameter<File> outputPrefixesFile = addParameter(new FileParameterBuilder("output-prefixes-file")
            .withDescription("output file with prefixes")
            .withDefaultValue(workDir.append("prefixes"))
            .create());

    public final Parameter<Boolean> ignoreBadKmers = addParameter(new BoolParameterBuilder("ignore-bad-kmers")
            .withDescription("not outputs bad kmers")
            .withDefaultValue(false)
            .create());

    public final Parameter<Boolean> outputCounts = addParameter(new BoolParameterBuilder("output-kmer-counts")
            .withDescription("outputs kmer counts")
            .withDefaultValue(false)
            .create());

    public final Parameter<KmerIteratorFactory> kmerIteratorFactory = Parameter.createParameter(
            new KmerIteratorFactoryParameterBuilder("kmer-iterator-factory")
                    .optional()
                    .withDescription("factory used for iterating through kmers")
                    .withDefaultValue(new ShortKmerIteratorFactory())
                    .create());

    private final InMemoryValue<Long> badKmersNumberOutValue = new InMemoryValue<Long>();
    public final InValue<Long> badKmersNumberOut = addOutput("bad-kmers-number", badKmersNumberOutValue, Long.class);

    private final InMemoryValue<Long> readsNumberOutValue = new InMemoryValue<Long>();
    public final InValue<Long> readsNumberOut = addOutput("reads-number", readsNumberOutValue, Long.class);

    private final InMemoryValue<File[]> goodKmersFilesOutValue = new InMemoryValue<File[]>();
    public final InValue<File[]> goodKmersFilesOut = addOutput("good-kmers-files", goodKmersFilesOutValue, File[].class);

    private final InMemoryValue<File[]> badKmersFilesOutValue = new InMemoryValue<File[]>();
    public final InValue<File[]> badKmersFilesOut = addOutput("bad-kmers-files", badKmersFilesOutValue, File[].class);

    private int prefixLength;
    private long prefix;
    private long prefixMask;

    private int LEN;
    private long MASK;
    private long MAX_SIZE;

    private long totalGood = 0;
    private long totalBad = 0;

    private final static Random random = new Random(1989);

    @Override
    protected void runImpl() throws ExecutionFailedException {
        if (k.get() <= 0) {
            error("The size of k-mer must be at least 1.");
            System.exit(1);
        }
        if (k.get() > 31) {
            error("Maximum value of k-mer size is 31.");
            System.exit(1);
        }


        LEN = k.get();
        MASK = (1L << (2 * LEN)) - 1;
        MAX_SIZE = maxSize.get();
        outputDir.get().mkdir();

        debug("MAXIMAL_SIZE = " + MAX_SIZE);

        ArrayLong2IntHashMap hm = null;
        try {
            hm = load(inputFiles.get(), MAX_SIZE);
        } catch (IOException e) {
            throw new ExecutionFailedException("Couldn't load kmers", e);
        }
        if (hm.size() == 0) {
            error("No k-mers found in input reads. " +
                    "Please check k-mer size parameter and/or reads' length distribution.");
            System.exit(1);
        }
        debug("hm.size() = " + NumUtils.groupDigits(hm.size()));
        long totalKmers = hm.size();

        int[] stat = new int[256];
        for (int i = 0; i < hm.hm.length; ++i) {
            for (long key : hm.hm[i].keySet()) {
                int b = hm.hm[i].get(key);
                if (b >= stat.length) {
                    b = stat.length - 1;
                }
                ++stat[b];
            }
        }

        try {
            dumpStat(stat, workDir + File.separator + "distribution");
        } catch (FileNotFoundException e) {
            throw new ExecutionFailedException(e);
        }

        int threshold = maximalBadFrequency.get();
        if (threshold == -1) {
            // searching second peak
            long skipped = stat[1];
            for (int i = 2; (i <= 20) && (skipped / totalKmers <= 0.80); ++i) {
                // i.e. allow to discard no more than 80% of all k-mers
                if ((stat[i - 1] >= stat[i]) && (stat[i] < stat[i + 1])) {
                    threshold = i - 1;
                    break;
                }
                skipped += stat[i];
            }
        }
        if (threshold == -1) {
            threshold = 1;
        }
        info("Threshold = " + threshold);
        if (prefixLength != 0) {
            hm = null;
        }

        long maxPrefix = 1L << (2 * prefixLength);
        String workdir = outputDir.get().getAbsolutePath();

        goodKmersFilesOutValue.set(new File[(int)maxPrefix]);
        badKmersFilesOutValue.set(new File[(int)maxPrefix]);

        try {
            PrintWriter pw = new PrintWriter(outputPrefixesFile.get());
            for (long cp = 0; cp < maxPrefix; ++cp) {

                long curPrefix = cp << (2 * (LEN - prefixLength));
                pw.println(Misc.getString(cp, prefixLength));
                info("Processing prefix: \"" + Misc.getString(cp, prefixLength) + "\"");

                ArrayLong2IntHashMap kmers = (prefixLength == 0) ? hm : load(inputFiles.get(), Long.MAX_VALUE, curPrefix, prefixMask, prefixLength);
                debug("Loaded " + kmers.size() + " kmers");

                String goodFile = workdir + File.separator + "kmers" + Misc.getString(cp, prefixLength) + ".good";
                goodKmersFilesOutValue.get()[(int)cp] = new File(goodFile);
                String badFile = workdir + File.separator + "kmers" + Misc.getString(cp, prefixLength) + ".bad";
                badKmersFilesOutValue.get()[(int)cp] = new File(badFile);
                dumpKmers(kmers.hm, threshold, goodFile, badFile);

                debug(Misc.getString(cp, prefixLength) + " done");
            }
            pw.close();
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
        info(String.format("Total bad kmers : %12s", NumUtils.groupDigits(totalBad)));
        info(String.format("Total good kmers: %12s", NumUtils.groupDigits(totalGood)));
        badKmersNumberOutValue.set(totalBad);

    }

    @Override
    protected void cleanImpl() {
    }

    public static void main(String[] args) {
        new KmerStatisticsGatherer().mainImpl(args);
    }

    public KmerStatisticsGatherer() {
        super(NAME, DESCRIPTION);
    }

    ArrayLong2IntHashMap load(File[] files, long maxSize) throws IOException {
        return load(files, maxSize, 0, 0, 0);
    }

    ArrayLong2IntHashMap load(File[] files, long maxSize, long prefix, long prefixMask, int prefixLength) throws IOException {
        info("Loading input reads...");

        if (files.length == 0) {
            error("No input files provided!", new IllegalArgumentException());
            System.exit(1);
        }
        
        ArrayLong2IntHashMap hm = new ArrayLong2IntHashMap((int)(Math.log(availableProcessors.get()) / Math.log(2)) + 4);
        Source<DnaQ> reader = ReadersUtils.readDnaQLazy(files);

        DnaQReadDispatcher dispatcher = new DnaQReadDispatcher(reader, LOAD_TASK_SIZE, progress);
        KmerLoadWorker[] workers = new KmerLoadWorker[availableProcessors.get()];
        CountDownLatch latch = new CountDownLatch(workers.length);

        for (int i = 0; i < workers.length; ++i) {
            workers[i] = new KmerLoadWorker(dispatcher, latch, new Random(42),
                    LEN, maxSize, hm, prefix, prefixMask, prefixLength, kmerIteratorFactory.get());
            new Thread(workers[i]).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            warn("Main thread interrupted");
            for (KmerLoadWorker worker : workers) {
                worker.interrupt();
            }
        }

        this.prefix = workers[0].prefix;
        this.prefixMask = workers[0].prefixMask;
        this.prefixLength = workers[0].prefixLength;

        readsNumberOutValue.set(dispatcher.reads);

        return hm;
    }

    void dumpKmers(Long2IntMap[] hm, int threshold, String goodFile, String badFile) throws IOException {
        DataOutputStream good = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(goodFile)));
        DataOutputStream bad = new DataOutputStream(new IOUtils.NullOutputStream());
        if (!ignoreBadKmers.get()) {
            bad = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(badFile)));
        }
        boolean outputValues = outputCounts.get();
        for (Long2IntMap map : hm) {
            for (Long2IntMap.Entry e: map.long2IntEntrySet()) {
                DataOutputStream out;
                if (e.getIntValue() <= threshold) {
                    out = bad;
                    ++totalBad;
                } else {
                    out = good;
                    ++totalGood;
                }
                out.writeLong(e.getLongKey());
                if (outputValues) {
                    out.writeInt(e.getIntValue());
                }
            }
        }
        good.close();
        bad.close();
    }

    void dumpStat(int[] stat, String filename) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(filename);
        for (int i = 1; i < stat.length; ++i) {
            pw.println(i + " " + stat[i]);
        }
        pw.close();
    }

}


