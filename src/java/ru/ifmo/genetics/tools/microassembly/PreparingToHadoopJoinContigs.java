package ru.ifmo.genetics.tools.microassembly;

import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.StringParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.*;

import java.io.File;
import java.io.IOException;

public class PreparingToHadoopJoinContigs extends Tool {
    public static final String NAME = "preparing";
    public static final String DESCRIPTION = "preparing to running hadoop join contigs " +
            "(uploading input files to HDFS, quality format determination)";


    // input parameters
    public final Parameter<File[]> readsFiles = addParameter(new FileMVParameterBuilder("reads-files")
            .mandatory()
            .withDescription("reads files")
            .create());

    public final Parameter<File> allContigsFile = addParameter(new FileParameterBuilder("all-contigs-file")
            .mandatory()
            .withDescription("file with all contigs")
            .create());

    public final Parameter<File> alignsDir = addParameter(new FileParameterBuilder("aligns-dir")
            .mandatory()
            .withDescription("file with aligns directory")
            .create());



    public final Parameter<String> hdfsWorkDirPrefix = addParameter(new StringParameterBuilder("hdfs-work-dir-prefix")
            .withShortOpt("H")
            .withDefaultValue("workDir")
            .withDescription("work directory prefix in HDFS")
            .create());

    public final Parameter<String> hdfsReadsDir = addParameter(new StringParameterBuilder("hdfs-reads-dir")
            .withShortOpt("r")
            .withDefaultValue(new StringUnionYielder(hdfsWorkDirPrefix, "/source/reads"))
            .withDescription("hdfs directory with reads")
            .create());

    public final Parameter<String> hdfsAlignsDir = addParameter(new StringParameterBuilder("hdfs-aligns-dir")
            .withShortOpt("a")
            .withDefaultValue(new StringUnionYielder(hdfsWorkDirPrefix, "/source/map"))
            .withDescription("hdfs directory with aligns")
            .create());

    public final Parameter<String> hdfsContigsDir = addParameter(new StringParameterBuilder("hdfs-contigs-dir")
            .withShortOpt("C")
            .withDefaultValue(new StringUnionYielder(hdfsWorkDirPrefix, "/source/contigs"))
            .withDescription("hdfs directory with contigs")
            .create());

    public final Parameter<String> hdfsRealWorkDir = addParameter(new StringParameterBuilder("hdfs-real-work-dir")
            .withDefaultValue(new StringUnionYielder(hdfsWorkDirPrefix, "/work"))
            .withDescription("real work directory in HDFS")
            .create());

    public final Parameter<String> qualityFormatIn = addParameter(new StringParameterBuilder("quality-format")
            .withShortOpt("F")
            .withDefaultValue("auto")
            .withDescription("quality format for reads")
            .create());


    // internal variables

    // output parameters
    private final InMemoryValue<String> qualityFormatOutValue = new InMemoryValue<String>();
    public final InValue<String> qualityFormatOut = qualityFormatOutValue.inValue();


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            info("Cleaning HDFS workdir (" + hdfsWorkDirPrefix.get() + ")...");
            execCommandWithoutChecking("hadoop fs -rmr " + hdfsWorkDirPrefix.get());

            info("Making directories in HDFS...");
            execCommandWithoutLogging("hadoop fs -mkdir " + hdfsReadsDir.get());
            execCommandWithoutLogging("hadoop fs -mkdir " + hdfsAlignsDir.get());
            execCommandWithoutLogging("hadoop fs -mkdir " + hdfsContigsDir.get());
            execCommandWithoutLogging("hadoop fs -mkdir " + hdfsRealWorkDir.get());
            
            info("Coping reads to HDFS...");
            String readsStr = new ArrayToStringYielder<File>(readsFiles).get();
            execCommandWithoutLogging("hadoop fs -copyFromLocal " + readsStr + " " + hdfsReadsDir.get());
            
            info("Coping contigs to HDFS...");
            execCommandWithoutLogging("hadoop fs -copyFromLocal " + allContigsFile.get() + " " + hdfsContigsDir.get());

            info("Coping alignment files to HDFS...");
            String alignsFiles = new ArrayToStringYielder<File>(new ListFilesYielder(alignsDir)).get();
            execCommandWithoutLogging("hadoop fs -copyFromLocal " + alignsFiles + " " + hdfsAlignsDir.get());

            String quality = qualityFormatIn.get();
            if (quality.equals("auto")) {
                info("Determinating quality format in reads...");
                File[] files = readsFiles.get();
                if (files.length == 0) {
                    throw new ExecutionFailedException("Can't determine qualify format in reads: no reads files");
                }
                quality = ReadersUtils.determineQualityFormat(files[0]).toString();
            }
            qualityFormatOutValue.set(quality);

        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            throw new ExecutionFailedException(e);
        }
    }



    @Override
    protected void cleanImpl() {
    }

    public PreparingToHadoopJoinContigs() {
        super(NAME, DESCRIPTION);
    }
}
