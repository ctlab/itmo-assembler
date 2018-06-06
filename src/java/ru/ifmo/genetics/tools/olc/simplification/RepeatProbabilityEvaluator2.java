package ru.ifmo.genetics.tools.olc.simplification;

import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.kmers.Kmer;
import ru.ifmo.genetics.dna.kmers.KmerIteratorFactory;
import ru.ifmo.genetics.dna.kmers.ShortKmerIteratorFactory;
import ru.ifmo.genetics.statistics.NormalDistribution;
import ru.ifmo.genetics.structures.map.ArrayLong2IntHashMap;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Repeat probability evaluator, evaluates probability using reads' coverage.
 */
public class RepeatProbabilityEvaluator2 {

    final ArrayList<Dna> reads;
    final ArrayLong2IntHashMap hm;
    final int k;

    public RepeatProbabilityEvaluator2(ArrayList<Dna> reads, ArrayLong2IntHashMap hm, int k) {
        this.reads = reads;
        this.hm = hm;
        this.k = k;
    }


    private static boolean shouldDebugOutput = false;

    public double calculateRepeatProbability(int from, int to, int beginShift) {
        if (beginShift < 0) {
            return calculateRepeatProbability(to, from, -beginShift);
        }

        // preparing
        Dna a = reads.get(from);
        Dna b = reads.get(to);

        int x = beginShift;
        int y = a.length() - 1;
        int z = beginShift + b.length() - 1;
        if (GraphSimplification.shouldDebugOutput(from, to)) {
            System.out.println("x = " + x + ", y = " + y + ", z = " + z);
        }

        int[] aCov = getCoverage(a);
        int[] bCov = getCoverage(b);

        double[] cov = new double[z - k + 2];
        for (int i = 0; i < cov.length; i++) {
            if (i < x) {
                cov[i] = aCov[i];
            } else if (i <= y - k + 1) {
                cov[i] = (aCov[i] + bCov[i - x]) / 2.0;
            } else {
                cov[i] = bCov[i - x];
            }
        }

        double[] covSum = new double[cov.length + 1];
        covSum[0] = 0;
        for (int i = 1; i < covSum.length; i++) {
            covSum[i] = cov[i - 1] + covSum[i - 1];
        }
        double Sc = covSum[cov.length];
        if (GraphSimplification.shouldDebugOutput(from, to)) {
            System.out.println("Sc = " + String.format("%.1f", Sc));
        }

        double[] covSmooth = smoothOut(cov, 16);


        // calculating maximal probability
        double maxP = 0;
        for (int repeatBegin = 1; repeatBegin <= x; repeatBegin++) {
            for (int repeatEnd = y; repeatEnd < z; repeatEnd++) {
                double aUMeanCov = covSum[repeatBegin] / repeatBegin;
                double bUMeanCov = (covSum[z - k + 1 + 1] - covSum[repeatEnd - k + 1 + 1]) / (z - repeatEnd);
                double repeatMeanCov = (covSum[repeatEnd - k + 1 + 1] - covSum[repeatBegin]) / (repeatEnd - repeatBegin + 1 - k + 1);

                double pc = calculateCertainRepeatProbability(repeatBegin, repeatEnd, z, covSmooth, Sc, aUMeanCov, bUMeanCov, repeatMeanCov);
                maxP = Math.max(maxP, pc);
            }
        }
        
        if (GraphSimplification.shouldDebugOutput(from, to)) {
            for (int repeatBegin = 1; repeatBegin <= x; repeatBegin++) {
                for (int repeatEnd = y; repeatEnd < z; repeatEnd++) {
                    double aUMeanCov = covSum[repeatBegin] / repeatBegin;
                    double bUMeanCov = (covSum[z - k + 1 + 1] - covSum[repeatEnd - k + 1 + 1]) / (z - repeatEnd);
                    double repeatMeanCov = (covSum[repeatEnd - k + 1 + 1] - covSum[repeatBegin]) / (repeatEnd - repeatBegin + 1 - k + 1);

                    double pc = calculateCertainRepeatProbability(repeatBegin, repeatEnd, z, covSmooth, Sc, aUMeanCov, bUMeanCov, repeatMeanCov);
                    if (pc == maxP) {
                        System.out.println("Found maximum p for repeat begin = " + repeatBegin + ", end = " + repeatEnd);
                        shouldDebugOutput = true;
                        calculateCertainRepeatProbability(repeatBegin, repeatEnd, z, cov, Sc, aUMeanCov, bUMeanCov, repeatMeanCov);
                        shouldDebugOutput = false;
                    }
                }
            }
        }

        return maxP;
    }



