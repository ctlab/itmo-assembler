package ru.ifmo.genetics.tools.scaffolding;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Erf;

import ru.ifmo.genetics.tools.scaffolding.stupid.Tester;

/**
 * Created with IntelliJ IDEA. User: niyaz.nigmatullin Date: 31.05.12 Time:
 * 14:46 To change this template use File | Settings | File Templates.
 */
public class MostProbableDistance {

	static final double NORMAL_DISTRIBUTION_CENTER = 3000;
	static final double NORMAL_DISTRIBUTION_DEVIATION = 0.08 * NORMAL_DISTRIBUTION_CENTER;
	static final double NORMAL_DISTRIBUTION_DEVIATION_SQUARED = NORMAL_DISTRIBUTION_DEVIATION
			* NORMAL_DISTRIBUTION_DEVIATION;
	static final int READ_LENGTH = 36;

	public static int getMostProbableDistanceSlow(final int contigLength1,
			final int contigLength2, final int[] d1, final int[] d2,
			final int dnaLength, final int allReads) throws MathException {
		double maximalProbability = Double.NEGATIVE_INFINITY;
		int bestDistance = -1;
		for (int d = 1000; d <= 4000; d++) {
			double currentProbability = getProbabilityThatAllMatepairsMatch(d,
					contigLength1, contigLength2, d1, d2, dnaLength, allReads);
			if (currentProbability > maximalProbability) {
				maximalProbability = currentProbability;
				bestDistance = d;
			}
		}
		return bestDistance;
	}

	public static int getMostProbableDistanceFast(final int contigLength1,
			final int contigLength2, final int[] d1, final int[] d2,
			final int dnaLength, final int allReads) throws MathException {

		int left = 0;
		int right = dnaLength;
		while (left < right - 2) {
			int m1 = (int) ((0L + left + left + right) / 3);
			int m2 = (int) ((0L + left + right + right) / 3);
			double f1 = getProbabilityThatAllMatepairsMatch(m1, contigLength1,
					contigLength2, d1, d2, dnaLength, allReads);
			double f2 = getProbabilityThatAllMatepairsMatch(m2, contigLength1,
					contigLength2, d1, d2, dnaLength, allReads);
			if (f1 > f2) {
				right = m2;
			} else {
				left = m1;
			}
		}
		double maximalProbability = Double.NEGATIVE_INFINITY;
		int bestDistance = -1;
		for (int d = left; d <= right; d++) {
			double f = getProbabilityThatAllMatepairsMatch(d, contigLength1,
					contigLength2, d1, d2, dnaLength, allReads);
			if (f > maximalProbability) {
				maximalProbability = f;
				bestDistance = d;
			}
		}
		return bestDistance;
	}

	public static int getMostProbableDistanceSlowVeryFast(
			final int contigLength1, final int contigLength2, final int[] d1,
			final int[] d2, final int dnaLength, final int allReads)
			throws MathException {
		double aSum = 0;
		double aSq = 0;
		double cRev = 0;
		double cSqRev = 0;
		double cSum = 0;

		for (int i = 0; i < d1.length; i++) {
			double a = d1[i] + d2[i] + 2 * READ_LENGTH
					- NORMAL_DISTRIBUTION_CENTER;
			aSum += a;
			aSq += a * a;
			double c = dnaLength - d1[i] - d2[i] - 2 * READ_LENGTH + 1;
			cRev += 1.0 / c;
			cSqRev += 1.0 / c / c;
			cSum += Math.log(c);
		}

		double maximalProbability = Double.NEGATIVE_INFINITY;
		int bestDistance = -1;
		for (int d = 1000; d <= 4000; d++) {
			double currentProbability = getProbabilityThatAllMatepairsMatchVeryFast(
					d, contigLength1, contigLength2, dnaLength, allReads,
					d1.length, aSum, aSq, cRev, cSqRev, cSum);
			if (currentProbability > maximalProbability) {
				maximalProbability = currentProbability;
				bestDistance = d;
			}
		}
		return bestDistance;
	}

