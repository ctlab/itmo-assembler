package ru.ifmo.genetics.statistics;

public class ExponentialDistribution {
    public final int from, direction;
    public final double lambda;
    public final double multiplier;
    
    private ExponentialDistribution(int from, int direction, double lambda, double multiplier) {
        this.from = from;
        this.direction = direction;
        this.lambda = lambda;
        this.multiplier = multiplier;
    }

    public static ExponentialDistribution createWithVariance(int from, int direction, double variance) {
        double lambda = Math.pow(variance, -0.5);    // variance = lambda^(-2) for exp. distribution
        return createWithLambda(from, direction, lambda);
    }
    public static ExponentialDistribution createWithLambda(int from, int direction, double lambda) {
        return createWithLambda(from, direction, lambda, 1);
    }
    public static ExponentialDistribution createWithLambda(int from, int direction, double lambda, double multiplier) {
        return new ExponentialDistribution(from, direction, lambda, multiplier);
    }

    public double getProb(int x) {
        x -= from;
        x *= direction;
        if (x < 0) {
            return 0;
        }
        double ans = multiplier * lambda * Math.exp(-lambda * x);
        return ans;
    }
}
