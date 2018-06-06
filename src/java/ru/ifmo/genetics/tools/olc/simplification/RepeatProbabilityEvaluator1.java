package ru.ifmo.genetics.tools.olc.simplification;

import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.statistics.ExponentialDistribution;

import java.util.ArrayList;

/**
 * Repeat probability evaluator, evaluates probability using repeat statistic model.
 */
public class RepeatProbabilityEvaluator1 {

    final ArrayList<Dna> reads;
    final int genomeLen;

    public RepeatProbabilityEvaluator1(ArrayList<Dna> reads, int genomeLen) {
        this.reads = reads;
        this.genomeLen = genomeLen;
    }


    public double calculateRepeatProbability(int from, int to, int beginShift) {
        if (beginShift < 0) {
            return calculateRepeatProbability(to, from, -beginShift);
        }

        int x = beginShift;
        int y = reads.get(from).length() - 1;
        int z = beginShift + reads.get(to).length() - 1;

//        return calculateRepeatProbability0(x, y, z);
//        return 1.0 / (y - x + 1);
        return calculateCertainRepeatProbability(y - x + 1);
    }


    /**
     * first read  -   |---------|
     * second read -        |----------|
     * position    -   0    x    y     z
     *
     * <br></br><br></br>
     * Attention!  First read starts at 0 and ends at Y INCLUSIVE.
     *             Second read starts at X and ends at Z INCLUSIVE.
     *
     */
    private double calculateRepeatProbability0(int x, int y, int z) {
        assert (0 <= x) && (x <= y) && (y <= z);
        return calculateRepeatProbability1(x, y, z);
    }

    private double calculateRepeatProbability1(int x, int y, int z) {
        double p = 0;

        double rp = 1;
        for (int b = 1; b <= x; b++) {
            double pc = calculateRepeatProbability2(x, y, z, b);

            p += rp * pc;
            rp *= 1 - pc;
        }

        return p;
    }

    private double calculateRepeatProbability2(int x, int y, int z, int rb) {
        double p = 0;

        double rp = 1;
        for (int e = z - 1; e >= y; e--) {
            double pc = calculateCertainRepeatProbability(e - rb + 1);

            p += rp * pc;
            rp *= 1 - pc;
        }

        return p;
    }

    private static final ExponentialDistribution expDistrib = ExponentialDistribution.createWithLambda(40, 1, 0.01, 700);

    /**
     * Probability of certain repeat at some positions X and Y with len LEN.
     */
    double calculateCertainRepeatProbability(int len) {

        // approximation only for E.Coli
        double expectedCount = expDistrib.getProb(len);
//        double p = count * 1.0 / genomeLen / genomeLen;
        double p = 650000 * expectedCount / genomeLen;
        if (p > 1) {
            p = 1;
        }
//        System.out.println("In calc_certain, len = " + len + ", count = " + count + ", p = " + String.format("%.10f", p));

        return p;

//        if (len == 4) {
//            return 1.0 / genomeLen / genomeLen;
//        }
//        return 0;
    }



    public static void main(String[] args) {
        System.out.println("Calculating probability using repeat statistic model...");

        int genomeLen = 4500000;

        int aLen = 200;
        int bLen = 200;
        int ovLen = 99;
        int bs = aLen - ovLen;

        int x = bs;
        int y = aLen - 1;
        int z = bs + bLen - 1;

        RepeatProbabilityEvaluator1 evaluator = new RepeatProbabilityEvaluator1(null, genomeLen);
        double p = evaluator.calculateRepeatProbability0(x, y, z);

        System.out.println();
        System.out.println("Genome len = " + genomeLen);
        System.out.println("a.len = " + aLen + ", b.len = " + bLen + ", bShift = " + bs + ", ovLen = " + ovLen);
        System.out.println("X = " + x + ", Y = " + y + ", Z = " + z);
        System.out.println("P = " + String.format("%.5f%%", 100.0 * p));
    }
}
