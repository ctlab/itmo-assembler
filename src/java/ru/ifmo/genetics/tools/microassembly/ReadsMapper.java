package ru.ifmo.genetics.tools.microassembly;

import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;

import java.io.File;
import java.io.IOException;

public class ReadsMapper extends Tool {
    public static final String NAME = "reads-mapper";
    public static final String DESCRIPTION = "maps reads on contigs";


    // input parameters
    public final Parameter<File> indexFile = addParameter(new FileParameterBuilder("index-file")
            .mandatory()
            .withDescription("contigs index file path (without extension)")
            .create());

    public final Parameter<File[]> readsFiles = addParameter(new FileMVParameterBuilder("reads-files")
            .mandatory()
            .withDescription("reads files")
            .create());

    public final Parameter<File> resultingMapDir = addParameter(new FileParameterBuilder("resulting-map-dir")
            .optional()
            .withDefaultValue(workDir.append("map"))
            .withDescription("file with resulting map directory")
            .create());

    public final Parameter<Integer> trimming = addParameter(new IntParameterBuilder("trimming")
            .optional()
            .withDefaultValue(0)
            .withDescription("length of trimming from right end")
            .create());

    // internal variables

    // output parameters


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            FileUtils.createOrClearDir(resultingMapDir.get());
            
            for (File f : readsFiles.get()) {
                info("Running mapping command for file " + f.getName() + "...");

                String command = "bowtie --trim3 " + trimming.get() + " -a -m 10 -p " + availableProcessors.get();
                String resultingFile = new File(resultingMapDir.get(), FileUtils.baseName(f) + ".map").toString();
                command += " " + indexFile.get() + " " + f + " " + resultingFile;

                execCommand(command);

            }
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            throw new ExecutionFailedException(e);
        }
    }



    @Override
    protected void cleanImpl() {
    }

    public ReadsMapper() {
        super(NAME, DESCRIPTION);
    }
}
