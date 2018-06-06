package ru.ifmo.genetics.tools.ec.simple;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.sources.Source;
import ru.ifmo.genetics.structures.map.ArrayLong2LongHashMap;
import ru.ifmo.genetics.tools.ec.DnaQReadDispatcher;
import ru.ifmo.genetics.tools.io.LazyLongReader;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.LongParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.*;
import java.util.concurrent.CountDownLatch;

public class FixesApplier extends Tool {
    public static final String NAME = "fixes-applier";
    public static final String DESCRIPTION = "applies fixes";

    public final Parameter<Integer> k = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size")
            .create());

    public final Parameter<File[]> reads = addParameter(new FileMVParameterBuilder("reads")
            .mandatory()
            .withDescription("list of input files containing reads")
            .create());

    public final Parameter<File[]> fixes = addParameter(new FileMVParameterBuilder("fixes")
            .mandatory()
            .withDescription("list of input files containing fixes")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .withDefaultValue(workDir.append("corrected"))
            .withDescription("directory for output files")
            .create());

    public final Parameter<Long> readsNumber = addParameter(new LongParameterBuilder("reads-number")
            .mandatory()
            .withDescription("the number of reads")
            .create());

    int DISPATCH_WORK_RANGE_SIZE = 1 << 15;
    int len = 0;

    long dels = 0;
    long subs = 0;
    long ins = 0;

    long totalReadsProcessed = 0;
    long totalReadsCorrected = 0;

    // output parameters
    private final InMemoryValue<File[]> resultingReadsOutValue = new InMemoryValue<File[]>();
    public final InValue<File[]> resultingReadsOut = addOutput("resulting-reads", resultingReadsOutValue, File[].class);


    @Override
    protected void runImpl() throws ExecutionFailedException {
        len = k.get();
        File[] reads = this.reads.get();
        File[] fixes = this.fixes.get();
        File outputDir = this.outputDir.get();

        try {
            outputDir.mkdir();
            applyFixes(reads, outputDir, fixes);
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }

    }

    private void applyFixes(File[] reads, File workDir, File[] fixesFiles) throws IOException {
        info("Loading fixes...");
        ArrayLong2LongHashMap allFixes = new ArrayLong2LongHashMap((int)(Math.log(availableProcessors.get()) / Math.log(2)) + 4);
        LazyLongReader reader = new LazyLongReader(fixesFiles);
        ReadFixesDispatcher dispatcher = new ReadFixesDispatcher(reader, DISPATCH_WORK_RANGE_SIZE);
        ReadFixesWorker[] workers = new ReadFixesWorker[availableProcessors.get()];
        CountDownLatch latch = new CountDownLatch(workers.length);
        for (int i = 0; i < workers.length; ++i) {
            workers[i] = new ReadFixesWorker(dispatcher, allFixes, latch);
            new Thread(workers[i]).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            warn("interrupted");
            for (ReadFixesWorker worker : workers) {
                worker.interrupt();
            }
        }

        debug("loaded " + allFixes.size() + " fixes");
        info("Applying fixes...");
        applyFixes(workDir, reads, allFixes) ;
        info("done");
    }
    
    private void applyFixes(File workDir, File[] reads, ArrayLong2LongHashMap allFixes) throws IOException {
        progress.setTotalTasks(readsNumber.get());
        progress.createProgressBar();
        File[] resFiles = new File[reads.length];

        for (int i = 0; i < reads.length; i++) {
            File sourceFile = reads[i];
            File targetFile = new File(workDir, sourceFile.getName());
            OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));
            applyFixes(sourceFile, out, allFixes);
            out.close();
            resFiles[i] = targetFile;
        }
        progress.destroyProgressBar();
        resultingReadsOutValue.set(resFiles);
    }
        
    private void applyFixes(File in, OutputStream out, ArrayLong2LongHashMap fixes) throws IOException {

        Source<DnaQ> reader = ReadersUtils.readDnaQLazy(in);
        int workersNumber = availableProcessors.get();
        DnaQReadDispatcher dispatcher = new DnaQReadDispatcher(reader, out, DISPATCH_WORK_RANGE_SIZE, totalReadsProcessed,
                workersNumber, progress);
        ApplyFixesWorker[] workers = new ApplyFixesWorker[workersNumber];
        CountDownLatch latch = new CountDownLatch(workers.length);
        for (int i = 0; i < workers.length; ++i) {
            workers[i] = new ApplyFixesWorker(dispatcher, latch, fixes, len, i);
            new Thread(workers[i]).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            warn("interrupted");
            for (ApplyFixesWorker worker : workers) {
                worker.interrupt();
            }
        }
        totalReadsProcessed = dispatcher.totalReadsProcessed;

    }

    private String correct(String read, Long2LongMap fixes, LongSet goodKmers) {
        int readLen = read.length();

        int[][] ar = new int[4][readLen];
        int[][] in = new int[5][readLen];
        int[] del = new int[readLen];
        /*
        Arrays.fill(in[4], 1);
        for (int i = 0; i < readLen; ++i) {
            ar[DnaTools.fromChar(read.charAt(i))][i] = 1;
        }
        */

        for (int i = len; i <= readLen; ++i) {
            long okmer = Misc.getCode(read.substring(i - len, i));
            if (goodKmers.contains(okmer)) {
                for (int j = 0; j < len; ++j) {
                    ++ar[DnaTools.fromChar(read.charAt(i - len + j))][i - len + j];
                }
                continue;
            }
            if (!fixes.containsKey(okmer)) {
                continue;
            }
            long fix = fixes.get(okmer);
            String o = read.substring(i - len, i);
            
            for (int j = 0; j < 8; ++j) {
                int cfix = (int)(fix >> (8  * j)) & 255;
                if (cfix == 0) {
                    break;
                }

                int type = cfix >> 5;
                int pos = len - (cfix & 31);

                if (type == 0) {
                    // del
                    ++in[DnaTools.fromChar(o.charAt(pos))][i - len + pos];
                } else if (type == 4) {
                    // in
                    ++del[i - len + pos];
                } else {
                    // sub
                    ++ar[DnaTools.fromChar(o.charAt(pos)) ^ type][i - len + pos];
                    --ar[DnaTools.fromChar(o.charAt(pos))][i - len + pos];
                }
            }
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < readLen; ++i) {
            int c = DnaTools.fromChar(read.charAt(i));
            for (int j = 0; j < 4; ++j) {
                if (ar[j][i] > ar[c][i]) {
                    c = j;
                }
            }
            if (del[i] <= ar[c][i]) {
                result.append(DnaTools.toChar((byte)c));
                if (c != DnaTools.fromChar(read.charAt(i))) {
                    ++subs;
                }
            } else {
                ++ins;
            }

            c = 4;
            for (int j = 0; j < 4; ++j) {
                if (in[j][i] > in[c][i]) {
                    c = j;
                }
            }
            if (c < 4) {
                result.append(DnaTools.toChar((byte)c));
                ++dels;
            }
        }


        return result.toString();
    }



    @Override
    protected void cleanImpl() {
    }

    public FixesApplier() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new FixesApplier().mainImpl(args);
    }
}
