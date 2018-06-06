package ru.ifmo.genetics.statistics;

public class NormalDistribution {
    public final int center;
    public final double variance;
    public final double sigma;

    public NormalDistribution(int center, double variance) {
        this.center = center;
        this.variance = variance;
        this.sigma = Math.sqrt(variance);
    }

    private static double SQRT2PI = Math.sqrt(2 * Math.PI);

    public double getProb(int x) {
        x -= center;
        double ans = 1 / (sigma * SQRT2PI);
        ans *= Math.exp(- 0.5 * Math.pow(x / sigma, 2));
        return ans;
    }
}
