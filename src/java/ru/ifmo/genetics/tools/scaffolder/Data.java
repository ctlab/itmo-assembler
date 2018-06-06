package ru.ifmo.genetics.tools.scaffolder;

import java.util.List;

public class Data {
	private Data() {

	}

	public static int dnaLength;
	public static int contigs;
	public static int contigSum;
	public static final double COVER_DEMAND = 0.90;

	public static Library[] libraries;

	public static double getMaxDeviation() {
		double max = 0;
		for (Library lib : libraries) {
			max = Math.max(max, lib.deviation);
		}
		return max;
	}

	public static double getMaxInsertSize() {
		return getMaxInsertSizeLibrary().insertSize;
	}

	public static Library getMaxInsertSizeLibrary() {
		Library ans = null;
		for (Library lib : libraries) {
			if (ans == null || ans.insertSize < lib.insertSize) {
				ans = lib;
			}
		}
		return ans;
	}

	public static int allReads() {
		int ans = 0;
		for (Library lib : libraries) {
			ans += lib.size();
		}
		return ans;
	}

	// public static final double NORMAL_DISTRIBUTION_CENTER = 2155.354;
	// public static final double NORMAL_DISTRIBUTION_DEVIATION = 291.6941;//
	// 0.1 *
	// // NORMAL_DISTRIBUTION_CENTER;
	// public static final double NORMAL_DISTRIBUTION_DEVIATION_SQUARED =
	// NORMAL_DISTRIBUTION_DEVIATION
	// * NORMAL_DISTRIBUTION_DEVIATION;
	// public static final int READ_LENGTH = 36;
	// public static final double COVER_DEMAND = 0.90;

	public static double getOneCover() {
		double sumLen = 0;
		for (Library lib : libraries) {
			sumLen += lib.size() * lib.readLength;
		}
		return 2.0 * sumLen / dnaLength;
	}

	public static double getMinInsertSize() {
		double ans = Double.POSITIVE_INFINITY;
		for (Library lib : libraries) {
			ans = Math.min(ans, lib.insertSize);
		}
		return ans;
	}
}
