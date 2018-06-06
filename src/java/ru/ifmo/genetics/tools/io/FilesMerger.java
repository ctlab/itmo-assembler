package ru.ifmo.genetics.tools.io;

import ru.ifmo.genetics.io.readers.ReaderInSmallMemory;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class FilesMerger extends Tool {
    public static final String NAME = "files-merger";
    public static final String DESCRIPTION = "merge many files to one";


    // input parameters
    public final Parameter<File[]> files = addParameter(new FileMVParameterBuilder("files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("files to merge")
            .create());

    public final Parameter<File> resultingFile = addParameter(new FileParameterBuilder("resulting-file")
            .mandatory()
            .withShortOpt("o")
            .withDescription("resulting file")
            .create());

    // internal variables

    // output parameters


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            PrintWriter out = new PrintWriter(resultingFile.get());

            for (File f : files.get()) {
                ReaderInSmallMemory reader = new ReaderInSmallMemory(f);
                while (reader.hasRemaining()) {
                    CharSequence s = reader.readLine();
                    out.println(s);
                }
                reader.close();
            }

            out.close();

        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
    }



    @Override
    protected void cleanImpl() {
    }

    public FilesMerger() {
        super(NAME, DESCRIPTION);
    }
}
