package ru.ifmo.genetics.tools.ec;

import ru.ifmo.genetics.io.writers.BinqDedicatedWriter;
import ru.ifmo.genetics.io.writers.DoubleFastaWriter;
import ru.ifmo.genetics.io.readers.BinqReader;
import ru.ifmo.genetics.io.sources.TruncatingSource;
import ru.ifmo.genetics.io.writers.WritersUtils;
import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.File;
import java.io.IOException;

public class BinqTruncater extends Tool {
    public static final String NAME = "binq-truncater";
    public static final String DESCRIPTION = "truncates binq files by quality value";

    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("input-files")
            .mandatory()
            .withDescription("input files with reads")
            .create());

    public final Parameter<Integer> phredThreshold = addParameter(new IntParameterBuilder("phred-threshold")
            .optional()
            .withDefaultValue(10)
            .withShortOpt("u")
            .withDescriptionShort("Phred threshold")
            .withDescription("threshold used to cut reads")
            .withDescriptionRuShort("Порог качества")
            .withDescriptionRu("Порог качества при обрезании чтений")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .optional()
            .withDefaultValue(workDir.append("truncated"))
            .withDescription("output directory")
            .create());

    private final InMemoryValue<File[]> truncatedReadsOutValue = new InMemoryValue<File[]>();
    public final InValue<File[]> truncatedReadsOut = addOutput("truncated-reads", truncatedReadsOutValue, File[].class);

    @Override
    protected void runImpl() throws ExecutionFailedException {

        int phredThreshold = this.phredThreshold.get();
        File outputDirectory = this.outputDir.get();
        outputDirectory.mkdir();

        Timer t = new Timer();
        long sumLen = 0;
        long sumTrustLen = 0;

        File[] truncatedReads = new File[inputFiles.get().length];
        int i = 0;
        for (File inputFile: inputFiles.get()) {
            BinqReader binqSource = new BinqReader(inputFile);
            TruncatingSource truncatingSource = new TruncatingSource(binqSource, phredThreshold);
            File outputFile = new File(outputDirectory, inputFile.getName());
            truncatedReads[i++] = outputFile;
            info("Truncating " + inputFile.getName() + "...");
            try {
                WritersUtils.writeDnaQsToBinqFile(truncatingSource, outputFile);
            } catch (IOException e) {
                throw new ExecutionFailedException(e);
            }
            info("Removed " +
                    ((int)((1 - truncatingSource.getSumTrustLen() / (double) truncatingSource.getSumLen()) * 100)) + "% of data");
            sumLen += truncatingSource.getSumLen();
            sumTrustLen += truncatingSource.getSumTrustLen();
        }
        truncatedReadsOutValue.set(truncatedReads);
        debug("Total time = " + t + ", total sumTrustLen / sumLen = " + sumTrustLen / (double) sumLen);
    }

    @Override
    protected void cleanImpl() {
    }

    public BinqTruncater() {
        super(NAME, DESCRIPTION);
    }

}