	public static int getMostProbableDistanceFastVeryFast(
			final int contigLength1, final int contigLength2, final int[] d1,
			final int[] d2, final int dnaLength, final int allReads)
			throws MathException {

		double aSum = 0;
		double aSq = 0;
		double cRev = 0;
		double cSqRev = 0;
		double cSum = 0;

		for (int i = 0; i < d1.length; i++) {
			double a = d1[i] + d2[i] + 2 * READ_LENGTH
					- NORMAL_DISTRIBUTION_CENTER;
			aSum += a;
			aSq += a * a;
			double c = dnaLength - d1[i] - d2[i] - 2 * READ_LENGTH + 1;
			cRev += 1.0 / c;
			cSqRev += 1.0 / c / c;
			cSum += Math.log(c);
		}

		int left = 0;
		int right = (int) (10 * NORMAL_DISTRIBUTION_CENTER);
		while (left < right - 2) {
			int m1 = (int) ((0L + left + left + right) / 3);
			int m2 = (int) ((0L + left + right + right) / 3);
			double f1 = getProbabilityThatAllMatepairsMatchVeryFast(m1,
					contigLength1, contigLength2, dnaLength, allReads,
					d1.length, aSum, aSq, cRev, cSqRev, cSum);
			double f2 = getProbabilityThatAllMatepairsMatchVeryFast(m2,
					contigLength1, contigLength2, dnaLength, allReads,
					d1.length, aSum, aSq, cRev, cSqRev, cSum);
			if (f1 > f2) {
				right = m2;
			} else {
				left = m1;
			}
		}
		double maximalProbability = Double.NEGATIVE_INFINITY;
		int bestDistance = -1;
		for (int d = left; d <= right; d++) {
			double f = getProbabilityThatAllMatepairsMatchVeryFast(d,
					contigLength1, contigLength2, dnaLength, allReads,
					d1.length, aSum, aSq, cRev, cSqRev, cSum);
			if (f > maximalProbability) {
				maximalProbability = f;
				bestDistance = d;
			}
		}
		System.out.println(cSum);
		System.out.println(maximalProbability + " "
				+ Math.exp(maximalProbability));
		System.err.println("n = " + " " + d1.length);
		if (d1.length > 1) {
			double s = 0;
			for (int i = 0; i < d2.length; i++) {
				s += (d1[i] + d2[i] + bestDistance + 2 * READ_LENGTH - NORMAL_DISTRIBUTION_CENTER)
						* (d1[i] + d2[i] + bestDistance + 2 * READ_LENGTH - NORMAL_DISTRIBUTION_CENTER);
			}
			s = Math.sqrt(s / (d1.length - 1));
			Tester.out
					.println("interval = "
							+ Math.round(bestDistance - 1.96 * s
									/ Math.sqrt(d1.length))
							+ ".."
							+ (Math.round(bestDistance + 1.96 * s
									/ Math.sqrt(d1.length))));
			Tester.out.flush();
		}
		return bestDistance;
	}

	public static double getProbabilityThatAtLeastOneMatepairMatches(int d,
			int contigLength1, int contigLength2, int dnaLength)
			throws MathException {
		double prob = 0;
		for (int i = d + 2 * READ_LENGTH;; i++) {
			int w = weight(i, contigLength1, contigLength2, d);
			if (w == 0) {
				break;
			}
			prob += w * pNorm(i) / (dnaLength - i + 1);
		}
		return prob;
	}

	public static double getProbabilityThatAtLeastOneMatepairMatchesVeryFast(
			int d, int contigLength1, int contigLength2, int dnaLength)
			throws MathException {

		int start = 2 * READ_LENGTH + d;
		int left = d + READ_LENGTH + Math.min(contigLength1, contigLength2);
		int right = d + READ_LENGTH + Math.max(contigLength1, contigLength2);
		int finish = contigLength1 + contigLength2 + d;

		int b1 = -(start) + 1;
		int b2 = Math.min(contigLength1, contigLength2) - READ_LENGTH + 1;
		int b3 = finish + 1;

		double erfs = getERF(start);
		double erfl = getERF(left);
		double erfr = getERF(right);
		double erff = getERF(finish);

		double prob = 2.0 * NORMAL_DISTRIBUTION_DEVIATION / sqrt2Pi
				* (exp(start) - exp(left) - exp(right) + exp(finish));

		prob += erfs * (b1 + NORMAL_DISTRIBUTION_CENTER);
		prob += erfl * (b2 - b1 - NORMAL_DISTRIBUTION_CENTER);
		prob += erfr * (b3 - NORMAL_DISTRIBUTION_CENTER - b2);
		prob += erff * (-b3 + NORMAL_DISTRIBUTION_CENTER);

		return prob / dnaLength / 2.0;
	}

