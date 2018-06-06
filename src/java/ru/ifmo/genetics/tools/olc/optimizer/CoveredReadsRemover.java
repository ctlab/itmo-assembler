package ru.ifmo.genetics.tools.olc.optimizer;

import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.readers.DedicatedLineReader;
import ru.ifmo.genetics.io.writers.DedicatedLineWriter;
import ru.ifmo.genetics.executors.BlockingThreadPoolExecutor;
import ru.ifmo.genetics.tools.olc.overlaps.GeneratingOverlapsAfterRemovingCRTask;
import ru.ifmo.genetics.tools.olc.overlaps.RemovingCoveredReadsTask;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static ru.ifmo.genetics.utils.NumUtils.groupDigits;

public class CoveredReadsRemover extends Tool {
    public static final String NAME = "covered-reads-remover";
    public static final String DESCRIPTION = "removes covered reads by marking as removed reads in overlaps file";

    // input params
    public final Parameter<File> readsFile = addParameter(new FileParameterBuilder("reads-file")
            .mandatory()
            .withDescription("file with reads")
            .create());

    public final Parameter<File[]> overlapsFiles = addParameter(new FileMVParameterBuilder("overlaps-files")
            .mandatory()
            .withDescription("files with overlaps")
            .create());

    public final Parameter<File> outOverlapsFile = addParameter(new FileParameterBuilder("out-overlaps-file")
            .optional()
            .withDefaultValue(workDir.append("overlaps.removedCoveredReads.raw"))
            .withDescription("resulting file with overlaps")
            .create());
    
    // internal variables
    private int readsNumber;
    private ArrayList<Dna> reads;
    private File[] overlapsFileArray;
    private boolean rawOverlaps;
    private RemovingCRTaskContext context;


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            load();
            determineCoveredReads();
            generateResultingOverlaps();
            calculateStatistics();
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
        overlapsFileArray = overlapsFiles.get();
        if (overlapsFileArray.length > 0) {
            rawOverlaps = overlapsFileArray[0].getName().contains(".raw");
            for (File f : overlapsFileArray) {
                if (rawOverlaps != f.getName().contains(".raw")) {
                    throw new RuntimeException("Overlaps files with different raw types: " +
                            "file1 = " + overlapsFileArray[0].getName() + ", " +
                            "file2 = " + f.getName());
                }
            }
        }
    }

    private void determineCoveredReads() throws InterruptedException, ExecutionFailedException, FileNotFoundException {
        info("Determining covered reads...");

        context = new RemovingCRTaskContext(reads);

        // determining...
        DedicatedLineReader reader = new DedicatedLineReader(overlapsFileArray, availableProcessors.get());
        reader.start();

        BlockingThreadPoolExecutor executor = new BlockingThreadPoolExecutor(availableProcessors.get());
        while (true) {
            ByteBuffer buffer = reader.getBuffer();
            if (buffer == null) {
                break;
            }
            executor.blockingExecute(new RemovingCoveredReadsTask(context, buffer, rawOverlaps, reader));
        }
        executor.shutdownAndAwaitTermination();

        // post processing
        for (int i = 0; i < readsNumber; ++i) {
            if (context.removingRead[i]) {
                context.removingRead[i ^ 1] = true;
            }
        }
    }


    private void generateResultingOverlaps() throws IOException, InterruptedException {
        info("Generating resulting overlaps...");

        DedicatedLineWriter writer = new DedicatedLineWriter(outOverlapsFile.get(), availableProcessors.get());
        DedicatedLineReader reader = new DedicatedLineReader(overlapsFileArray, availableProcessors.get());
        reader.start();

        BlockingThreadPoolExecutor executor = new BlockingThreadPoolExecutor(availableProcessors.get());
        while (true) {
            ByteBuffer buffer = reader.getBuffer();
            if (buffer == null) {
                break;
            }
            executor.blockingExecute(new GeneratingOverlapsAfterRemovingCRTask(
                    context, buffer, rawOverlaps, reader, writer));
        }

        ByteBuffer output = writer.getBuffer();
        for (int i = 0; i < context.removingRead.length; i++) {
            if (context.removingRead[i]) {
                assert context.removingRead[i ^ 1];

                String overlap = i + " -1 0";
                if (!rawOverlaps) {
                    overlap += " 0";
                }
                output = writer.writeLine(output, overlap);
            }
        }
        writer.writeBuffer(output);


        executor.shutdownAndAwaitTermination();
        writer.close();
    }


    private void calculateStatistics() {
        int allReads = 0, removingReads = 0;
        for (int i = 0; i < readsNumber; ++i) {
            if (!context.readRemoved[i]) {
                assert !context.readRemoved[i ^ 1];
                allReads++;
                if (context.removingRead[i]) {
                    removingReads++;
                }
            }
        }

        String s = "Marked to remove " + groupDigits(removingReads) + " reads";
        if (allReads != 0) {
            s += String.format(" (%.1f%% of all)", removingReads * 100.0 / allReads);
        }
        info(s);

        debug("All overlaps here: " + groupDigits(context.overlapsAll.get()));
        long removingOv = context.overlapsAll.get() - context.overlapsOK.get();
        s = "Overlaps to be removed: " + groupDigits(removingOv);
        if (context.overlapsAll.get() != 0) {
            s += String.format(" (%.1f%% of all)", removingOv * 100.0 / context.overlapsAll.get());
        }
        info(s);
        if (context.overlapsAll.get() != 0) {
            debug("After optimizing there are " + groupDigits(context.overlapsOK.get()) + " overlaps, " +
                    groupDigits(allReads - removingReads) + " reads " +
                    "(" + String.format("%.1f", context.overlapsOK.get() * 2 / (double) (allReads - removingReads))
                    + " overlaps per read)");


            long len = 0;
            int max = 0, min = 9999, cnt = 0;
            for (int i = 0; i < reads.size(); i++) {
                if (!context.readRemoved[i] && !context.removingRead[i]) {
                    int cl = reads.get(i).length();
                    len += cl;
                    max = Math.max(max, cl);
                    min = Math.min(min, cl);
                    cnt++;
                }
            }
            debug("Statistics on " + cnt + " good reads:");
            debug("Sum len = " + groupDigits(len));
            debug("min = " + min + ", avg = " + (len / cnt) + ", max = " + max);
        }
    }


    @Override
    protected void cleanImpl() {
        reads = null;
        overlapsFileArray = null;
        context = null;
    }

    public CoveredReadsRemover() {
        super(NAME, DESCRIPTION);
    }

}
