package ru.ifmo.genetics.tools.olc.optimizer;

import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.ThreadUnsafeBufferedOutputStream;
import ru.ifmo.genetics.io.readers.ReaderInSmallMemory;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import static ru.ifmo.genetics.utils.NumUtils.groupDigits;

public class OverlapsSlicer extends Tool {
    public static final String NAME = "overlaps-slicer";
    public static final String DESCRIPTION = "slices overlaps by removing part of the reads";


    // input params
    public final Parameter<File> readsFile = addParameter(new FileParameterBuilder("reads-file")
            .mandatory()
            .withDescription("file with all reads")
            .create());

    public final Parameter<File[]> overlapsFiles = addParameter(new FileMVParameterBuilder("overlaps-files")
            .mandatory()
            .withDescription("files with overlaps")
            .create());

    public final Parameter<File> outReadsFile = addParameter(new FileParameterBuilder("out-reads-file")
            .optional()
            .withDefaultValue(workDir.append("reads.sliced.fasta"))
            .withDescription("file with sliced reads")
            .create());

    public final Parameter<File> outOverlapsFile = addParameter(new FileParameterBuilder("out-overlaps-file")
            .optional()
            .withDefaultValue(workDir.append("overlaps.sliced.raw"))
            .withDescription("file with sliced overlaps")
            .create());



    private static double getSlicingCoefficient(long numReads, long numOverlaps, long memory) {
        // solves equation: 8 * 1.25 * numOverlaps * x^2 + (60 + 2 * 8 + 60) * numReads * x - 0.8 * memory= 0
        double a = 8. * 1.25 * numOverlaps;
        double b = (60. + 2 * 8 + 60) * numReads;
        double c = -0.8 * memory;

        double d = b * b - 4 * a * c;
        double x = (-b + Math.sqrt(d)) / 2 / a;
//        double x2 = (-b - Math.sqrt(d)) / 2 / a; // it's negative
        return x;
    }

    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            debug("Available memory: " + Misc.availableMemoryAsString());

            ArrayList<Dna> reads = ReadersUtils.loadDnasAndAddRC(readsFile.get());

            int readsNumber = reads.size();
            info("Reads number = " + groupDigits(readsNumber));

            long inputOverlapsNumber = 0;
            for (File f : overlapsFiles.get()) {
                inputOverlapsNumber += FileUtils.linesNumber(f);
            }
            info("Overlaps number = " + groupDigits(inputOverlapsNumber));

            double slicingCoefficient = getSlicingCoefficient(reads.size(), inputOverlapsNumber, Misc.availableMemory());

            int slicingThreshold = readsNumber;
            if (slicingCoefficient < 1) {
                slicingThreshold =  (int)(readsNumber * slicingCoefficient) & ~1;
                info(String.format("Ignoring %.1f%% of reads with ids no less then %d", (1 - slicingCoefficient) * 100, slicingThreshold));
            }

            PrintWriter readsOut = new PrintWriter(outReadsFile.get());
            for (int i = 0; i < slicingThreshold; i += 2) {
                readsOut.printf(">%d\n%s\n", i / 2, reads.get(i));
            }
            readsOut.close();

            ThreadUnsafeBufferedOutputStream overlapsOut = new ThreadUnsafeBufferedOutputStream(new FileOutputStream(outOverlapsFile.get()));
            long size = 0;
            for (File f : overlapsFiles.get()) {
                ReaderInSmallMemory overlapsReader = new ReaderInSmallMemory(f);
                while (true) {
                    int from = overlapsReader.readInteger();
                    if (from == -1) {
                        break;
                    }
                    int to = overlapsReader.readInteger();
                    int shift = overlapsReader.readInteger();
                    if (from < slicingThreshold && to < slicingThreshold) {
                        overlapsOut.print(from + " " + to + " " + shift + "\n");
                        ++size;
                    }
                }
                overlapsReader.close();
            }
            overlapsOut.close();

            String s = "Overlaps remained = " + groupDigits(size);
            if (inputOverlapsNumber != 0) {
                s += String.format(" = %.1f%% of all", 100.0 * size / inputOverlapsNumber);
            }
            info(s);

        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
    }


    @Override
    protected void cleanImpl() {
    }

    public OverlapsSlicer() {
        super(NAME, DESCRIPTION);
    }

}
