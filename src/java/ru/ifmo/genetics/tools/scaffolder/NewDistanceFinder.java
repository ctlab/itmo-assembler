package ru.ifmo.genetics.tools.scaffolder;
import static java.lang.Math.exp;
import static org.apache.commons.math.special.Erf.erf;

import org.apache.commons.math.MathException;

public class NewDistanceFinder {

	public static final double NORMAL_DISTRIBUTION_CENTER = 3000;
	public static final double NORMAL_DISTRIBUTION_DEVIATION = 0.1 * NORMAL_DISTRIBUTION_CENTER;
	public static final double NORMAL_DISTRIBUTION_DEVIATION_SQUARED = NORMAL_DISTRIBUTION_DEVIATION
			* NORMAL_DISTRIBUTION_DEVIATION;
	public static final int READ_LENGTH = 36;

	public static int getMostProbableDistanceAverage(int contigLength1,
			int contigLength2, int[] d1, int[] d2, int dnaLength, int n) {
		double s = 0;
		for (int i = 0; i < d1.length; i++) {
			s += NORMAL_DISTRIBUTION_CENTER - d1[i] - d2[i] - 2 * READ_LENGTH;
		}
		s /= d1.length;

		return (int) s;
	}

	public static int getMostProbableDistance(final int contigLength1,
			final int contigLength2, final int[] d1, final int[] d2,
			final int dnaLength, final int allReads) throws MathException {

		double dSq = dSq(d1, d2, dnaLength);
		double dLin = dLin(d1, d2, dnaLength);

		return getMostProbableDistance(contigLength1, contigLength2, dnaLength,
				allReads, d1.length, dSq, dLin);
	}

	private static final double sqrt2 = Math.sqrt(2);
	private static final double sqrt2Pi = Math.sqrt(2 * Math.PI);

	private static int getMostProbableDistance(int contigLength1,
			int contigLength2, int dnaLength, int allReads, int n, double dSq,
			double dLin) throws MathException {
		int left = (int) (-2 * NORMAL_DISTRIBUTION_CENTER);
		int right = (int) (10 * NORMAL_DISTRIBUTION_CENTER);
		while (right - left > 1) {
			int m = (left + right) / 2;
			double f = getDerivativeThatAllMatepairsMatch(m, contigLength1,
					contigLength2, dnaLength, allReads, n, dSq, dLin);
			if (f < 0) {
				right = m;
			} else {
				left = m;
			}
		}
		double fl = getDerivativeThatAllMatepairsMatch(left, contigLength1,
				contigLength2, dnaLength, allReads, n, dSq, dLin);
		double fr = getDerivativeThatAllMatepairsMatch(right, contigLength1,
				contigLength2, dnaLength, allReads, n, dSq, dLin);
		return Math.abs(fl) < Math.abs(fr) ? left : right;
	}

	public static double dSq(int[] d1, int[] d2, int dnaLength) {
		return -d1.length / (2 * NORMAL_DISTRIBUTION_DEVIATION_SQUARED) + 0
				* cSqRev(d1, d2, dnaLength) / 2;
	}

	public static double dLin(int[] d1, int[] d2, int dnaLength) {
		return aSum(d1, d2) / NORMAL_DISTRIBUTION_DEVIATION_SQUARED + 0
				* cRev(d1, d2, dnaLength);
	}

	public static double dCon(int[] d1, int[] d2, int dnaLength, int l1, int l2) {
		double w = 0;
		for (int i = 0; i < d2.length; i++) {
			w += Math
					.log(Math.max(
							0,
							Math.min(l1 - READ_LENGTH, l1 + l2 - d1[i] - d2[i])
									- Math.max(0, l1 + READ_LENGTH - d1[i]
											- d2[i]) + 1));
		}
		return d1.length
				* Math.log(sqrt2Pi * NORMAL_DISTRIBUTION_DEVIATION)
				- (0 * cLog(d1, d2, dnaLength) + d1.length
						* Math.log(dnaLength)) - aSqSum(d1, d2)
				/ (2 * NORMAL_DISTRIBUTION_DEVIATION_SQUARED) + w * 0;
	}

	public static double getDerivativeThatAllMatepairsMatch(int d,
			int contigLength1, int contigLength2, int dnaLength, int allReads,
			int n, double dSq, double dLin) throws MathException {
		double p = getProbabilityThatAtLeastOneMatepairMatches(d,
				contigLength1, contigLength2, dnaLength);
		double dp = getDerivativeThatAtLeastOneMatepairMatches(d,
				contigLength1, contigLength2, dnaLength);
		double ret1 = -(allReads - n) * dp / (1 - p);
		// ret1 += -n * dp / d;
		double ret2 = 2 * dSq * d + dLin;
		return ret1 + ret2;
	}

	public static double getProbabilityThatAllMatepairsMatch(int d,
			int contigLength1, int contigLength2, int dnaLength, int allReads,
			int n, double dSq, double dLin, double dCon) throws MathException {
		double p = getProbabilityThatAtLeastOneMatepairMatches(d,
				contigLength1, contigLength2, dnaLength);
		double ret1 = (allReads - n) * Math.log(1 - p);
		// if (p > 1e-3) {
		// ret1 += -n * Math.log(p);
		// }
		double ret2 = (dSq * d + dLin) * d + dCon;
		// System.out.println(p + " " + Math.log(p));
		return ret1 + ret2;
	}

