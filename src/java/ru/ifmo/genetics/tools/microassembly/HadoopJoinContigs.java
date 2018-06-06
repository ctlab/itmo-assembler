package ru.ifmo.genetics.tools.microassembly;

import org.apache.hadoop.fs.Path;
import ru.ifmo.genetics.distributed.contigsJoining.tasks.FillHoles;
import ru.ifmo.genetics.distributed.contigsJoining.tasks.FindHoles;
import ru.ifmo.genetics.distributed.contigsJoining.tasks.MakeMaybeAlignedPairs;
import ru.ifmo.genetics.distributed.util.JobUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.StringParameterBuilder;

import java.io.File;
import java.io.IOException;

public class HadoopJoinContigs extends Tool {
    private final static String NAME = "hadoop-contigs-joiner";
    private final static String DESCRIPTION = "Runs microassembly, thus joining contigs to bigger ones";


    // input parameters
    public final Parameter<File> hdfsWorkDir = addParameter(new FileParameterBuilder("hdfs-work-dir")
            .mandatory()
            .withDescription("real work directory in HDFS")
            .create());

    public final Parameter<File> hdfsReadsDir = addParameter(new FileParameterBuilder("hdfs-reads-dir")
            .mandatory()
            .withShortOpt("r")
            .withDescription("hdfs directory with reads")
            .create());

    public final Parameter<File> hdfsAlignsDir = addParameter(new FileParameterBuilder("hdfs-aligns-dir")
            .mandatory()
            .withShortOpt("a")
            .withDescription("hdfs directory with aligns")
            .create());

    public final Parameter<File> hdfsContigsDir = addParameter(new FileParameterBuilder("hdfs-contigs-dir")
            .mandatory()
            .withShortOpt("C")
            .withDescription("hdfs directory with contigs")
            .create());


    public final Parameter<Integer> minLength = addParameter(new IntParameterBuilder("min-length")
            .mandatory()
            .withShortOpt("l")
            .withDescription("lower bound for insert size")
            .create());

    public final Parameter<Integer> maxLength = addParameter(new IntParameterBuilder("max-length")
            .mandatory()
            .withShortOpt("L")
            .withDescription("upper bound for insert size")
            .create());


    public final Parameter<String> qualityFormat = addParameter(new StringParameterBuilder("quality-format")
            .mandatory()
            .withShortOpt("F")
            .withDescription("quality format for reads")
            .create());

    public final Parameter<Integer> trimming = addParameter(new IntParameterBuilder("trimming")
            .optional()
            .withDefaultValue(0)
            .withShortOpt("tr")
            .withDescription("length trimmed from the right end of reads")
            .create());

    public static final String FILLED_HOLES_DIR = "3_filled_holes";



    @Override
    public void runImpl() throws ExecutionFailedException {
        Path joinedAlignsPath = new Path(hdfsWorkDir.get().toString(), "1_joined_aligns");
        Path holesPath = new Path(hdfsWorkDir.get().toString(), "2_holes");
        Path filledHolesPath = new Path(hdfsWorkDir.get().toString(), FILLED_HOLES_DIR);

        try {
            if (!JobUtils.jobSucceededOrRemove(joinedAlignsPath)) {
                MakeMaybeAlignedPairs.make(
                        new Path(hdfsAlignsDir.get().toString()),
                        new Path(hdfsReadsDir.get().toString()),
                        qualityFormat.get(), joinedAlignsPath, trimming.get());
            }

            if (!JobUtils.jobSucceededOrRemove(holesPath)) {
                FindHoles.find(joinedAlignsPath, new Path(hdfsContigsDir.get().toString()), holesPath);
            }

            if (!JobUtils.jobSucceededOrRemove(filledHolesPath)) {
                FillHoles.fill(holesPath, minLength.get(), maxLength.get(), filledHolesPath);
            }
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
    }

    @Override
    protected void cleanImpl() {
    }

    public static void main(String[] args) {
        new HadoopJoinContigs().mainImpl(args);
    }

    public HadoopJoinContigs() {
        super(NAME, DESCRIPTION);
    }
}
