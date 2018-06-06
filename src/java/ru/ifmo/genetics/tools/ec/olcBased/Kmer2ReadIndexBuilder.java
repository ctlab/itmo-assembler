package ru.ifmo.genetics.tools.ec.olcBased;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import ru.ifmo.genetics.io.MultiFile2MemoryMap;
import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.tools.io.LazyLongReader;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Kmer2ReadIndexBuilder extends Tool {
    public static final String NAME = "kmer2read-index-builder";
    public static final String DESCRIPTION = "builds index ...";

    // input parameters
    public final Parameter<Integer> anchorLen = addParameter(new IntParameterBuilder("anchor-length")
            .mandatory()
            .withShortOpt("a")
            .withDescription("anchor length")
            .create());

    public final Parameter<Integer> maximalErrorsNumber = addParameter(new IntParameterBuilder("max-errors-number")
            .withShortOpt("e")
            .withDefaultValue(12)
            .withDescriptionShort("Maximal errors")
            .withDescription("maximal errors number per read")
            .withDescriptionRuShort("Макс. число ошибок")
            .withDescriptionRu("Максимальное число ошибок на чтение")
            .create());

    public final Parameter<File> chainFile = addParameter(new FileParameterBuilder("chain-file")
            .mandatory()
            .withShortOpt("ch")
            .withDescription("chain file")
            .create());

    public final Parameter<File[]> goodKmerFiles = addParameter(new FileMVParameterBuilder("good-kmer-files")
            .mandatory()
            .withShortOpt("g")
            .withDescription("good kmer files")
            .create());

    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("input-files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("reads to process")
            .create());


    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .withDefaultValue(workDir.append("corrected"))
            .withShortOpt("o")
            .withDescription("directory for output files")
            .create());

    public final Parameter<File> outputIndexFile = addParameter(new FileParameterBuilder("output-index-file")
            .withDefaultValue(workDir.append("index"))
            .withDescription("output index file")
            .create());


    // internal variables
    private List<LongList> chains;
    private Long2IntMap kmer2chain;
    private Long2IntMap kmer2ind;
    private Long2IntMap kmers;
    private Kmer2ReadIndexBuilderWorker[] workers;
    private Long2IntMap times;

    // output parameters
    private final InMemoryValue<File[]> resultingReadsOutValue = new InMemoryValue<File[]>();
    public final InValue<File[]> resultingReadsOut = addOutput("resulting-reads", resultingReadsOutValue, File[].class);


    static long corrected = 0;
    static long uncorrected = 0;
    static long readsChanged = 0;
    static long readsSkipped = 0;
    static long readsProcessed = 0;
    static long kmersProcessed = 0;


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            info("Loading data...");
            loadChains();
            loadKmers();

            File[] fs = FileUtils.copyFiles(inputFiles.get(), outputDir.get());
            inputFiles.set(fs);

            buildIndex();
            dumpIndex();

            correct();
            printStatistics();

            resultingReadsOutValue.set(fs);

        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
    }


    void loadChains() throws IOException {
        DataInputStream chainReader = new DataInputStream(new BufferedInputStream(new FileInputStream(chainFile.get())));
        chains = new ArrayList<LongList>();
        kmer2chain = new Long2IntOpenHashMap();
        kmer2ind = new Long2IntOpenHashMap();
        while (true) {
            try {
                int size = chainReader.readInt();
                LongList list = new LongArrayList();
                for (int q = 0; q < size; ++q) {
                    long kmer = chainReader.readLong();
                    list.add(kmer);
                    kmer2chain.put(kmer, chains.size());
                    kmer2ind.put(kmer, q);
                    // System.err.print(KmerUtils.kmer2String(kmer, len) + " ");
                }
                // System.err.println();
                chains.add(list);
            } catch (EOFException e) {
                break;
            }
        }
        info(chains.size() + " chains loaded");
    }

    void loadKmers() throws IOException {
        LazyLongReader reader = new LazyLongReader(goodKmerFiles.get());
        kmers = new Long2IntOpenHashMap();
        while (true) {
            try {
                long kmer = reader.readLong();
                kmers.put(kmer, 0);
            } catch (EOFException e) {
                break;
            }
        }
        int i = 0;
        for (long l : kmers.keySet()) {
            kmers.put(l, i++);
        }
        info(kmers.size() + " kmers loaded");
    }

    void buildIndex() throws IOException {
        info("Building index...");
        Kmer2ReadIndexBuilderDispatcher dispatcher = new Kmer2ReadIndexBuilderDispatcher(inputFiles.get(), 1 << 18);
        workers = new Kmer2ReadIndexBuilderWorker[availableProcessors.get()];
        CountDownLatch latch = new CountDownLatch(workers.length);
        for (int i = 0; i < workers.length; ++i) {
            workers[i] = new Kmer2ReadIndexBuilderWorker(kmers, chains, kmer2chain, kmer2ind, anchorLen.get(), chains.size(), dispatcher, latch);
            new Thread(workers[i]).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.err.println("interrupted");
            for (Kmer2ReadIndexBuilderWorker worker : workers) {
                worker.interrupt();
            }
            System.exit(1);
        }
    }

    void dumpIndex() throws IOException {
        info("Dumping index...");
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputIndexFile.get())));
        for (int i = 0; i < chains.size(); ++i) {
            int size = 0;
            for (Kmer2ReadIndexBuilderWorker worker : workers) {
                if (worker.index[i] != null) {
                    size += worker.index[i].size();
                }
            }
            if (size == 0) {
                continue;
            }
            out.writeInt(i);
            out.writeInt(size);
            for (Kmer2ReadIndexBuilderWorker worker : workers) {
                if (worker.index[i] != null) {
                    out.write(worker.index[i].toByteArray());
                }
            }
        }
        for (int i = 0; i < workers.length; ++i) {
            workers[i] = null;
        }
        out.close();
    }

    void correct() throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(outputIndexFile.get())));

        info("Loading reads...");
        MultiFile2MemoryMap mf = new MultiFile2MemoryMap(inputFiles.get());

        info("Starting correction...");

        progress.setTotalTasks(chains.size());
        progress.createProgressBar();

        NewCleanDispatcher cleanDispatcher = new NewCleanDispatcher(in, 1000, mf, progress);
        NewCleanWorker[] cleanWorkers = new NewCleanWorker[availableProcessors.get()];
        CountDownLatch cleanLatch = new CountDownLatch(cleanWorkers.length);
        //        RandomAccessMultiFile mf = new RandomAccessMultiFile(readFiles, "rw");
        times = new Long2IntOpenHashMap();
        Timer t = new Timer();
        for (int i = 0; i < cleanWorkers.length; ++i) {
            cleanWorkers[i] = new NewCleanWorker(cleanLatch, cleanDispatcher, chains, kmer2chain, kmer2ind, mf, anchorLen.get(), times, maximalErrorsNumber.get());
            new Thread(cleanWorkers[i]).start();
        }
        try {
            cleanLatch.await();
        } catch (InterruptedException e) {
            for (NewCleanWorker worker : cleanWorkers) {
                worker.interrupt();
            }
            throw new RuntimeException(e);
        }

        progress.destroyProgressBar();
        info("Finished, processed " + kmersProcessed + " chains in " + t);
        info("Total skipped: " + uncorrected);
        info("Dumping...");
        mf.dump();
    }

    void printStatistics() {
        long totalTimes = 0;
        int minTimes = Integer.MAX_VALUE;
        int maxTimes = 0;
        for (int t : times.values()) {
            totalTimes += t;
            minTimes = Math.min(minTimes, t);
            maxTimes = Math.max(maxTimes, t);
        }

        debug("min times : " + minTimes);
        debug("mean times: " + totalTimes / times.size());
        debug("max times : " + maxTimes);
    }




    @Override
    protected void cleanImpl() {
        chains = null;
        kmer2chain = null;
        kmer2ind = null;
        kmers = null;
        workers = null;
        times = null;
    }

    public Kmer2ReadIndexBuilder() {
        super(NAME, DESCRIPTION);
    }

}