	private static double aSum(int[] d1, int[] d2) {
		double aSum = 0;
		for (int i = 0; i < d1.length; i++) {
			double a = NORMAL_DISTRIBUTION_CENTER
					- (d1[i] + d2[i] + 2 * READ_LENGTH);
			aSum += a;
		}
		return aSum;
	}

	private static double cRev(int[] d1, int[] d2, int dnaLength) {
		double cRev = 0;
		for (int i = 0; i < d1.length; i++) {
			double c = dnaLength - d1[i] - d2[i] - 2 * READ_LENGTH + 1;
			cRev += 1.0 / c;
		}
		return cRev;
	}

	private static double cSqRev(int[] d1, int[] d2, int dnaLength) {
		double cSqRev = 0;
		for (int i = 0; i < d1.length; i++) {
			double c = dnaLength - d1[i] - d2[i] - 2 * READ_LENGTH + 1;
			cSqRev += 1.0 / c / c;
		}
		return cSqRev;
	}

	private static double aSqSum(int[] d1, int[] d2) {
		double ans = 0;
		for (int i = 0; i < d1.length; i++) {
			ans += (NORMAL_DISTRIBUTION_CENTER - d1[i] - d2[i] - 2 * READ_LENGTH)
					* (NORMAL_DISTRIBUTION_CENTER - d1[i] - d2[i] - 2 * READ_LENGTH);
		}
		return ans;
	}

	private static double cLog(int[] d1, int[] d2, int dnaLength) {
		double ans = 0;
		for (int i = 0; i < d1.length; i++) {
			ans += Math.log(dnaLength - d1[i] - d2[i] - 2 * READ_LENGTH + 1);
		}
		return ans;
	}

	public static double getProbabilityThatAtLeastOneMatepairMatches(int d,
			int contigLength1, int contigLength2, int dnaLength)
			throws MathException {

		int start = 2 * READ_LENGTH;
		int left = READ_LENGTH + Math.min(contigLength1, contigLength2);
		int right = READ_LENGTH + Math.max(contigLength1, contigLength2);
		int finish = contigLength1 + contigLength2 + d;

		int b1 = (2 * READ_LENGTH + d) - 1;
		int b2 = Math.min(contigLength1, contigLength2) - READ_LENGTH + 1;
		int b3 = contigLength1 + contigLength2 + d + 1;

		double ans = 0;
		ans += intXExp(b1, start, left);

		ans += intExp(d + left, d + right) * b2;

		ans += -intXExp(b3, right, finish);

		return ans / sqrt2Pi / NORMAL_DISTRIBUTION_DEVIATION / dnaLength;
	}

	public static double getDerivativeThatAtLeastOneMatepairMatches(int d,
			int contigLength1, int contigLength2, int dnaLength)
			throws MathException {

		int start = 2 * READ_LENGTH;
		int left = READ_LENGTH + Math.min(contigLength1, contigLength2);
		int right = READ_LENGTH + Math.max(contigLength1, contigLength2);
		int finish = contigLength1 + contigLength2;

		int b1 = (2 * READ_LENGTH + d) - 1;
		int b2 = Math.min(contigLength1, contigLength2) - READ_LENGTH + 1;
		int b3 = contigLength1 + contigLength2 + d + 1;

		double ans = 0;
		ans += dIntXExp(b1, start, left, 1, 0, 0);

		ans += dIntExp(d + left, d + right, 1, 1) * b2;

		ans += -dIntXExp(b3, right, finish, 1, 0, 0);

		return ans / sqrt2Pi / NORMAL_DISTRIBUTION_DEVIATION / dnaLength;
	}

	private static final double sqrtPiDiv2 = Math.sqrt(Math.PI / 2);

	private static double intExp(double c, double d) throws MathException {
		return sqrtPiDiv2
				* NORMAL_DISTRIBUTION_DEVIATION
				* (erf((NORMAL_DISTRIBUTION_CENTER - c) / sqrt2
						/ NORMAL_DISTRIBUTION_DEVIATION) - erf((NORMAL_DISTRIBUTION_CENTER - d)
						/ sqrt2 / NORMAL_DISTRIBUTION_DEVIATION));
	}

	private static double dIntExp(double c, double d, double dc, double dd)
			throws MathException {
		return -exp(sqNorm(c)) * dc + exp(sqNorm(d)) * dd;
	}

	private static double intXExp(double b, double c, double d)
			throws MathException {
		double e = sqNorm(c);
		double f = sqNorm(d);
		return NORMAL_DISTRIBUTION_DEVIATION_SQUARED * (exp(e) - exp(f))
				+ (NORMAL_DISTRIBUTION_CENTER - b) * intExp(c, d);
	}

	private static double dIntXExp(double b, double c, double d, double db,
			double dc, double dd) throws MathException {
		double e = sqNorm(c);
		double de = -(c - NORMAL_DISTRIBUTION_CENTER) * dc;
		double f = sqNorm(d);
		double df = -(d - NORMAL_DISTRIBUTION_CENTER) * dd;
		return NORMAL_DISTRIBUTION_DEVIATION_SQUARED
				* (exp(e) * de - exp(f) * df) + -db * intExp(c, d)
				+ (NORMAL_DISTRIBUTION_CENTER - b) * dIntExp(c, d, dc, db);
	}

	private static double sqNorm(double x) {
		return -(x - NORMAL_DISTRIBUTION_CENTER)
				* (x - NORMAL_DISTRIBUTION_CENTER) / 2
				/ NORMAL_DISTRIBUTION_DEVIATION_SQUARED;
	}
}
