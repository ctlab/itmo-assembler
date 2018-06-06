package ru.ifmo.genetics.tools.scaffolder;
import org.apache.commons.math.MathException;

public class SwedenDistanceFinder {

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
		double dCon = dCon(d1, d2, dnaLength, contigLength1, contigLength2);

		return getMostProbableDistance(contigLength1, contigLength2, dnaLength,
				allReads, d1.length, dSq, dLin, dCon);
	}

	private static final double sqrt2 = Math.sqrt(2);
	private static final double sqrt2Pi = Math.sqrt(2 * Math.PI);

	private static int getMostProbableDistance(int contigLength1,
			int contigLength2, int dnaLength, int allReads, int n, double dSq,
			double dLin, double dCon) throws MathException {
		// int left = (int) (-2 * NORMAL_DISTRIBUTION_CENTER);
		// int right = (int) (10 * NORMAL_DISTRIBUTION_CENTER);
		int left = Integer.MAX_VALUE / 2;
		int right = Integer.MIN_VALUE / 2;
		for (int i = (int) (-2 * NORMAL_DISTRIBUTION_CENTER); i <= 4 * NORMAL_DISTRIBUTION_CENTER; i += NORMAL_DISTRIBUTION_DEVIATION) {
			double q = SwedenDistanceFinder
					.getProbabilityThatAtLeastOneMatepairMatches(i,
							contigLength1, contigLength2, dnaLength);
			if (!Double.isInfinite(Math.log(dnaLength * q))) {
				left = Math.min(left, i);
				right = Math.max(right, i);
			}
		}
		while (right - left > 2) {
			int ml = (2 * left + right) / 3;
			int mr = (left + 2 * right) / 3;
			double fl = getProbabilityThatAllMatepairsMatch(ml, contigLength1,
					contigLength2, dnaLength, allReads, n, dSq, dLin, dCon);
			double fr = getProbabilityThatAllMatepairsMatch(mr, contigLength1,
					contigLength2, dnaLength, allReads, n, dSq, dLin, dCon);
			if (fl < fr) {
				left = ml;
			} else {
				right = mr;
			}
		}
		double f = getProbabilityThatAllMatepairsMatch(left, contigLength1,
				contigLength2, dnaLength, allReads, n, dSq, dLin, dCon);
		int ans = left;
		for (int i = left - 2; i <= right + 1; i++) {
			double fn = getProbabilityThatAllMatepairsMatch(i, contigLength1,
					contigLength2, dnaLength, allReads, n, dSq, dLin, dCon);
			if (fn > f + 1e-8) {
				f = fn;
				ans = i;
			}
		}
		return ans;
	}

	private static double exp(int x) {
		return Math.exp(-(x - NORMAL_DISTRIBUTION_CENTER)
				* (x - NORMAL_DISTRIBUTION_CENTER) / 2
				/ NORMAL_DISTRIBUTION_DEVIATION_SQUARED);
	}

	private static double dExp(int x) {
		return -exp(x) * (x - NORMAL_DISTRIBUTION_CENTER)
				/ NORMAL_DISTRIBUTION_DEVIATION_SQUARED;
	}

	private static double getERF(double x) throws MathException {
		return org.apache.commons.math.special.Erf
				.erf((NORMAL_DISTRIBUTION_CENTER - x) / sqrt2
						/ NORMAL_DISTRIBUTION_DEVIATION);
	}

	public static double dSq(int[] d1, int[] d2, int dnaLength) {
		double res = -d1.length / (2 * NORMAL_DISTRIBUTION_DEVIATION_SQUARED)
				+ cSqRev(d1, d2, dnaLength) / 2;
		if (Double.isNaN(res)) {
			System.err.println("dSq");
		}
		return res;
		// return -d1.length / (2 * NORMAL_DISTRIBUTION_DEVIATION_SQUARED)+ 0
		// * cSqRev(d1, d2, dnaLength) / 2;
	}

	public static double dLin(int[] d1, int[] d2, int dnaLength) {
		double res = aSum(d1, d2) / NORMAL_DISTRIBUTION_DEVIATION_SQUARED
				+ cRev(d1, d2, dnaLength);
		if (Double.isNaN(res)) {
			System.err.println("dLin");
		}
		return res;
		// return aSum(d1, d2) / NORMAL_DISTRIBUTION_DEVIATION_SQUARED + 0
		// * cRev(d1, d2, dnaLength);
	}

	public static double dCon(int[] d1, int[] d2, int dnaLength, int l1, int l2) {
		// double w = 0;
		// for (int i = 0; i < d2.length; i++) {
		// w += Math
		// .log(Math.max(
		// 0,
		// Math.min(l1 - READ_LENGTH, l1 + l2 - d1[i] - d2[i])
		// - Math.max(0, l1 + READ_LENGTH - d1[i]
		// - d2[i]) + 1));
		// }
		double res = d1.length
				* Math.log(sqrt2Pi * NORMAL_DISTRIBUTION_DEVIATION)
				- (cLog(d1, d2, dnaLength) + d1.length * Math.log(dnaLength))
				- aSqSum(d1, d2) / (2 * NORMAL_DISTRIBUTION_DEVIATION_SQUARED);
		return res;
		// return d1.length
		// * Math.log(sqrt2Pi * NORMAL_DISTRIBUTION_DEVIATION)
		// - (0 * cLog(d1, d2, dnaLength) + d1.length
		// * Math.log(dnaLength)) - aSqSum(d1, d2)
		// / (2 * NORMAL_DISTRIBUTION_DEVIATION_SQUARED) + w * 0;
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
		double q = getProbabilityThatAtLeastOneMatepairMatches(d,
				contigLength1, contigLength2, dnaLength);
		// if (contigLength1 == 5438 && contigLength2 == 13401) {
		// double q2 = getProbabilityThatAtLeastOneMatepairMatchesStup(d,
		// contigLength1, contigLength2, dnaLength);
		// System.err.println(Math.abs(q - q2) + "\t" + 2 * Math.abs(q - q2)
		// / (q + q2) + "\t" + q + "\t" + q2);
		// }
		// double ret1 = (allReads - n) * Math.log(1 - q);
		// if (q > 1e-3) {
		// ret1 += -n * Math.log(q);
		// }
		double ret2 = (dSq * d + dLin) * d + dCon;
		// System.out.println(p + " " + Math.log(p));
		// return ret1 + ret2;
		// if (contigLength1 == 3012 && contigLength2 == 23959) {
		// System.err.println("d: " + d + "\t"
		// + (ret2 - n * Math.log(dnaLength * q)) + "\t" + dnaLength
		// * q);
		// }
		return ret2 - n * Math.log(dnaLength * q);
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

		int start = 2 * READ_LENGTH + Math.max(d, 0);
		int left = Math
				.max(d + READ_LENGTH + Math.min(contigLength1, contigLength2),
						start);
		int right = Math.max(
				d + READ_LENGTH + Math.max(contigLength1, contigLength2), left);
		int finish = Math.max(contigLength1 + contigLength2 + d, right);

		int b1 = -(2 * READ_LENGTH + d) + 1;
		int b2 = Math.min(contigLength1, contigLength2) - READ_LENGTH + 1;
		int b3 = contigLength1 + contigLength2 + d + 1;

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

	public static double getProbabilityThatAtLeastOneMatepairMatchesStup(int d,
			int contigLength1, int contigLength2, int dnaLength)
			throws MathException {

		int start = 2 * READ_LENGTH + Math.max(d, 0);
		int left = Math
				.max(d + READ_LENGTH + Math.min(contigLength1, contigLength2),
						start);
		int right = Math.max(
				d + READ_LENGTH + Math.max(contigLength1, contigLength2), left);
		int finish = Math.max(contigLength1 + contigLength2 + d, right);

		int b1 = -(2 * READ_LENGTH + d) + 1;
		int b2 = Math.min(contigLength1, contigLength2) - READ_LENGTH + 1;
		int b3 = contigLength1 + contigLength2 + d + 1;

		double ans = 0;

		for (int i = start; i < left; i++) {
			ans += getP(i) * (i + b1);
		}
		for (int i = left; i < right; i++) {
			ans += getP(i) * b2;
		}
		for (int i = right; i <= finish; i++) {
			ans += getP(i) * (b3 - i);
		}
		if (ans > b2) {
			System.err.println("error: " + ans + "\t" + b2);
		}
		ans /= dnaLength;
		return ans;
	}

	public static double getDerivativeThatAtLeastOneMatepairMatches(int d,
			int contigLength1, int contigLength2, int dnaLength)
			throws MathException {

		int start = 2 * READ_LENGTH + Math.max(d, 0);
		int left = Math
				.max(d + READ_LENGTH + Math.min(contigLength1, contigLength2),
						start);
		int right = Math.max(
				d + READ_LENGTH + Math.max(contigLength1, contigLength2), left);
		int finish = Math.max(contigLength1 + contigLength2 + d, right);

		int b1 = -(2 * READ_LENGTH + d) + 1;
		int db1 = -1;
		int b2 = Math.min(contigLength1, contigLength2) - READ_LENGTH + 1;
		int b3 = contigLength1 + contigLength2 + d + 1;
		int db3 = 1;
		double erfs = getERF(start);
		double erfl = getERF(left);
		double erfr = getERF(right);
		double erff = getERF(finish);

		double prob = 2.0 * NORMAL_DISTRIBUTION_DEVIATION / sqrt2Pi
				* (dExp(start) - dExp(left) - dExp(right) + dExp(finish));

		prob += db1 * erfs + (b1 + NORMAL_DISTRIBUTION_CENTER) * getP(start);
		prob += -db1 * erfl + (b2 - b1 - NORMAL_DISTRIBUTION_CENTER)
				* getP(left);
		prob += db3 * erfr + (b3 - NORMAL_DISTRIBUTION_CENTER - b2)
				* getP(right);
		prob += -db3 * erff + (-b3 + NORMAL_DISTRIBUTION_CENTER) * getP(finish);

		return prob / dnaLength / 2.0;
	}

	public static double getP(double x) {
		return Math.exp(-(NORMAL_DISTRIBUTION_CENTER - x)
				* (NORMAL_DISTRIBUTION_CENTER - x) / 2
				/ NORMAL_DISTRIBUTION_DEVIATION_SQUARED)
				/ NORMAL_DISTRIBUTION_DEVIATION / sqrt2Pi;
	}

}
