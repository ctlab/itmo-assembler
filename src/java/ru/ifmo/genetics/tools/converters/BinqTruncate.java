package ru.ifmo.genetics.tools.converters;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import ru.ifmo.genetics.io.readers.BinqReader;
import ru.ifmo.genetics.io.sources.TruncatingSource;
import ru.ifmo.genetics.io.writers.WritersUtils;
import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.utils.tool.Tool;

import java.io.File;

public class BinqTruncate {
    private static Logger logger = Logger.getLogger(BinqTruncate.class);

    public static void main(String[] args) throws Exception {
        Options options = new Options();

        options.addOption("h", "help", false, "prints this message");
        options.addOption("o", "output-directory", true,  "sets the ouput directory (mandatory)");
        options.addOption("p", "phred-threshold", true,  "sets the phred-threshold (default: 10)");


        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace(System.err);
            return;
        }

        if (cmd.hasOption("help")) {
            new HelpFormatter().printHelp("layout", options);
            return;
        }


        String outputDirectory = cmd.getOptionValue("output-directory");

        int phredThreshold = Integer.parseInt(cmd.getOptionValue("phred-threshold", "10"));

        Timer t = new Timer();

        long sumLen = 0;
        long sumTrustLen = 0;

        for (String s: cmd.getArgs()) {
            File inputFile = new File(s);
            BinqReader binqSource = new BinqReader(inputFile);
            TruncatingSource truncatingSource = new TruncatingSource(binqSource, phredThreshold);
            File outputFile = new File(outputDirectory, inputFile.getName());
            Tool.info(logger, "Truncating " + s + "...");
            WritersUtils.writeDnaQsToBinqFile(truncatingSource, outputFile);
            Tool.info(logger, "... done, sumTrustLen / sumLen = " +
                    truncatingSource.getSumTrustLen() / (double) truncatingSource.getSumLen());
            sumLen += truncatingSource.getSumLen();
            sumTrustLen += truncatingSource.getSumTrustLen();
        }


        Tool.info(logger, "Al truncating done");
        Tool.info(logger, "Total time = " + t + ", total sumTrustLen / sumLen = " + sumTrustLen / (double) sumLen);

    }
}
