package ru.ifmo.genetics.tools.io;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.io.formats.QualityFormatFactory;
import ru.ifmo.genetics.utils.tool.*;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.StringParameterBuilder;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

public class LazyDnaQReaderTool extends Tool {
    public static final String NAME = "dnaq-reader";
    public static final String DESCRIPTION = "reads dnaqs from any file";


    // input params
    public final Parameter<File> fileIn = addParameter(new FileParameterBuilder("in-file")
            .mandatory()
            .withShortOpt("i")
            .withDescription("file to read DnaQs from")
            .create());

    public Parameter<String> fileFormatIn = addParameter(new StringParameterBuilder("in-format")
            .optional()
            .withDescription("input file format (fastq, fastq.gz, binq, fasta, fasta.gz)")
            .withDefaultValue(new FileFormatYielder(fileIn))
            .create());

    public Parameter<String> qualityFormatIn = addParameter(new StringParameterBuilder("in-qformat")
            .optional()
            .withDescription("input file quality format (only for fastq, Illumina and Sanger are acceptable)")
            .withDefaultComment("determined automatically")
            .create());


    public Parameter<Integer> setPhred = addParameter(new IntParameterBuilder("set-phred")
            .optional()
            .withDescription("sets phred quality for fasta files")
            .withDefaultValue(20)
            .create());


    // internal variables
    private InMemoryValue<NamedSource<DnaQ>> dnaQsSource = new InMemoryValue<NamedSource<DnaQ>>();

    // output params
    public InValue<NamedSource<DnaQ>> dnaQsSourceOut = dnaQsSource.inValue();


    @Override
    protected void runImpl() throws IOException {
        File f = fileIn.get();
        String fileFormat = fileFormatIn.get();
        QualityFormat qf = (qualityFormatIn.get() == null) ? null :
                QualityFormatFactory.getInstance().get(qualityFormatIn.get());
        int phred = setPhred.get();

        dnaQsSource.set(
                ReadersUtils.readDnaQLazy(f, fileFormat, qf, phred));
    }


    @Override
    protected void cleanImpl() {
    }

    public LazyDnaQReaderTool() {
        super(NAME, DESCRIPTION);
    }


}