    double calculateCertainRepeatProbability(int repeatBegin, int repeatEnd, int z, double[] cov, double Sc,
                                             double aUMeanCov, double bUMeanCov, double repeatMeanCov) {
        if (shouldDebugOutput) {
            System.out.println();
            System.out.println("Calculate certain repeat probability, begin = " + repeatBegin + ", end = " + repeatEnd);
            System.out.println("aUMeanCov = " + String.format("%.1f", aUMeanCov) + ", bUMeanCov = " + String.format("%.1f", bUMeanCov) + ", " +
                    "repeatMeanCov = " + String.format("%.1f", repeatMeanCov));
            double S2 = aUMeanCov * repeatBegin + bUMeanCov * (z - repeatEnd) + repeatMeanCov * (repeatEnd - repeatBegin + 1 - k + 1);
            System.out.println("S2 = " + String.format("%.1f", S2));
        }

        if (repeatMeanCov < aUMeanCov + bUMeanCov) {
            repeatMeanCov = aUMeanCov + bUMeanCov;
        }

        // calculating wrong area
        String covS = "";
        String eCovS = "";
        double wrongArea = 0;
        for (int i = 0; i <= z - k + 1; i++) {
            double eCov;
            if (i < repeatBegin) {
                eCov = aUMeanCov;
            } else if (i <= repeatEnd - k + 1) {
                eCov = repeatMeanCov;
            } else {
                eCov = bUMeanCov;
            }

            wrongArea += Math.abs(cov[i] - eCov);

            if (shouldDebugOutput) {
                covS += " " + (int) cov[i];
                eCovS += " "  + (int) eCov;
            }
        }

        double wrongAreaPart = wrongArea / Sc;
        double p = 1 - wrongAreaPart;

        if (shouldDebugOutput) {
            System.out.println("wrong area        = " + String.format("%.5f", wrongArea));
            System.out.println("wrong area part   = " + String.format("%.5f", wrongAreaPart));
            System.out.println("p                 = " + String.format("%.5f", p));
            System.out.println();
            System.out.println("Cov string:");
            System.out.println(covS);
            System.out.println("E cov string:");
            System.out.println(eCovS);
            System.out.println();
        }

        return p;
    }


    private KmerIteratorFactory<Kmer> factory = new ShortKmerIteratorFactory();

    private int[] getCoverage(Dna dna) {
        int[] cov = new int[dna.length() - k + 1];
        Iterator<Kmer> iterator = factory.kmersOf(dna, k).iterator();
        for (int i = 0; i < cov.length; i++) {
            cov[i] = hm.get(iterator.next().toLong());
        }
        if (iterator.hasNext()) {
            throw new RuntimeException("AAAAAA");
        }
        return cov;
    }

    public static double[] smoothOut(double[] vs, double variance) {
        NormalDistribution distribution = new NormalDistribution(0, variance);

        double[] nvs = new double[vs.length];
        for (int i = 0; i < nvs.length; i++) {
            for (int di = -10; di <= 10; di++) {
                int ni = i + di;
                if (ni < 0) {
                    ni = 0;
                }
                if (ni >= vs.length) {
                    ni = vs.length - 1;
                }
                double v = vs[ni];
                nvs[i] += v * distribution.getProb(di);
            }
        }

        return nvs;
    }
}
