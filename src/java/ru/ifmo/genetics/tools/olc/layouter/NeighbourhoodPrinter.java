package ru.ifmo.genetics.tools.olc.layouter;

import org.apache.commons.cli.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.tools.olc.overlaps.Overlaps;
import ru.ifmo.genetics.tools.olc.overlaps.OverlapsList;
import ru.ifmo.genetics.utils.Misc;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class NeighbourhoodPrinter {
    private String readsFile;
    private String overlapsFile;
    private String outputFile;
    private List<Integer> centers;
    private int depth;
    private int readsNumber = -1;
    private Configuration config;

    public NeighbourhoodPrinter(Configuration config) {
        depth = config.getInt("depth");
        overlapsFile = config.getString("overlaps");
        outputFile = config.getString("output");
        readsFile = config.getString("reads");
        centers = new ArrayList<Integer>();
        readsNumber = config.getInt("reads_number", -1);
        for (Object s: config.getList("centers")) {
            centers.add(Integer.parseInt((String)s));
        }
        this.config = config;
    }

    public void run() throws IOException, InterruptedException {
        ArrayList<Dna> reads;
        if (readsNumber == -1) {
            reads = ReadersUtils.loadDnasAndAddRC(new File(readsFile));
            readsNumber = reads.size();
        } else {
            reads = new ArrayList<Dna>(readsNumber);
            for (int i = 0; i < readsNumber; ++i) {
                reads.add(Dna.emptyDna);
            }
        }

        Overlaps<Dna> allOverlaps = new Overlaps<Dna>(reads, new File[]{new File(overlapsFile)}, 6);
        System.err.println("overlaps loaded");


        if (centers.contains(-1)) {
            centers.clear();
            for (int i = 0; i < reads.size(); i++) {
                centers.add(i);
            }
        }

        for (int i : centers) {
            System.err.println("Read " + i);
            System.err.println(reads.get(i));
        }

        HashSet<Integer> visited = new HashSet<Integer>();
        HashSet<Integer> queue = new HashSet<Integer>();
        PrintWriter out = new PrintWriter(outputFile);

        queue.addAll(centers);
        OverlapsList edges = new OverlapsList(allOverlaps.withWeights);
        for (int i = 0; i < depth; ++i) {
            HashSet<Integer> nextQueue = new HashSet<Integer>();

            for (int v : queue) {
                allOverlaps.getAllOverlaps(v, edges);
                if (edges.size() == 0) {
                    System.err.println("Vertex " + v + ": no edges");
                }
                for (int j = 0; j < edges.size(); ++j) {
                    int vv = v;
                    int nv = edges.getTo(j);
                    int centerShift = edges.getCenterShift(j);

                    if (!visited.contains(nv) && !queue.contains(nv)) {
                        nextQueue.add(nv);
                    }

                    if (!visited.contains(nv)) {
                        if (!Overlaps.isWellOriented(vv, nv, centerShift)) {
                            int t = vv;
                            vv = nv;
                            nv = t;
                            centerShift = -centerShift;
                        }

//                        int ovLen = allOverlaps.calculateOverlapLen(vv, nv, centerShift);
//                        out.println(vv + " " + nv + " " + ovLen);
                        out.print(vv + " " + nv + " " + centerShift);
                        if (edges.isWithWeights()) {
                            out.println(" " + edges.getWeight(j));
                        } else {
                            out.println();
                        }
                    }
                }
                visited.add(v);
            }

            queue = nextQueue;
        }


        String S = "PREV...";
        String T = "NEXT...";

        for (int v: queue) {
            allOverlaps.getAllOverlaps(v, edges);

            boolean hasNotVisitedOutEdge = false;
            boolean hasNotVisitedInEdge = false;

            for (int j = 0; j < edges.size(); ++j) {
                int nv = edges.getTo(j);
                int centerShift = edges.getCenterShift(j);
                if (!visited.contains(nv)) {
                    if (Overlaps.isWellOriented(v, nv, centerShift)) {
                        hasNotVisitedOutEdge = true;
                    } else {
                        hasNotVisitedInEdge = true;
                    }
                }
            }

            if (hasNotVisitedOutEdge) {
                out.println(v + " " + T + " " + 0);
            }
            if (hasNotVisitedInEdge) {
                out.println(S + " " + v + " " + 0);
            }
        }


        System.err.println("Reachable vertex count: " + visited.size());

        out.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Options options = new Options();

        options.addOption("h", "help", false, "prints this message");
        options.addOption("c", "config", true,  "sets the config file name");
        options.addOption("o", "output", true,  "sets the ouput file name");
        options.addOption("O", "overlaps", true,  "sets the overlaps file name");
        options.addOption("d", "depth", true,  "sets the depth of neighbourhood");
        options.addOption("r", "reads", true,  "sets the reads file name");
        options.addOption("n", "reads-number", true,  "sets the number of reads");
        options.addOption("C", "centers", true,  "sets the center of neighbourhood");

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

        String configFileName = cmd.getOptionValue("config");
        Configuration config = new PropertiesConfiguration();
        if (configFileName != null) {
            try {
                config = new PropertiesConfiguration(configFileName);
            } catch (ConfigurationException e) {
                e.printStackTrace(System.err);
                return;
            }
        }

        Misc.addOptionToConfig(cmd, config, "output");
        Misc.addOptionToConfig(cmd, config, "overlaps");
        Misc.addOptionToConfig(cmd, config, "centers");
        Misc.addOptionToConfig(cmd, config, "depth");
        Misc.addOptionToConfig(cmd, config, "reads");
        Misc.addOptionToConfig(cmd, config, "reads-number");

        new NeighbourhoodPrinter(config).run();
    }
}
