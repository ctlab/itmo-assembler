package ru.ifmo.genetics.transcriptome;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.kmers.KmerIteratorFactory;
import ru.ifmo.genetics.dna.kmers.ShortKmerIteratorFactory;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.sources.Source;
import ru.ifmo.genetics.structures.map.ArrayLong2IntHashMap;
import ru.ifmo.genetics.tools.ec.DnaQReadDispatcher;
import ru.ifmo.genetics.tools.ec.KmerLoadWorker;
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

public class TranscriptomeKmerStatisticsGatherer extends Tool {
    public static final String NAME = "kmer-statistics-gatherer";
    public static final String DESCRIPTION = "differentiates good kmers from bad ones";

    static final int LOAD_TASK_SIZE = 1 << 15;

    public final Parameter<Integer> maximalBadFrequence = addParameter(new IntParameterBuilder("maximal-bad-frequence")
            .optional()
            .withShortOpt("b")
            .withDescription("maximal frequency for a kmer to be assumed erroneous")
            .create());

    public final Parameter<Integer> k = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size")
            .create());

    public final Parameter<Long> maxSize = addParameter(new LongParameterBuilder("max-size")
            .optional()
            .withDescription("maximal hashset size")
            .withDefaultValue(NumUtils.highestBits(Misc.availableMemory() / 42, 3))
            .create());

    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("reads")
            .mandatory()
            .withDescription("list of input files")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .withShortOpt("o")
            .withDescription("directory to place output files")
            .withDefaultValue(workDir.append("kmers"))
            .create());

    public final Parameter<File> prefixesFile = addParameter(new FileParameterBuilder("prefixes-file")
            .withDescription("file with prefixes")
            .withDefaultValue(workDir.append("prefixes"))
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

    private int prefixLength;
    private long prefixMask;

    private int LEN;
    private long MASK;
    private long MAX_SIZE;

    private long totalGood = 0;
    private long totalBad = 0;

    private final static Random random = new Random(1989);

    @Override
    protected void runImpl() throws ExecutionFailedException {

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

        int threshold = 1; //TODO

        info("threshold = " + threshold);
        if (prefixLength != 0) {
            hm = null;
        }

        long maxPrefix = 1L << (2 * prefixLength);
        String workdir = outputDir.get().getAbsolutePath();
        try {
            PrintWriter pw = new PrintWriter(prefixesFile.get());
            for (long cp = 0; cp < maxPrefix; ++cp) {

                long curPrefix = cp << (2 * (LEN - prefixLength));
                pw.println(Misc.getString(cp, prefixLength));
                info("processing prefix: \"" + Misc.getString(cp, prefixLength) + "\"");

                ArrayLong2IntHashMap kmers = (prefixLength == 0) ? hm : load(inputFiles.get(), Long.MAX_VALUE, curPrefix, prefixMask, prefixLength);
                debug("loaded " + kmers.size() + " kmers");

                String goodFile = workdir + File.separator + "kmers" + Misc.getString(cp, prefixLength) + ".good";
                String badFile = workdir + File.separator + "kmers" + Misc.getString(cp, prefixLength) + ".bad";
                dumpKmers(kmers.hm, threshold, goodFile, badFile);

                debug(Misc.getString(cp, prefixLength) + " done");
            }
            pw.close();
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
        info("total good kmers: " + totalGood);
        info("total bad kmers:  " + totalBad);
        badKmersNumberOutValue.set(totalBad);

    }

    @Override
    protected void cleanImpl() {
    }

    public static void main(String[] args) {
        new TranscriptomeKmerStatisticsGatherer().mainImpl(args);
    }

    public TranscriptomeKmerStatisticsGatherer() {
        super(NAME, DESCRIPTION);
    }

    ArrayLong2IntHashMap load(File[] files, long maxSize) throws IOException {
        return load(files, maxSize, 0, 0, 0);
    }

    ArrayLong2IntHashMap load(File[] files, long maxSize, long prefix, long prefixMask, int prefixLength) throws IOException {

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
        info("loaded");

        //this.prefix = workers[0].getPrefix();
        this.prefixMask = workers[0].getPrefixMask();
        this.prefixLength = workers[0].getPrefixLength();

        readsNumberOutValue.set(dispatcher.getReads());

        return hm;
    }

    void dumpKmers(Long2IntMap[] hm, int threshold, String goodFile, String badFile) throws IOException {
        DataOutputStream good = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(goodFile)));
        //DataOutputStream bad = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(badFile)));
        for (Long2IntMap map : hm) {
            for (long key : map.keySet()) {
                if (map.get(key) <= threshold) {
          //          bad.writeLong(key);
            //        bad.writeInt(map.get(key));
                    ++totalBad;
                } else {
                    good.writeLong(key);
                    good.writeInt(map.get(key));
                    ++totalGood;
                }
            }
        }
        good.close();
        //bad.close();
    }

}


