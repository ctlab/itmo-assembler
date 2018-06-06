package ru.ifmo.genetics.tools.ec.simple;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashBigSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import ru.ifmo.genetics.structures.map.ArrayLong2LongHashMap;
import ru.ifmo.genetics.tools.io.LazyLongReader;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.KmerUtils;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.*;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class Cleaner extends Tool {
    public static final String NAME = "cleaner";
    public static final String DESCRIPTION = "performs error correction";

    public final Parameter<Integer> k = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k")
            .create());

    public final Parameter<Integer> maximalIndelsNumber = addParameter(new IntParameterBuilder("maximal-indels-number")
            .mandatory()
            .withDescription("maximal indels number")
            .create());

    public final Parameter<Integer> maximalSubsNumber = addParameter(new IntParameterBuilder("maximal-subs-number")
            .mandatory()
            .withDescription("maximal substitutions number")
            .create());

    public final Parameter<String> prefixParameter = addParameter(new StringParameterBuilder("prefix")
            .withDefaultValue("")
            .withDescription("prefix")
            .create());

    public final Parameter<File> kmersDir = addParameter(new FileParameterBuilder("kmers-dir")
            .withDefaultValue(workDir.append("kmers"))
            .withDescription("directory with good and bad kmers files")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .withDefaultValue(workDir.append("fixes"))
            .withShortOpt("o")
            .withDescription("directory for fixes")
            .create());

    public final Parameter<Long> badKmersNumber = addParameter(new LongParameterBuilder("bad-kmers-number")
            .mandatory()
            .withDescription("the number of bad kmers")
            .create());

    private InMemoryValue<File[]> fixesFilesOutValue = new InMemoryValue<File[]>();
    public InValue<File[]> fixesFilesOut = addOutput("fixes-files", fixesFilesOutValue, File[].class);

    int LEN;
    long MASK;
    int DISPATCH_WORK_RANGE_SIZE = 1 << 10;
    int CLEAN_WORK_THREADS_NUMBER;
    int MAXIMAL_SUBS_NUMBER;
    int MAXIMAL_INDELS_NUMBER;
    String prefix;
    File DIR;
    long bad;

    @Override
    protected void runImpl() throws ExecutionFailedException {
        LEN = k.get();
        MASK = (1L << (2 * LEN)) - 1;
        CLEAN_WORK_THREADS_NUMBER = availableProcessors.get();
        MAXIMAL_INDELS_NUMBER = maximalIndelsNumber.get();
        MAXIMAL_SUBS_NUMBER = maximalSubsNumber.get();
        prefix = prefixParameter.get();
        DIR = kmersDir.get();
        outputDir.get().mkdir();
        bad = badKmersNumber.get();

        try {
            runAndGetResults(bad);
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            throw new ExecutionFailedException(e);
        }

    }

    @Override
    protected void cleanImpl() {
    }

    public Cleaner() {
        super(NAME, DESCRIPTION);
    }

    public Cleaner(String prefix) {
        super(NAME + "_" + prefix, DESCRIPTION);
    }

    public static void main(String[] args) {
        new Cleaner().mainImpl(args);
    }

    private ArrayLong2LongHashMap[] clean(LongSet goodKMers, LazyLongReader reader, long bad)
            throws FileNotFoundException, EOFException
    {

        CleanDispatcher dispatcher = new CleanDispatcher(reader, DISPATCH_WORK_RANGE_SIZE, bad, LEN,
                CLEAN_WORK_THREADS_NUMBER, progress);

        CleanWorker[] workersPool = new CleanWorker[CLEAN_WORK_THREADS_NUMBER];

        CountDownLatch latch = new CountDownLatch(workersPool.length);

        for (int i = 0; i < CLEAN_WORK_THREADS_NUMBER; ++i) {
            workersPool[i] = new CleanWorker(dispatcher, goodKMers, LEN, latch,
                    MAXIMAL_SUBS_NUMBER, MAXIMAL_INDELS_NUMBER);
            new Thread(workersPool[i]).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            warn("Main thread interrupted");
            for (CleanWorker worker : workersPool) {
                worker.interrupt();
            }
        }
        progress.destroyProgressBar();

        ArrayLong2LongHashMap[] maps = new ArrayLong2LongHashMap[workersPool.length];
        int i = 0;
        for (CleanWorker worker : workersPool) {
            maps[i++] = worker.getResults();
        }
        return maps;
    }

    public long kmersNumberInFiles(String suffix, List<String> prefixes) throws IOException {
        ArrayList<String> fileNames = new ArrayList<String>(prefixes.size());
        for (String prefix: prefixes) {
            fileNames.add(DIR.getAbsolutePath() + File.separator + "kmers" + prefix + suffix);
        }
        long toLoad = FileUtils.filesSizeByNames(fileNames) / 8;
        return toLoad;
    }

    public void loadKMers(String suffix, List<String> prefixes, LongCollection c) throws IOException {
        if (prefixes == null) {
            prefixes = new ArrayList<String>();
            prefixes.add("");
        }
        ArrayList<String> fileNames = new ArrayList<String>(prefixes.size());
        for (String prefix: prefixes) {
            fileNames.add(DIR + File.separator + "kmers" +  prefix + suffix);
        }

        for (String fileName : fileNames) {
            info("loading k-mers from " + fileName);
            long k = 0;
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
            while (true) {
                try {
                    long l = in.readLong();
                    c.add(l);
                    c.add(KmerUtils.reverseComplement(l, LEN));
                    k += 2;
                } catch (EOFException e) {
                    break;
                }
            }

            debug("added " + k + " kmers to set, set size = " + c.size());
        }
    }

    private ArrayLong2LongHashMap[] runAndGetResults(long bad) throws IOException, InterruptedException {
        List<String> prefixes = Arrays.asList(new String[]{prefix});

        info("prefixes: \"" + prefix + "\"");

        long kmersNumber = 2 * kmersNumberInFiles(".good", prefixes);
        debug("kmers to load = " + kmersNumber/2);
        LongSet goodKMers;
        double expHashSetSize = kmersNumber / Hash.DEFAULT_LOAD_FACTOR;
        debug("hash set size >= " + String.format("%.1f", expHashSetSize));
        if (expHashSetSize >= (1 << 30) ) {
            goodKMers = new LongOpenHashBigSet(kmersNumber);
        } else {
            goodKMers = new LongOpenHashSet((int)kmersNumber);
        }
        loadKMers(".good", prefixes, goodKMers);

        String[] badKmersFiles = new String[prefixes.size()];
        for (int i = 0; i < prefixes.size(); ++i) {
            badKmersFiles[i] = DIR + File.separator + "kmers" + prefixes.get(i) + ".bad";
        }
        LazyLongReader reader = new LazyLongReader(badKmersFiles);

        info("starting error correction...");
        ArrayLong2LongHashMap[] fixes = clean(goodKMers, reader, bad);

        info("dumping fixes");
        long totalFixes = dumpFixes(fixes, outputDir.get().getAbsolutePath() + File.separator + "prefix" + Misc.join(prefixes, "_") + ".fixes");
        info("dumped " + totalFixes + " fixes");

        return fixes;
    }

    private long dumpFixes(ArrayLong2LongHashMap fixes, String filename) throws IOException {
        return dumpFixes(fixes, filename, false);
    }

    private long dumpFixes(ArrayLong2LongHashMap fixes, String filename, boolean append) throws IOException {
        long total = 0;

        debug("dumping " + fixes.size() + " fixes to " + filename);
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename, append)));
        for (LongSet keySet: fixes.keySets()) {
            for (long key : keySet) {
                out.writeLong(key);
                out.writeLong(fixes.get(key));
                ++total;
            }
        }
        out.close();

        fixesFilesOutValue.set(new File[]{new File(filename)});
        return total;
    }

    private long dumpFixes(ArrayLong2LongHashMap[] fixes, String filename) throws IOException {
        long total = 0;

        int i = 0;
        for (ArrayLong2LongHashMap map : fixes) {
            total += dumpFixes(map, filename, i != 0);
            ++i;
        }
        return total;
    }

}
