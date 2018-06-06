package ru.ifmo.genetics.tools;

import org.apache.commons.lang.mutable.*;
import ru.ifmo.genetics.dna.*;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.tools.io.LazyDnaReaderTool;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.io.sources.NamedSource;

import java.io.*;
import java.util.*;

public class KSelector {
    public final static long THRESHOLD = 35;
    public final static int minK = 27;
    public final static int maxK = 50;
    public final static int kd = 8;

    public static void main(String[] args) throws Exception {
        Random r = new Random(42);
        final int prefixLength = 12;
        HashSet<String> prefixes = new HashSet<String>();
        for (int i = 0; i < 16*1024; ++i) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < prefixLength; ++j) {
                sb.append(DnaTools.NUCLEOTIDES[r.nextInt(4)]);
            }
            prefixes.add(sb.toString());
        }

        ArrayList<LightDna> suffixes = new ArrayList<LightDna>();

        int read = 0;


        for (String file: args) {
            System.err.println("file " + file);

            NamedSource<Dna> reader = ReadersUtils.readDnaLazy(new File(file));
            for (Dna dna: reader) {
                read++;
                String s = DnaTools.toString(dna);
                for (int i = 0; i + prefixLength < s.length(); ++i) {
                    if (!prefixes.contains(s.substring(i, i + prefixLength))) {
                        continue;
                    }
                    suffixes.add(new Dna(new DnaView(dna, i, dna.length())));
                }

                LightDna dnaRc = DnaView.rcView(dna);
                String sRc = DnaTools.toString(dnaRc);

                for (int i = 0; i + prefixLength < sRc.length(); ++i) {
                    if (!prefixes.contains(sRc.substring(i, i + prefixLength))) {
                        continue;
                    }
                    suffixes.add(new Dna(new DnaView(dnaRc, i, dna.length())));
                }
                if (read % 1000000 == 0) {
                    System.err.println("read: " + read + ", added: " + suffixes.size());
                }
            }
        }


        HashMap<LightDna, MutableLong> kmers = new HashMap<LightDna, MutableLong>();
        HashMap<LightDna, MutableLong> k1mers = new HashMap<LightDna, MutableLong>();

        // PrintWriter suffixesOut = new PrintWriter("suffixes");
        // for (LightDna dna: suffixes) {
            // suffixesOut.println(dna);
        // }
        // suffixesOut.close();

        System.out.printf("K\tbad ratio\tbad\tgood\tdead-ends\tforks\tcovered\n");
        for (int k = Math.max(minK, prefixLength); k <= maxK; k += kd) {
            kmers.clear();
            k1mers.clear();

            for (LightDna dna: suffixes) {
                if (dna.length() < k) {
                    continue;
                }
                Misc.addMutableLong(kmers, new DnaView(dna, 0, k), 1);
                if (dna.length() < k + 1) {
                    continue;
                }
                Misc.addMutableLong(k1mers, new DnaView(dna, 0, k + 1), 1);
            }

            TreeMap<Long, MutableInt> stat = new TreeMap<Long, MutableInt>();
            for (Map.Entry<LightDna, MutableLong> entry: kmers.entrySet()) {
                Misc.addMutableInt(stat, entry.getValue().longValue(), 1);
            }

            PrintWriter statOut = new PrintWriter("stat-" + k);
            for (Map.Entry<Long, MutableInt> entry: stat.entrySet()) {
                statOut.println(entry.getKey() + " " + entry.getValue());
            }
            statOut.close();

            long coveredKmers = 0;
            long deadEnds = 0;
            long forks = 0;
            long goodKmers = 0;

            PrintWriter goodOut = new PrintWriter("good-" + k);
            PrintWriter badOut = new PrintWriter("bad-" + k);
            for (Map.Entry<LightDna, MutableLong> entry: kmers.entrySet()) {
                if (entry.getValue().longValue() < THRESHOLD) {
                    continue;
                }

                coveredKmers++;


                long maxOut = -1;
                for (Dna nuc: Dna.oneNucDnas) {
                    LightDna k1mer = new ConcatenatingDnaView(entry.getKey(), nuc);
                    if (k1mers.containsKey(k1mer) && k1mers.get(k1mer).longValue() >= THRESHOLD) {
                        long t = k1mers.get(k1mer).longValue();
                        if (t > maxOut) {
                            maxOut = t;
                        }
                    }
                }

                long threshold = Math.max(THRESHOLD, maxOut / 3);

                int continuations = 0;
                for (Dna nuc: Dna.oneNucDnas) {
                    LightDna k1mer = new ConcatenatingDnaView(entry.getKey(), nuc);
                    if (k1mers.containsKey(k1mer) && k1mers.get(k1mer).longValue() >= threshold) {
                        continuations++;
                    }
                }

                if (continuations == 0) {
                    badOut.println(entry.getKey() + " " + entry.getValue());
                    badOut.println();
                    deadEnds++;
                } else if (continuations == 1) {
                    goodKmers++;
                    goodOut.println(entry.getKey() + " " + entry.getValue());
                } else {
                    forks++;
                    badOut.println(entry.getKey() + " " + entry.getValue());
                    for (Dna nuc: Dna.oneNucDnas) {
                        LightDna k1mer = new ConcatenatingDnaView(entry.getKey(), nuc);
                        if (k1mers.containsKey(k1mer) && k1mers.get(k1mer).longValue() >= THRESHOLD) {
                            badOut.println(k1mer + " " + k1mers.get(k1mer).longValue());
                        }
                    }
                    badOut.println();
                }
            }
            goodOut.close();
            badOut.close();

            if (coveredKmers == 0) {
                continue;
            }

            long badKmers = deadEnds + forks;
            double badRatio = (double)(deadEnds + forks) / coveredKmers;
            System.out.printf("%d\t%f\t%d\t%d\t%d\t%d\t%d\n", k, badRatio, badKmers, goodKmers, deadEnds, forks, coveredKmers);

        }


    }
}
