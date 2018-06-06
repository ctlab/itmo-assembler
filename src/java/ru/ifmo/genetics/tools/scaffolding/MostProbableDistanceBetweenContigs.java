package ru.ifmo.genetics.tools.scaffolding;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Erf;
import ru.ifmo.genetics.utils.NumUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;


public class MostProbableDistanceBetweenContigs {
	
	public static double MU = 3000;
	public static double SIGMA = 0.08 * MU;
	public static int GENOME_LENGTH = (int) 1.8e9;
	public static int MP_READ_LENGTH = 36;
	public static int MP_READS_COUNT = (int) 2e8; 
    public static int MAX_D = (int)(2 * MU);
	private static double[] normalDistribution;
	
	public static void main(String[] args) throws IOException {
        if (args.length != 7) {
            System.err.println("usage: get_distance <N> <L> <l1> <l2> <mean-insert-length> <relative-deviation> <read-length> < <pairs>");
            System.exit(1);
        }

        MP_READS_COUNT = Integer.parseInt(args[0]);
        GENOME_LENGTH = Integer.parseInt(args[1]);
		int l1 = Integer.parseInt(args[2]);
		int l2 = Integer.parseInt(args[3]);
        MU = Double.parseDouble(args[4]);
        SIGMA = Double.parseDouble(args[5]) * MU;
        MP_READ_LENGTH = Integer.parseInt(args[6]);
        MAX_D = (int)(2 * MU);

		PrintWriter out = new PrintWriter(new FileWriter(new File("likelihood-function.txt")));
		
		normalDistribution = new double[2 * (int) MU + 1];
		for (int i = 0; i <= 2 * MU; i++) {
			normalDistribution[i] = 1 / (SIGMA * Math.sqrt(2 * Math.PI)) * Math.exp(-(i - MU) * (i - MU) / (2 * SIGMA * SIGMA));
		}
		normalDistribution = NumUtils.normalize(normalDistribution);

        Scanner pairsScanner = new Scanner(System.in);

		Integer[] d1;
		Integer[] d2;
		
        {
            ArrayList<Integer> d1list = new ArrayList<Integer>();
            ArrayList<Integer> d2list = new ArrayList<Integer>();

            while (pairsScanner.hasNext()) {
                d1list.add(pairsScanner.nextInt());
                d2list.add(pairsScanner.nextInt());
            }

            d1 = d1list.toArray(new Integer[d1list.size()]);
            d2 = d2list.toArray(new Integer[d2list.size()]);
        }
            
        int n = d1.length;

		// arithmetical mean
        int averageD = -1;
		{
			int total = 0;
			for (int i = 0; i < d1.length; i++) {
				total += MU - d1[i] - d2[i] - 2 * MP_READ_LENGTH;
			}
            averageD = (int)Math.round(1.0 * total / n);
			System.out.println("Average: " + 1.0 * total / d1.length);
		}

		double maxLikelihood = Double.NEGATIVE_INFINITY;
		int mpd = -1;
		
        double[] likelihood = new double[MAX_D + 1];
		for (int d = 0; d <= MAX_D; d++) {
            double p0 = calcPL1L2D(l1, l2, d);
            likelihood[d] = - MP_READS_COUNT * p0 + n * Math.log(MP_READS_COUNT);
            for (int i = 0; i < n; ++i) {
                likelihood[d] += Math.log(pNorm(d1[i] + d2[i] + d + 2 * MP_READ_LENGTH));
                // likelihood[d] -= Math.log(GENOME_LENGTH - (d1[i] + d2[i] + d));
            }
            /*
			double likelihood = (MP_READS_COUNT - d1.length) * Math.log(1 - calcPL1L2D(l1, l2, d));
			for (int i = 0; i < d1.length; i++) {
				likelihood -= (d + d1[i] + d2[i] + 2 * MP_READ_LENGTH - MU) * 
                              (d + d1[i] + d2[i] + 2 * MP_READ_LENGTH - MU) / 
                              (2 * SIGMA * SIGMA);
				likelihood -= Math.log(GENOME_LENGTH - (d + d1[i] + d2[i] + 2 * MP_READ_LENGTH) + 1);
			}
            */
			if (likelihood[d] > maxLikelihood) {
				maxLikelihood = likelihood[d];
				mpd = d;
			}
			out.println(likelihood[d]);
		}

        int leftEnd = -1;
        for (int i = 0; i <= MAX_D; ++i) {
            if (likelihood[i] >= maxLikelihood - 1) {
                leftEnd = i;
                break;
            }
        }
        int rightEnd = -1;
        for (int i = MAX_D; i >= 0; --i) {
            if (likelihood[i] >= maxLikelihood - 1) {
                rightEnd = i;
                break;
            }
        }
		System.out.println("Maximal likelihood distance taking number of mp-reads into account: " + mpd);
		System.out.println("interval: [" + leftEnd + ", " + rightEnd + "]");

        double p0 = calcPL1L2D(l1, l2, mpd);

        int expectedN = (int)Math.round(MP_READS_COUNT * p0);

        System.err.println(expectedN + " expected, " + n + " got");

        double relativeLikelihood = (n - expectedN) * Math.log(MP_READS_COUNT * p0);

        for (int i = 2; i < n; ++i) {
            relativeLikelihood -= Math.log(i);
        }

        for (int i = 2; i < expectedN; ++i) {
            relativeLikelihood += Math.log(i);
        }

        double lensSum = 0;
        for (int i = 0; i < n; ++i) {
            lensSum += d1[i] + d2[i] + 2 * MP_READ_LENGTH + mpd;
        }

        double meanLen = lensSum / n;

        double squaresOfDeviation = 0;
        for (int i = 0; i < n; ++i) {
            squaresOfDeviation += Math.pow(d1[i] + d2[i] + 2 * MP_READ_LENGTH + mpd - meanLen, 2);
        }

        double variance = Math.sqrt(squaresOfDeviation / (n - 1));

        System.err.println("Parameters of lenghts distributions: " + meanLen + " +- " + variance);
        System.err.println("Real mean length: " + getRealMeanLength(l1, l2, mpd));
        System.err.println("relative likelihood (by number of mp's): " + relativeLikelihood);

		out.close();
		
	}

