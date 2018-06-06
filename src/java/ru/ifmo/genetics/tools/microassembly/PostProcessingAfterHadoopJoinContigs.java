package ru.ifmo.genetics.tools.microassembly;

import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.StringParameterBuilder;

import java.io.File;
import java.io.IOException;

public class PostProcessingAfterHadoopJoinContigs extends Tool {
    public static final String NAME = "post-processing";
    public static final String DESCRIPTION = "post processing after running hadoop join contigs " +
            "(downloading output files from HDFS)";


    // input parameters
    public final Parameter<String> hdfsHolesDir = addParameter(new StringParameterBuilder("hdfs-holes-dir")
            .mandatory()
            .withDescription("holes directory in HDFS")
            .create());

    public final Parameter<File> holesDir = addParameter(new FileParameterBuilder("holes-dir")
            .mandatory()
            .withDescription("holes directory")
            .create());


    // internal variables

    // output parameters


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            FileUtils.createOrClearDirRecursively(holesDir.get());
            
            info("Coping filled holes from HDFS...");
            String filesStr = hdfsHolesDir.get() + "/part*";
            execCommandWithoutLogging("hadoop fs -copyToLocal " + filesStr + " " + holesDir.get());

        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            throw new ExecutionFailedException(e);
        }
    }



    @Override
    protected void cleanImpl() {
    }

    public PostProcessingAfterHadoopJoinContigs() {
        super(NAME, DESCRIPTION);
    }
}
