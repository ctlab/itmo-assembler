package ru.ifmo.genetics.tools.olc.overlaps;

import org.apache.commons.cli.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.readers.ReaderInSmallMemory;
import ru.ifmo.genetics.utils.Misc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class OverlapsChecker {

    private String readsFile;
    private String overlapsFile;
    // final char[] DNA = "ATGC".toCharArray();

    OverlapsChecker(Configuration config) {
        readsFile = config.getString("reads");
        overlapsFile = config.getString("overlaps");
    }

    int readsCount;

    private ArrayList<Dna> reads;

    private int numberOfErrors(int read1, int shift, int read2) {
//        return SaturatingTask.numberOfErrors(reads.get(read1), shift, reads.get(read2));
        // ToDo: rewrite it
        return -1;
    }

    
    private void run() throws IOException {
        reads = ReadersUtils.loadDnasAndAddRC(new File(readsFile));
        readsCount = reads.size();

        System.err.println(readsCount + " reads loaded");

        ReaderInSmallMemory overlapsReader = new ReaderInSmallMemory(new File(overlapsFile));
        while (true) {
            int from = overlapsReader.readInteger();
            if (from == -1) {
                break;
            }
            int to = overlapsReader.readInteger();
            int shift = overlapsReader.readInteger();
            if (numberOfErrors(from, shift, to) > 10) {
                System.err.println("Wrong overlap: " + from + " " + to + " " + shift);
            }
        }
        overlapsReader.close();
        System.err.println("overlaps loaded");

    }

    public static void main(String[] args) throws IOException {
        Options options = new Options();

        options.addOption("h", "help", false, "prints this message");
        options.addOption("c", "config", true,  "sets the config file name, default to config.properties");
        options.addOption("O", "overlaps", true,  "sets the overlaps file name");
        options.addOption("r", "reads", true,  "sets the reads file name");

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace(System.err);
            return;
        }

        if (cmd.hasOption("help")) {
            new HelpFormatter().printHelp("overlap", options);
            return;
        }

        String configFileName = cmd.getOptionValue("config", "config.properties");
        Configuration config;
        try {
            config = new PropertiesConfiguration(configFileName).subset("overlapper");
        } catch (ConfigurationException e) {
            e.printStackTrace(System.err);
            return;
        }

        Misc.addOptionToConfig(cmd, config, "output");
        Misc.addOptionToConfig(cmd, config, "reads");
        Misc.addOptionToConfig(cmd, config, "overlaps");


        new OverlapsChecker(config).run();
    }

}

