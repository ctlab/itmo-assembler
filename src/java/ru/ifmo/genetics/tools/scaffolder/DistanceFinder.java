package ru.ifmo.genetics.tools.scaffolder;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.math.MathException;

public class DistanceFinder {

	// public static boolean newFix = false;

	public static int getMostProbableDistanceAverage(int contigLength1,
			int contigLength2, MatePair[] pairs) {
		double s = 0;
		for (MatePair mp : pairs) {
			s += mp.lib.insertSize - mp.getD1() - mp.getD2() - 2
					* mp.lib.readLength;
		}
		s /= pairs.length;
		return (int) Math.round(s);
	}

	public static int setMostProbableDistance(Edge e) throws MathException {
		boolean rev1 = false;
		boolean rev2 = false;
		double best = Double.NEGATIVE_INFINITY;
		int ans = Integer.MIN_VALUE;
		boolean br1 = false;
		boolean br2 = false;
		for (int iOr1 = 0; iOr1 < 1; iOr1++) {
			for (int iOr2 = 0; iOr2 < 1; iOr2++) {
				int dist = getMostProbableDistance(e.v1.info.len,
						e.v2.info.len, e.pairs, e.cnt);
				double likelihood = getProbabilityThatAllMatepairsMatch(dist,
						e.v1.info.len, e.v2.info.len, e.pairs.length,
						dSq(e.pairs), dLin(e.pairs), dCon(e.pairs), e.cnt);
				if (likelihood > best) {
					if (rev1 || rev2) {
						System.err.println(dist + "\t" + likelihood + "\t"
								+ e.v1.info.len + "\t" + e.v2.info.len + "\t"
								+ e.realDist());
						System.err.println(ans + "\t" + best + "\t" + br1
								+ "\t" + br2);
					}
					best = likelihood;
					ans = dist;
					br1 = rev1;
					br2 = rev2;
				}
				rev2 = !rev2;
			}
			rev1 = !rev1;
		}
		e.setLen(ans);
		// if (e.cnt[0] == 18) {
		// System.err.println("length: " + ans + "\t" + e.getAvLen() + "\t"
		// + e.realDist());
		// int[] d1 = e.getD1();
		// int[] d2 = e.getD2();
		// double dSq = dSq(e.pairs);
		// double dLin = dLin(e.pairs);
		// double dCon = dCon(e.pairs);
		// double dSq2 = dSq(d1, d2);
		// double dLin2 = dLin(d1, d2);
		// double dCon2 = dCon(d1, d2);
		// System.err.println(Math.abs(dSq - dSq2) + "\t"
		// + Math.abs(dLin - dLin2) + "\t" + Math.abs(dCon - dCon2));
		// System.err.println(Arrays.toString(d1));
		// System.err.println(Arrays.toString(d2));
		// for (int i = 0; i < d1.length; i++) {
		// d1[i] += d2[i];
		// }
		// System.err.println(Arrays.toString(d1));
		// }
		return ans;
	}

	public static int getMostProbableDistance(final int contigLength1,
			final int contigLength2, final MatePair[] pairs, final int[] cnt)
			throws MathException {

		double dSq = dSq(pairs);
		double dLin = dLin(pairs);
		double dCon = dCon(pairs);

		return getMostProbableDistance(contigLength1, contigLength2,
				pairs.length, dSq, dLin, dCon, cnt);
	}

	private static final double sqrt2 = Math.sqrt(2);
	public static final double sqrt2Pi = Math.sqrt(2 * Math.PI);

