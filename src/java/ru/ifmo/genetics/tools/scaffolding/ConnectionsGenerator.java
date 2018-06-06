package ru.ifmo.genetics.tools.scaffolding;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.apache.commons.math.MathException;
import org.apache.commons.math.random.MersenneTwister;
import org.apache.commons.math.random.RandomGenerator;
import org.apache.commons.math.special.Erf;

public class ConnectionsGenerator {
	private final int N;
	private final int L;
	private final int l1, l2;
	private final int d;
	private final double meanInsertLength, insertLengthDeviation;
	private final int readLength;

	private final RandomGenerator random = new MersenneTwister();

	// private final Random random = new Random();

	public ConnectionsGenerator(int N, int L, int l1, int l2, int d,
			double meanInsertLength, double insertLengthRelativeDeviation,
			int readLength) {
		this.N = N;
		this.L = L;
		this.l1 = l1;
		this.l2 = l2;
		this.d = d;
		this.meanInsertLength = meanInsertLength;
		this.insertLengthDeviation = meanInsertLength
				* insertLengthRelativeDeviation;
		this.readLength = readLength;
	}

	public void run() {
		try {
			double p0 = 0;
			for (int d1 = readLength; d1 <= l1; ++d1) {
				p0 += pNorm(d1 + d + readLength, d1 + d + l2);
			}
			p0 /= L;
			/*
			 * // more fine for (int d1 = readLength; d1 <= l1; ++d1) { for (int
			 * d2 = readLength; d2 <= l2; ++d2) { int l = d1 + d2 + d; p0 +=
			 * pNorm(l) / (L - l); } }
			 */
			System.err.println("Expecting " + (N * p0) + " connections");
		} catch (MathException e) {
			System.err.println("Failed to estimate number of connections");
		}
		int offset1 = (L - l1 - l2 - d) / 2;
		int offset2 = offset1 + l1 + d;
		int n = 0;

		PrintWriter out = null;
		try {
			out = new PrintWriter("input.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < N; ++i) {
			int insertLength = nextInsertLength();
			// System.err.println(insertLength);
			int pos1 = random.nextInt(L - insertLength + 1);
			int pos2 = pos1 + insertLength;
			// System.err.print(pos1 + " " + pos2 + " - ");
			if (!((offset1 <= pos1) && (pos1 <= offset1 + l1 - readLength))) {
				// first contig missed
				// System.err.println("dropped");
				continue;
			}

			if (!((offset2 + readLength <= pos2) && (pos2 <= offset2 + l2))) {
				// second contig missed
				// System.err.println("dropped");
				continue;
			}
			// System.err.println("ok");

			++n;
			System.out.println((offset1 + l1 - pos1 - readLength) + " "
					+ (pos2 - offset2 - readLength));
			out.println((offset1 + l1 - pos1 - readLength) + " "
					+ (pos2 - offset2 - readLength));
		}
		out.close();
		System.err.println(n + " connections generated");
		/*
		 * try { double s = 0; for (int i = 2000; i <= 4000; ++i) { s +=
		 * pNorm(i); System.err.println(pNorm(i)); } System.err.println(s); }
		 * catch (Exception e) { }
		 */
	}

	private int nextInsertLength() {
		// System.err.println(random.nextGaussian());
		return (int) (Math.round(meanInsertLength + random.nextGaussian()
				* insertLengthDeviation));
	}

	private double gaussDistributionFunction(double x) throws MathException {
		return (1 + Erf.erf((x - meanInsertLength) / insertLengthDeviation
				/ Math.sqrt(2))) / 2;
	}

	private double pNorm(int minLength, int maxLength) throws MathException {
		return gaussDistributionFunction(maxLength + 0.5)
				- gaussDistributionFunction(minLength - 0.5);
	}

	private double pNorm(int length) throws MathException {
		return pNorm(length, length);
	}

	public static void main(String[] args) {
		if (args.length != 8) {
			System.err
					.println("usage: generate_connections <N> <L> <l1> <l2> <d> <mean-insert-length> <relative-deviation> <read-length>");
			System.exit(1);
		}

		new ConnectionsGenerator(Integer.parseInt(args[0]),
				Integer.parseInt(args[1]), Integer.parseInt(args[2]),
				Integer.parseInt(args[3]), Integer.parseInt(args[4]),
				Double.parseDouble(args[5]), Double.parseDouble(args[6]),
				Integer.parseInt(args[7])).run();

	}

}
