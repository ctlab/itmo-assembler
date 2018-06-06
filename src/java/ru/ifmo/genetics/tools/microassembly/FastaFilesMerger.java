package ru.ifmo.genetics.tools.microassembly;

import ru.ifmo.genetics.io.readers.ReaderInSmallMemory;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class FastaFilesMerger extends Tool {
    public static final String NAME = "fasta-files-merger";
    public static final String DESCRIPTION = "merge fasta files to one, rewriting reads numbers";


    // input parameters
    public final Parameter<File[]> fastaFiles = addParameter(new FileMVParameterBuilder("fasta-files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("fasta files to merge")
            .create());

    public final Parameter<File> resultingFile = addParameter(new FileParameterBuilder("resulting-file")
            .withDefaultValue(workDir.append("all.fasta"))
            .withDescription("fasta file with resulting reads")
            .create());

    // internal variables

    // output parameters


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            PrintWriter out = new PrintWriter(resultingFile.get());

            long i = 0;
            for (File f : fastaFiles.get()) {
                ReaderInSmallMemory reader = new ReaderInSmallMemory(f);

                while (reader.hasRemaining()) {
                    CharSequence num = reader.readLine();
                    CharSequence s = reader.readLine();

                    out.println(">" + i);
                    out.println(s);
                    i++;
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

    public FastaFilesMerger() {
        super(NAME, DESCRIPTION);
    }
}
