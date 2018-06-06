package ru.ifmo.genetics.statistics;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.kmers.ShortKmer;
import ru.ifmo.genetics.io.readers.BinqReader;
import ru.ifmo.genetics.io.sources.Source;
import ru.ifmo.genetics.tools.io.LazyLongReader;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

public class ReadsCoverageCalculator {

    public static void main(String[] args) throws IOException {
        int len = Integer.parseInt(args[0]);
        double f = Double.parseDouble(args[1]);
        int i = 2;
        for (; (i < args.length) && !args[i].equals("--"); ++i);
        String[] kmerFiles = Arrays.copyOfRange(args, 2, i);
        String[] readFiles = Arrays.copyOfRange(args, i + 1, args.length);

        LongSet kmers = new LongOpenHashSet();
        LazyLongReader reader = new LazyLongReader(kmerFiles);
        while (true) {
            try {
                kmers.add(reader.readLong());
            } catch (EOFException e) {
                break;
            }
        }
        System.err.println("loaded " + kmers.size() + " good kmers");
        long ans = 0;
        long reads = 0;
        int[] stat = new int[100];
        long firstGood = 0;
        for (String filename : readFiles) {
            Source<DnaQ> source = new BinqReader(filename);
            for (DnaQ dnaq : source) {
                int covered = 0;
                int begin = -1;
                int j = 0;
                boolean counted = false;
                for (ShortKmer kmer : ShortKmer.kmersOf(dnaq, len)) {
                    if (kmers.contains(kmer.toLong())) {
                        if (!counted) {
                            ++firstGood;
                            counted = true;
                        }
                        ++covered;
                        if (begin != -1) {
                            int qlen = j - begin;
                            ++stat[qlen];
                            begin = -1;
                        }
                    } else {
                        if (begin == -1) {
                            begin = j;
                        }
                    }
                    ++j;
//                    counted = true;
                }
                if (begin != -1) {
                    ++stat[j - begin];
                }
                if (1. * covered / (dnaq.length - len + 1) >= f) {
                    ++ans;
                }
                ++reads;
            }
        }
        System.out.println("covered " + ans + " reads of " + reads + " (" + 100. * ans / reads + "%)");
        System.out.println(firstGood);
        /*
        for (int j = 0; j < stat.length; ++j) {
            if (stat[j] > 0) {
                System.out.println(j + " " + stat[j]);
            }
        }
        */
    }

}
