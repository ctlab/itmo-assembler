package ru.ifmo.genetics.tools.io;

import ru.ifmo.genetics.io.writers.DoubleFastaWriter;
import ru.ifmo.genetics.io.readers.BinqReader;
import ru.ifmo.genetics.io.formats.QualityFormatFactory;
import ru.ifmo.genetics.io.writers.FastqDedicatedWriter;
import ru.ifmo.genetics.io.writers.WritersUtils;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.StringParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.File;
import java.io.IOException;

public class ToFastqConverter extends Tool {
    public static final String NAME = "to-fastq-converter";
    public static final String DESCRIPTION = "converts input files to fastq format";


    // input parameters
    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("input-files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("input files")
            .create());

    public final Parameter<String> outputQualityFormat = addParameter(new StringParameterBuilder("output-quality-format")
            .withShortOpt("q")
            .withDefaultValue("illumina")
            .withDescription("output quality format")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .withShortOpt("o")
            .withDefaultValue(workDir.append("converted"))
            .withDescription("output directory")
            .create());


    private final InMemoryValue<File[]> convertedReadsOutValue = new InMemoryValue<File[]>();
    public final InValue<File[]> convertedReadsOut = addOutput("converted-reads", convertedReadsOutValue, File[].class);


    @Override
    protected void runImpl() throws ExecutionFailedException {
        outputDir.get().mkdirs();

        File[] convertedReads = new File[inputFiles.get().length];
        int i = 0;
        for (File f : inputFiles.get()) {
            File outFile = new File(outputDir.get(), FileUtils.baseName(f) + ".fastq");
            convertedReads[i++] = outFile;

            try {
                if (f.getName().toLowerCase().endsWith(".fastq")) {
                    info("File " + f.getName() + " is already in fastq format");
                    FileUtils.copyFile(f, outFile);
                    continue;
                }

                info("Converting " + f.getName() + " to fastq format...");
                BinqReader binqReader = new BinqReader(f);
                WritersUtils.writeDnaQsToFastqFile(
                        binqReader,
                        QualityFormatFactory.getInstance().get(outputQualityFormat.get()),
                        outFile
                );
            } catch (IOException e) {
                throw new ExecutionFailedException(e);
            }
        }
        convertedReadsOutValue.set(convertedReads);
    }



    @Override
    protected void cleanImpl() {
    }

    public ToFastqConverter() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new ToFastqConverter().mainImpl(args);
    }
}
