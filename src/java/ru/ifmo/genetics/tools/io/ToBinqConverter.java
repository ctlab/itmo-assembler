package ru.ifmo.genetics.tools.io;

import ru.ifmo.genetics.io.writers.BinqDedicatedWriter;
import ru.ifmo.genetics.io.writers.DoubleFastaWriter;
import ru.ifmo.genetics.io.writers.WritersUtils;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.File;
import java.io.IOException;

public class ToBinqConverter extends Tool {
    public static final String NAME = "to-binq-converter";
    public static final String DESCRIPTION = "converts input files to binq format";

    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("input-files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("input files")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .withShortOpt("o")
            .withDefaultValue(workDir.append("converted"))
            .withDescription("output direcotry")
            .create());


    private final InMemoryValue<File[]> convertedReadsOutValue = new InMemoryValue<File[]>();
    public final InValue<File[]> convertedReadsOut = addOutput("converted-reads", convertedReadsOutValue, File[].class);


    @Override
    protected void runImpl() throws ExecutionFailedException {
        outputDir.get().mkdirs();

        File[] convertedReads = new File[inputFiles.get().length];
        int i = 0;
        for (File f : inputFiles.get()) {
            File outFile = new File(outputDir.get(), FileUtils.baseName(f) + ".binq");
            convertedReads[i++] = outFile;

            try {
                if (f.getName().toLowerCase().endsWith(".binq")) {
                    info("File " + f.getName() + " is already in binq format");
                    FileUtils.copyFile(f, outFile);
                    continue;
                }

                info("Converting " + f.getName() + " to binq format...");
                LazyDnaQReaderTool r = new LazyDnaQReaderTool();
                r.fileIn.set(f);
                r.simpleRun();
                WritersUtils.writeDnaQsToBinqFile(r.dnaQsSourceOut.get(), outFile);
            } catch (IOException e) {
                throw new ExecutionFailedException(e);
            }
        }
        convertedReadsOutValue.set(convertedReads);
    }

    @Override
    protected void cleanImpl() {
    }

    public ToBinqConverter() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new ToBinqConverter().mainImpl(args);
    }
}