    private static double getRealMeanLength(int l1, int l2, int d) {
        double res = 0;
        double pSum = 0;
        for (int l = d + 2 * MP_READ_LENGTH; l <= l1 + l2 + d; ++l) {
            double p = weight(l1, l2, d, l) * pNorm(l);
            res += l * p;
            pSum += p;
        }
        return res / pSum;
    }

    private static int weight(int l1, int l2, int d, int l) {
        if (l < d + 2 * MP_READ_LENGTH)
            return 0;

        if (l > l1 + l2 + d)
            return 0;

        int minLeftOffset = Math.max(0, l1 + d + MP_READ_LENGTH - l);
        int minRightOffset = Math.max(0, l2 + d + MP_READ_LENGTH - l);
        int maxLeftOffset = l1 + l2 + d - minRightOffset - l;
        return maxLeftOffset - minLeftOffset + 1;
    }

	private static double calcPL1L2D(int l1, int l2, int d) {
        double p0 = 0;
        for (int d1 = MP_READ_LENGTH; d1 <= l1; ++d1)
        {
            p0 += pNorm(d1 + d + MP_READ_LENGTH, d1 + d + l2);
        }
        p0 /= GENOME_LENGTH;
        return p0;

        /*
		double result = 0;
		if (l2 < l1) {
			for (int l = 0; l <= l1 + l2 + d; l++) {
				double lengthProb = 0; 
				if (l <= 2 * MU) {
					lengthProb = normalDistribution[l] / (GENOME_LENGTH - l + 1);
				}
				if (l >= d + 72 && l < l2 + d + 36) {
					result += lengthProb * (l - d - 72 + 1);
				} else if (l >= l2 + d + 36 && l < l1 + d + 36) {
					result += lengthProb * (l2 - 35);
				} else if (l >= l1 + d + 36 && l <= l1 + l2 + d) {
					result += lengthProb * (l1 + l2 + d - l + 1);
				}	
			}
		} else {
			for (int l = 0; l <= l1 + l2 + d; l++) {
				double lengthProb = 0; 
				if (l <= 2 * MU) {
					lengthProb = normalDistribution[l] / (GENOME_LENGTH - l + 1);
				}
				if (l >= d + 72 && l < l1 + d + 36) {
					result += lengthProb * (l - d - 72 + 1);
				} else if (l >= l1 + d + 36 && l < l2 + d + 36) {
					result += lengthProb * (l1 - 35);
				} else if (l >= l2 + d + 36 && l <= l1 + l2 + d) {
					result += lengthProb * (l1 + l2 + d - l + 1);
				}
			}
		}
		return result;
        */
	}
	
    private static double gaussDistributionFunction(double x) throws MathException {
        return (1 + Erf.erf((x - MU) / SIGMA / Math.sqrt(2))) / 2;
    }

    private static double pNorm(int minLength, int maxLength) {
        try {
            return gaussDistributionFunction(maxLength + 0.5) - 
                gaussDistributionFunction(minLength - 0.5);
        } catch (MathException e) {
            throw new RuntimeException("pNorm failed with exception: " + e);
        }
    }

    private static double pNorm(int length) {
        return pNorm(length, length);
    }
}
