package ru.ifmo.genetics.tools.io;

import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.StringParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.File;
import java.io.IOException;

public class LazyDnaReaderTool extends Tool {
    public static final String NAME = "dna-reader";
    public static final String DESCRIPTION = "reads dnas from any file";


    public final Parameter<File> fileIn = addParameter(new FileParameterBuilder("in-file")
            .mandatory()
            .withShortOpt("i")
            .withDescription("file to read Dnas from")
            .create());

    public Parameter<String> fileFormatIn = addParameter(new StringParameterBuilder("in-format")
            .optional()
            .withDescription("input file format (fasta, fasta.gz, fastq, fastq.gz, binq)")
            .withDefaultValue(new FileFormatYielder(fileIn))
            .create());


    private InMemoryValue<NamedSource<Dna>> dnasSource = new InMemoryValue<NamedSource<Dna>>();
    public InValue<NamedSource<Dna>> dnasSourceOut = dnasSource.inValue();


    @Override
    protected void runImpl() throws IOException {
        File f = fileIn.get();
        String fileFormat = fileFormatIn.get();

        dnasSource.set(
                ReadersUtils.readDnaLazy(f, fileFormat)
        );
    }


    @Override
    protected void cleanImpl() {
    }

    public LazyDnaReaderTool() {
        super(NAME, DESCRIPTION);
    }


}