	private static int getMostProbableDistance(int contigLength1,
			int contigLength2, int n, double dSq, double dLin, double dCon,
			int[] cnt) throws MathException {
		// int left = (int) (-2 * NORMAL_DISTRIBUTION_CENTER);
		// int right = (int) (10 * NORMAL_DISTRIBUTION_CENTER);
		double left = Integer.MAX_VALUE / 2;
		double right = Integer.MIN_VALUE / 2;
		Library maxLib = Data.getMaxInsertSizeLibrary();
		for (double i = -2 * Data.getMaxDeviation(); i <= 4 * maxLib.insertSize; i += Data
				.getMaxDeviation()) {
			double q = DistanceFinder
					.getProbabilityThatAtLeastOneMatepairMatches(i, maxLib,
							contigLength1, contigLength2);
			if (!Double.isInfinite(Math.log(Data.dnaLength * q))) {
				left = Math.min(left, i);
				right = Math.max(right, i);
			}
		}
		while (right - left > 2) {
			double ml = (2 * left + right) / 3;
			double mr = (left + 2 * right) / 3;
			double fl = getProbabilityThatAllMatepairsMatch(ml, contigLength1,
					contigLength2, n, dSq, dLin, dCon, cnt);
			double fr = getProbabilityThatAllMatepairsMatch(mr, contigLength1,
					contigLength2, n, dSq, dLin, dCon, cnt);
			if (fl < fr) {
				left = ml;
			} else {
				right = mr;
			}
		}
		double f = getProbabilityThatAllMatepairsMatch(left, contigLength1,
				contigLength2, n, dSq, dLin, dCon, cnt);
		int ans = (int) Math.round(Math.floor(left));
		for (int i = (int) (left - 2); i <= right + 1; i++) {
			double fn = getProbabilityThatAllMatepairsMatch(i, contigLength1,
					contigLength2, n, dSq, dLin, dCon, cnt);
			if (fn > f + 1e-8) {
				f = fn;
				ans = i;
			}
		}
		if (cnt[0] == 18) {
			double[] v = new double[6000];
			for (int d = 0; d < v.length; d++) {
				v[d] = getProbabilityThatAllMatepairsMatch(d - 1500,
						contigLength1, contigLength2, n, dSq, dLin, dCon, cnt);
			}
			if (nonconvex == null) {
				try {
					nonconvex = new PrintWriter("nonconvex");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			nonconvex.println("#####");
			for (int i = 0; i < v.length; i++) {
				nonconvex.print(v[i] + "\t");
			}
			nonconvex.println();
			nonconvex.flush();
		}
		return ans;
	}

	static double getProbabilityThatAtLeastOneMatepairMatches(double d,
			Library lib, int contigLength1, int contigLength2)
			throws MathException {
		return getProbabilityThatAtLeastOneMatepairMatches(d, lib.readLength,
				lib.insertSize, lib.deviation, contigLength1, contigLength2);
	}

	static PrintWriter nonconvex;

	private static double exp(double x, double insertSize, double deviation) {
		return Math.exp(-(x - insertSize) * (x - insertSize) / 2 / deviation
				/ deviation);
	}

	private static double exp(double x, Library lib) {
		return exp(x, lib.insertSize, lib.deviation);
	}

	// private static double dExp(int x) {
	// return -exp(x) * (x - Data.NORMAL_DISTRIBUTION_CENTER)
	// / Data.NORMAL_DISTRIBUTION_DEVIATION_SQUARED;
	// }

	private static double getERF(double x, double insertSize, double deviation)
			throws MathException {
		return org.apache.commons.math.special.Erf.erf((insertSize - x) / sqrt2
				/ deviation);
	}

	public static double dSq(MatePair[] pairs) {
		double res = 0;
		for (MatePair p : pairs) {
			res -= 1.0 / (2 * p.lib.deviation * p.lib.deviation);
		}
		res += cSqRev(pairs) / 2;
		if (Double.isNaN(res)) {
			System.err.println("dSq");
		}
		return res;
	}

	public static double dLin(MatePair[] pairs) {
		double res = aSum(pairs) + cRev(pairs);
		if (Double.isNaN(res)) {
			System.err.println("dLin");
		}
		return res;
	}

	public static double dCon(MatePair[] pairs) {
		double res = 0;
		for (MatePair p : pairs) {
			res += Math.log(sqrt2Pi * p.lib.deviation);
		}
		res += -(cLog(pairs) + pairs.length * Math.log(Data.dnaLength))
				- aSqSum(pairs) / 2;
		return res;
	}

	static final double NORMAL_DISTRIBUTION_DEVIATION_SQUARED = 300 * 300;
	static final double NORMAL_DISTRIBUTION_DEVIATION = 300;
	static final double NORMAL_DISTRIBUTION_CENTER = 3000;
	static final double READ_LENGTH = 36;

	public static double dSq(int[] d1, int[] d2) {
		double res = -d1.length / (2 * NORMAL_DISTRIBUTION_DEVIATION_SQUARED)
				+ cSqRev(d1, d2) / 2;
		if (Double.isNaN(res)) {
			System.err.println("dSq");
		}
		return res;
		// return -d1.length / (2 * NORMAL_DISTRIBUTION_DEVIATION_SQUARED)+ 0
		// * cSqRev(d1, d2, dnaLength) / 2;
	}

	public static double dLin(int[] d1, int[] d2) {
		double res = aSum(d1, d2) / NORMAL_DISTRIBUTION_DEVIATION_SQUARED
				+ cRev(d1, d2);
		if (Double.isNaN(res)) {
			System.err.println("dLin");
		}
		return res;
		// return aSum(d1, d2) / NORMAL_DISTRIBUTION_DEVIATION_SQUARED + 0
		// * cRev(d1, d2, dnaLength);
	}

	public static double dCon(int[] d1, int[] d2) {
		double res = d1.length
				* Math.log(sqrt2Pi * NORMAL_DISTRIBUTION_DEVIATION)
				- (cLog(d1, d2) + d1.length * Math.log(Data.dnaLength))
				- aSqSum(d1, d2) / (2 * NORMAL_DISTRIBUTION_DEVIATION_SQUARED);
		return res;
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

	private static double cRev(int[] d1, int[] d2) {
		double cRev = 0;
		for (int i = 0; i < d1.length; i++) {
			double c = Data.dnaLength - d1[i] - d2[i] - 2 * READ_LENGTH + 1;
			cRev += 1.0 / c;
		}
		return cRev;
	}

	private static double cSqRev(int[] d1, int[] d2) {
		double cSqRev = 0;
		for (int i = 0; i < d1.length; i++) {
			double c = Data.dnaLength - d1[i] - d2[i] - 2 * READ_LENGTH + 1;
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

	private static double cLog(int[] d1, int[] d2) {
		double ans = 0;
		for (int i = 0; i < d1.length; i++) {
			ans += Math.log(Data.dnaLength - d1[i] - d2[i] - 2 * READ_LENGTH
					+ 1);
		}
		return ans;
	}

	// public static double getDerivativeThatAllMatepairsMatch(int d,
	// int contigLength1, int contigLength2, int n, double dSq, double dLin)
	// throws MathException {
	// double p = getProbabilityThatAtLeastOneMatepairMatches(d,
	// contigLength1, contigLength2);
	// double dp = getDerivativeThatAtLeastOneMatepairMatches(d,
	// contigLength1, contigLength2);
	// double ret1 = -(Data.allReads - n) * dp / (1 - p);
	// // ret1 += -n * dp / d;
	// double ret2 = 2 * dSq * d + dLin;
	// return ret1 + ret2;
	// }

	public static double getProbabilityThatAllMatepairsMatch(double d, Edge e)
			throws MathException {
		return getProbabilityThatAllMatepairsMatch(d, e.v1.info.len,
				e.v2.info.len, e.pairs.length, e.dSq, e.dLin, e.dCon, e.cnt);
	}

	public static double getProbabilityThatAllMatepairsMatch(double d,
			int contigLength1, int contigLength2, int n, double dSq,
			double dLin, double dCon, int[] cnt) throws MathException {
		double ret1 = 0;
		for (int i = 0; i < cnt.length; i++) {
			Library lib = Data.libraries[i];
			double q = getProbabilityThatAtLeastOneMatepairMatches(d, lib,
					contigLength1, contigLength2);
			ret1 += (lib.size() - cnt[i]) * Math.log(1 - q);
		}
		// if (contigLength1 == 5438 && contigLength2 == 13401) {
		// double q2 = getProbabilityThatAtLeastOneMatepairMatchesStup(d,
		// contigLength1, contigLength2, dnaLength);
		// System.err.println(Math.abs(q - q2) + "\t" + 2 * Math.abs(q - q2)
		// / (q + q2) + "\t" + q + "\t" + q2);
		// }
		// if (q > 1e-3) {
		// ret1 += -n * Math.log(q);
		// }
		double ret2 = (dSq * d + dLin) * d + dCon;
		// System.out.println(p + " " + Math.log(p));

		double ret3 = true ? 0 : getDistanceProbability(d);

		return ret1 + ret2 + ret3;
		// if (contigLength1 == 3012 && contigLength2 == 23959) {
		// System.err.println("d: " + d + "\t"
		// + (ret2 - n * Math.log(dnaLength * q)) + "\t" + dnaLength
		// * q);
		// }
		// return ret2 - n * Math.log(dnaLength * q);
	}

	private static double getDistanceProbability(double d) {
		return Math.log(1.0 - 1.0 * d / (Data.dnaLength - Data.contigSum))
				* (Data.contigs - 1);
	}

	private static double aSum(MatePair[] pairs) {
		double aSum = 0;
		for (MatePair p : pairs) {
			double a = p.lib.insertSize
					- (p.getD1() + p.getD2() + 2 * p.lib.readLength);
			aSum += a / p.lib.deviation / p.lib.deviation;
		}
		return aSum;
	}

	private static double cRev(MatePair[] pairs) {
		double cRev = 0;
		for (MatePair p : pairs) {
			double c = Data.dnaLength - p.getD1() - p.getD2() - 2
					* p.lib.readLength + 1;
			cRev += 1.0 / c;
		}
		return cRev;
	}

	private static double cSqRev(MatePair[] pairs) {
		double cSqRev = 0;
		for (MatePair p : pairs) {
			double c = Data.dnaLength - p.getD1() - p.getD2() - 2
					* p.lib.readLength + 1;
			cSqRev += 1.0 / c / c;
		}
		return cSqRev;
	}

	private static double aSqSum(MatePair[] pairs) {
		double ans = 0;
		for (MatePair p : pairs) {
			ans += (p.lib.insertSize - p.getD1() - p.getD2() - 2 * p.lib.readLength)
					* (p.lib.insertSize - p.getD1() - p.getD2() - 2 * p.lib.readLength)
					/ p.lib.deviation / p.lib.deviation;
		}
		return ans;
	}

	private static double cLog(MatePair[] pairs) {
		double ans = 0;
		for (MatePair p : pairs) {
			ans += Math.log(Data.dnaLength - p.getD1() - p.getD2() - 2
					* p.lib.readLength + 1);
		}
		return ans;
	}

	public static double getProbabilityThatAtLeastOneMatepairMatches(double d,
			double readLength, double insertSize, double deviation,
			int contigLength1, int contigLength2) throws MathException {

		double start = 2 * readLength + Math.max(d, 0);
		double left = Math.max(
				d + readLength + Math.min(contigLength1, contigLength2), start);
		double right = Math.max(
				d + readLength + Math.max(contigLength1, contigLength2), left);
		double finish = Math.max(contigLength1 + contigLength2 + d, right);

		double b1 = -(2 * readLength + d) + 1;
		double b2 = Math.min(contigLength1, contigLength2) - readLength + 1;
		double b3 = contigLength1 + contigLength2 + d + 1;

		double erfs = getERF(start, insertSize, deviation);
		double erfl = getERF(left, insertSize, deviation);
		double erfr = getERF(right, insertSize, deviation);
		double erff = getERF(finish, insertSize, deviation);

		double prob = 2.0
				* deviation
				/ sqrt2Pi
				* (exp(start, insertSize, deviation)
						- exp(left, insertSize, deviation)
						- exp(right, insertSize, deviation) + exp(finish,
							insertSize, deviation));

		prob += erfs * (b1 + insertSize);
		prob += erfl * (b2 - b1 - insertSize);
		prob += erfr * (b3 - insertSize - b2);
		prob += erff * (-b3 + insertSize);

		return prob / Data.dnaLength / 2.0;
	}

	// public static double getProbabilityThatAtLeastOneMatepairMatchesStup(int
	// d,
	// int contigLength1, int contigLength2) throws MathException {
	//
	// int start = 2 * Data.READ_LENGTH + Math.max(d, 0);
	// int left = Math.max(
	// d + Data.READ_LENGTH + Math.min(contigLength1, contigLength2),
	// start);
	// int right = Math.max(
	// d + Data.READ_LENGTH + Math.max(contigLength1, contigLength2),
	// left);
	// int finish = Math.max(contigLength1 + contigLength2 + d, right);
	//
	// int b1 = -(2 * Data.READ_LENGTH + d) + 1;
	// int b2 = Math.min(contigLength1, contigLength2) - Data.READ_LENGTH + 1;
	// int b3 = contigLength1 + contigLength2 + d + 1;
	//
	// double ans = 0;
	//
	// for (int i = start; i < left; i++) {
	// ans += getP(i) * (i + b1);
	// }
	// for (int i = left; i < right; i++) {
	// ans += getP(i) * b2;
	// }
	// for (int i = right; i <= finish; i++) {
	// ans += getP(i) * (b3 - i);
	// }
	// // if (ans > b2) {
	// // System.err.println("error: " + ans + "\t" + b2);
	// // }
	// ans /= Data.dnaLength;
	// return ans;
	// }

	// public static double getDerivativeThatAtLeastOneMatepairMatches(int d,
	// int contigLength1, int contigLength2) throws MathException {
	//
	// int start = 2 * Data.READ_LENGTH + Math.max(d, 0);
	// int left = Math.max(
	// d + Data.READ_LENGTH + Math.min(contigLength1, contigLength2),
	// start);
	// int right = Math.max(
	// d + Data.READ_LENGTH + Math.max(contigLength1, contigLength2),
	// left);
	// int finish = Math.max(contigLength1 + contigLength2 + d, right);
	//
	// int b1 = -(2 * Data.READ_LENGTH + d) + 1;
	// int db1 = -1;
	// int b2 = Math.min(contigLength1, contigLength2) - Data.READ_LENGTH + 1;
	// int b3 = contigLength1 + contigLength2 + d + 1;
	// int db3 = 1;
	// double erfs = getERF(start);
	// double erfl = getERF(left);
	// double erfr = getERF(right);
	// double erff = getERF(finish);
	//
	// double prob = 2.0 * Data.NORMAL_DISTRIBUTION_DEVIATION / sqrt2Pi
	// * (dExp(start) - dExp(left) - dExp(right) + dExp(finish));
	//
	// prob += db1 * erfs + (b1 + Data.NORMAL_DISTRIBUTION_CENTER)
	// * getP(start);
	// prob += -db1 * erfl + (b2 - b1 - Data.NORMAL_DISTRIBUTION_CENTER)
	// * getP(left);
	// prob += db3 * erfr + (b3 - Data.NORMAL_DISTRIBUTION_CENTER - b2)
	// * getP(right);
	// prob += -db3 * erff + (-b3 + Data.NORMAL_DISTRIBUTION_CENTER)
	// * getP(finish);
	//
	// return prob / Data.dnaLength / 2.0;
	// }

	public static double getP(double x, double insertSize, double deviation) {
		return Math.exp(-(insertSize - x) * (insertSize - x) / 2 / deviation
				/ deviation)
				/ deviation / sqrt2Pi;
	}

	public static double getProbabilityThatMatchesInsideContig(
			Collection<Contig> values, double insertSize, double deviation) {
		double p = 0;
		for (Contig c : values) {
			for (int i = 1; i <= c.len; i++) {
				p += getP(i, insertSize, deviation) * (c.len - i + 1)
						/ Data.dnaLength;
			}
		}
		return p;
	}

	public static double getProbabilityThatAtLeastOneMatepairMatches(double d,
			int contigLength1, int contigLength2) throws MathException {
		int sum = 0;
		for (Library lib : Data.libraries) {
			sum += lib.size();
		}
		double p = 0;
		for (Library lib : Data.libraries) {
			double q = getProbabilityThatAtLeastOneMatepairMatches(d, lib,
					contigLength1, contigLength2);
			p += (q * lib.size()) / sum;
		}
		return p;
	}

}
