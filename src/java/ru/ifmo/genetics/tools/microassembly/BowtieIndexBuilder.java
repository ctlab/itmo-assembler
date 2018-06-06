package ru.ifmo.genetics.tools.microassembly;

import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;

import java.io.File;
import java.io.IOException;

public class BowtieIndexBuilder extends Tool {
    public static final String NAME = "bowtie-index-builder";
    public static final String DESCRIPTION = "builds bowtie index";


    // input parameters
    public final Parameter<File> allContigsFile = addParameter(new FileParameterBuilder("all-contigs-file")
            .mandatory()
            .withShortOpt("i")
            .withDescription("file with all contigs")
            .create());

    public final Parameter<File> resultingIndexFile = addParameter(new FileParameterBuilder("resulting-index-file")
            .optional()
            .withDefaultValue(workDir.append("contigs-index"))
            .withDescription("file with resulting contigs index")
            .create());

    // internal variables

    // output parameters


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {

            execCommand("bowtie-build " + allContigsFile.get() + " " + resultingIndexFile.get());

        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            throw new ExecutionFailedException(e);
        }
    }



    @Override
    protected void cleanImpl() {
    }

    public BowtieIndexBuilder() {
        super(NAME, DESCRIPTION);
    }
}
