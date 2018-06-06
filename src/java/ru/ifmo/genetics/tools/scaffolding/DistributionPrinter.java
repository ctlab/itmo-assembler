package ru.ifmo.genetics.tools.scaffolding;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Erf;

import java.util.Map;
import java.util.TreeMap;

public class DistributionPrinter {
    private final int l1, l2;
    private final int d;
    private final double meanInsertLength, insertLengthDeviation;
    private final int readLength;

    public DistributionPrinter(int l1, int l2, int d, 
                                double meanInsertLength,
                                double insertLengthRelativeDeviation,
                                int readLength) {
        this.l1 = l1;
        this.l2 = l2;
        this.d = d;
        this.meanInsertLength = meanInsertLength;
        this.insertLengthDeviation = meanInsertLength * insertLengthRelativeDeviation;
        this.readLength = readLength;
    }

    public void run() {
        Map<Integer, Double> ps = new TreeMap<Integer, Double>();
        try {
            double p0 = 0;
            // more fine
            for (int d1 = readLength; d1 <= l1; ++d1)
            {
                for (int d2 = readLength; d2 <= l2; ++d2)
                {
                    int l = d1 + d2 + d;
                    if (!ps.containsKey(d1 + d2)) {
                        ps.put(d1 + d2, 0.);
                    }
                    ps.put(d1 + d2, ps.get(d1 + d2) + pNorm(l));
                    p0 += pNorm(l);
                }
            }
            for (Map.Entry<Integer, Double> e: ps.entrySet()) {
                System.out.println(e.getKey() + " " + (e.getValue() / p0));
            }
        } catch (MathException e) {
            System.err.println("Failed");
        }
    }

    private double gaussDistributionFunction(double x) throws MathException {
        return (1 + Erf.erf((x - meanInsertLength) / insertLengthDeviation / Math.sqrt(2))) / 2;
    }

    private double pNorm(int minLength, int maxLength) throws MathException {
        return gaussDistributionFunction(maxLength + 0.5) - 
               gaussDistributionFunction(minLength - 0.5);
    }

    private double pNorm(int length) throws MathException {
        return pNorm(length, length);
    }

    public static void main(String[] args) {
        if (args.length != 6) {
            System.err.println("usage: generate_connections <l1> <l2> <d> <mean-insert-length> <relative-deviation> <read-length>");
            System.exit(1);
        }

        new DistributionPrinter(Integer.parseInt(args[0]),
                                 Integer.parseInt(args[1]),
                                 Integer.parseInt(args[2]),
                                 Double.parseDouble(args[3]),
                                 Double.parseDouble(args[4]),
                                 Integer.parseInt(args[5])).run();

    }

}
