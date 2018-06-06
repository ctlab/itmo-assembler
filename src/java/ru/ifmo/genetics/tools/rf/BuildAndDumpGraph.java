package ru.ifmo.genetics.tools.rf;

import ru.ifmo.genetics.dna.kmers.ShallowBigKmer;
import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.structures.debriujn.CompactDeBruijnGraph;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BuildAndDumpGraph {
    private List<String> kmersFiles;
    
    private CompactDeBruijnGraph graph;
    private int k;
    private long memSize;
    
    private String dumpFile;
    
    private void buildGraph() throws IOException {
        long totalToRead = 0;
        for (String kmersFile : kmersFiles) {
            FileInputStream fileIn = new FileInputStream(kmersFile);
            long toRead = fileIn.getChannel().size() / 8;
            totalToRead += toRead;
            fileIn.close();
        }
        
        System.err.println("have to read " + totalToRead + " k-mers");
        graph = new CompactDeBruijnGraph(k, memSize);


        long xx = 0;
        Timer timer = new Timer();
        timer.start();
        long kmerMask = 0;
        for (String kmersFile : kmersFiles) {
            FileInputStream fileIn = new FileInputStream(kmersFile);
            DataInputStream in = new DataInputStream(new BufferedInputStream(fileIn));
            long toRead = fileIn.getChannel().size() / 8;
            xx += toRead;
            for (long i = 0; i < toRead; ++i) {
                long kmer = in.readLong();
                kmerMask |= kmer;
//                graph.addEdgeKey(kmer);
                graph.addEdge(new ShallowBigKmer(kmer, k + 1));
            }
            fileIn.close();
            System.err.println(xx + " out of " + totalToRead + " k-mers loaded");

            double done = ((double) xx) / totalToRead;
            double total = timer.getTime() / done / 1000;
            double remained = total * (1 - done);
            double elapsed = total * done;

            System.err.println(100 * done + "% done");
            System.err.println("estimated  total time: " + total + ", remaining: " + remained + ", elapsed: "
                    + elapsed);
        }
        if (kmerMask != ((1L << (2 * k + 2)) - 1)) {
            System.err.println("WARNING: kmer size mismatch");
            System.err.println("set: " + k);
            System.err.printf("pmerMask: 0x%x\n", kmerMask);
            for (int i = 1; i < 30; ++i) {
                if (kmerMask == ((1L << (2 * i)) - 1)) {
                    System.err.println("found: " + (i - 1));
                    break;
                }
            }
        }
        System.err.println("graph size: " + graph.edgesSize());
    }
    
    public void run() throws IOException {
        Timer timer = new Timer();
        System.err.println("Building graph...");
        buildGraph();
        System.err.println("Building graph done, it took " + timer);
        
        timer.start();

        System.err.println("Dumping...");
        FileOutputStream fos = new FileOutputStream(dumpFile);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos));
        graph.write(dos);
        dos.close();
        System.err.println("Done, it took " + timer);
    }
    
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: BuildAndDumpGraph <dump-filename> <k> <size in bytes> <kmers>+");
            return;
        }

        BuildAndDumpGraph m = new BuildAndDumpGraph();
        m.dumpFile = args[0];
        m.k = Integer.parseInt(args[1]);
        m.memSize = Long.parseLong(args[2]);
        m.kmersFiles = new ArrayList<String>(Arrays.asList(args).subList(3, args.length));
        
        m.run();
    }
}
