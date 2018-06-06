package ru.ifmo.genetics.tools.microassembly;

import ru.ifmo.genetics.Runner;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.StringParameterBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class HadoopJoinContigsRunner extends Tool {
    private final static String NAME = "contigs-joiner";
    private final static String DESCRIPTION = "Runs microassembly, thus joining contigs to bigger ones";


    // input parameters
    public final Parameter<String> hdfsWorkDir = addParameter(new StringParameterBuilder("hdfs-work-dir")
            .mandatory()
            .withDescription("real work directory in HDFS")
            .create());

    public final Parameter<String> hdfsReadsDir = addParameter(new StringParameterBuilder("hdfs-reads-dir")
            .mandatory()
            .withShortOpt("r")
            .withDescription("hdfs directory with reads")
            .create());

    public final Parameter<String> hdfsAlignsDir = addParameter(new StringParameterBuilder("hdfs-aligns-dir")
            .mandatory()
            .withShortOpt("a")
            .withDescription("hdfs directory with aligns")
            .create());

    public final Parameter<String> hdfsContigsDir = addParameter(new StringParameterBuilder("hdfs-contigs-dir")
            .mandatory()
            .withShortOpt("C")
            .withDescription("hdfs directory with contigs")
            .create());


    public final Parameter<Integer> minLength = addParameter(new IntParameterBuilder("min-length")
            .withShortOpt("l")
            .withDefaultValue(0)
            .withDescription("lower bound for insert size")
            .create());

    public final Parameter<Integer> maxLength = addParameter(new IntParameterBuilder("max-length")
            .withShortOpt("L")
            .withDefaultValue(1000)
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


    @Override
    public void runImpl() throws ExecutionFailedException {
        try {
            File jarWorkDir = workDir.append("jarWorkDir").get();
            FileUtils.createOrClearDir(jarWorkDir);

            String assemblerPath = Runner.getJarFilePath();
            String command = "hadoop jar " + assemblerPath + " " +
                    "-t hadoop-contigs-joiner " +
                    "-w " + jarWorkDir + " " +
                    "--hdfs-work-dir " + hdfsWorkDir.get() + " " +
                    "--hdfs-reads-dir " + hdfsReadsDir.get() + " " +
                    "--hdfs-aligns-dir " + hdfsAlignsDir.get() + " " +
                    "--hdfs-contigs-dir " + hdfsContigsDir.get() + " " +
                    "--min-length " + minLength.get() + " " +
                    "--max-length " + maxLength.get() + " " +
                    "--quality-format " + qualityFormat.get() + " " +
                    "--trimming " + trimming.get();

            info("Starting hadoop-contigs-joiner subprocess...");
            debug("Command = '" + command + "'");
            final Process process = Runtime.getRuntime().exec(command);

            BufferedReader log = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            Thread waitingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        process.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            waitingThread.start();

            debug("Hadoop-contigs-joiner log:");
            while (waitingThread.isAlive()) {
                while (log.ready()) {
                    debug(log.readLine());
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            while (log.ready()) {
                debug(log.readLine());
            }

            info("Hadoop-contigs-joiner has finished it's work");
            int exitValue = process.exitValue();
            debug("Exit value = " + exitValue);

            if (exitValue != 0) {
                throw new ExecutionFailedException("Non-zero return code of running Hadoop-contigs-joiner subprocess.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void cleanImpl() {
    }

    public HadoopJoinContigsRunner() {
        super(NAME, DESCRIPTION);
    }
}