	public static double getProbabilityThatAllMatepairsMatch(int d,
			int contigLength1, int contigLength2, int[] d1, int[] d2,
			int dnaLength, int allReads) throws MathException {
		double p = getProbabilityThatAtLeastOneMatepairMatches(d,
				contigLength1, contigLength2, dnaLength);
		double ret = (allReads - d1.length) * Math.log(1 - p);
		for (int i = 0; i < d1.length; i++) {
			double z = (d + d1[i] + d2[i] + 2 * READ_LENGTH - NORMAL_DISTRIBUTION_CENTER);
			ret -= z * z / (2 * NORMAL_DISTRIBUTION_DEVIATION_SQUARED);
			ret -= Math.log(dnaLength - (d + d1[i] + d2[i] + 2 * READ_LENGTH)
					+ 1);
		}
		return ret;
	}

	public static double getProbabilityThatAllMatepairsMatchVeryFast(int d,
			int contigLength1, int contigLength2, int dnaLength, int allReads,
			int n, double aSum, double aSq, double cRev, double cSqRev,
			double cSum) throws MathException {
		double p = getProbabilityThatAtLeastOneMatepairMatchesVeryFast(d,
				contigLength1, contigLength2, dnaLength);
		double ret1 = (allReads - n) * Math.log(1 - p);
		double ret2 = -(1.0 * d * d * n + 2 * d * aSum + aSq) / 2
				/ NORMAL_DISTRIBUTION_DEVIATION_SQUARED
				+ Math.log(1.0 / sqrt2Pi) * n;
		double ret3 = (cSqRev / 2.0 * d + cRev) * d - cSum;
		// System.out.println(ret1 + " " + ret2 + " " + ret3);
		double ret4 = -(d - CENTER) * (d - CENTER) / 2 / DISP / DISP
				- Math.log(sqrt2Pi * DISP);
		return (ret1 + ret2 + ret3 + ret4);
	}

	final static double CENTER = 2900;
	final static double DISP = 500;

	public static int weight(final int len, final int contigLength1,
			final int contigLength2, final int d) {
		if (contigLength1 + contigLength2 + d < len
				|| d > len - 2 * READ_LENGTH) {
			return 0;
		}
		int begin = Math.max(0, contigLength1 + d + READ_LENGTH - len);
		int end = Math.min(contigLength1 - READ_LENGTH, contigLength1 + d
				+ contigLength2 - len);
		return Math.max(0, end - begin + 1);
	}

	public static double gaussDistributionFunction(double x)
			throws MathException {
		return (1 + Erf.erf((x - NORMAL_DISTRIBUTION_CENTER)
				/ (NORMAL_DISTRIBUTION_DEVIATION * Math.sqrt(2)))) * .5;
	}

	public static double pNorm(int minLength, int maxLength)
			throws MathException {
		return gaussDistributionFunction(maxLength + 0.5)
				- gaussDistributionFunction(minLength - 0.5);
	}

	public static double pNorm(int length) throws MathException {
		return pNorm(length, length);
	}

	public static int getMostProbableDistanceAverage(int contigLength1,
			int contigLength2, int[] d1, int[] d2, int dnaLength, int n) {
		double s = 0;
		for (int i = 0; i < d1.length; i++) {
			s += NORMAL_DISTRIBUTION_CENTER - d1[i] - d2[i] - 2 * READ_LENGTH;
		}
		s /= d1.length;
		return (int) s;
	}

	private static final double sqrt2 = Math.sqrt(2);
	private static final double sqrt2Pi = Math.sqrt(2 * Math.PI);

	private static double exp(int x) {
		return Math.exp(-(x - NORMAL_DISTRIBUTION_CENTER)
				* (x - NORMAL_DISTRIBUTION_CENTER) / 2
				/ NORMAL_DISTRIBUTION_DEVIATION_SQUARED);
	}

	private static double getERF(double x) throws MathException {
		return org.apache.commons.math.special.Erf
				.erf((NORMAL_DISTRIBUTION_CENTER - x) / sqrt2
						/ NORMAL_DISTRIBUTION_DEVIATION);
	}

}
