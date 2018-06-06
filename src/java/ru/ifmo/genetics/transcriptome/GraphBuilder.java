package ru.ifmo.genetics.transcriptome;

import org.hsqldb.lib.ArrayUtil;
import ru.ifmo.genetics.utils.KmerUtils;

import java.io.*;
import java.util.Scanner;
import java.util.StringTokenizer;

public class GraphBuilder {
    private final CompactDeBruijnGraphWithStat graph;

    private final int MIN_FREQ = 3;

    public GraphBuilder(int k, long memSize) {
        graph = new CompactDeBruijnGraphWithStat(k, memSize);
    }

    private void readFile(File f, FileWriter out) throws FileNotFoundException {

        DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));



        while (true) {
            long kmer = 0;
            int prob = 0;
            try {
                kmer = inputStream.readLong();
                prob = inputStream.readInt();
                out.write(prob + "\n");
            } catch (IOException e) {
                break;
            }
            if (prob > MIN_FREQ) {
                graph.addEdge(kmer, prob);
            }
        }

    }

    public void build(File in) {
        File[] files = in.listFiles();

        File out = new File(in.getAbsolutePath() + "statout");
        try {
            out.createNewFile();

            FileWriter fw = new FileWriter(out);

        for (File f : files) {
            String fname = f.getAbsolutePath();
            if (fname.substring(fname.length() - 5, fname.length()).equals(".good")) {
                try {
                    readFile(f,fw);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                System.out.println("Read complete: " + fname);
            }

        }   fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CompactDeBruijnGraphWithStat getGraph() {
        return graph;
    }

}
