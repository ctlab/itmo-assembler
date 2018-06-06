package ru.ifmo.genetics.tools.scaffolding.stupid;

import org.apache.commons.math.MathException;
import ru.ifmo.genetics.tools.scaffolding.ConnectionsGenerator;
import ru.ifmo.genetics.tools.scaffolding.MostProbableDistance;

import javax.sound.midi.SysexMessage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by IntelliJ IDEA. User: niyaznigmatul Date: 28.03.12 Time: 21:25 To
 * change this template use File | Settings | File Templates.
 */
public class Tester {

	static final Random rand = new Random(123123L);

	static int random(int l, int r) {
		return rand.nextInt(r - l + 1) + l;
	}

	public static void main(String[] args) throws FileNotFoundException,
			MathException {
		PrintWriter out = new PrintWriter("test.log");
		Locale.setDefault(Locale.US);
		if (args.length > 0) {
			for (String e : args) {
				int testNum = Integer.parseInt(e);
				test(out, testNum);
			}
		} else {
			int testNum = 0;
			while (++testNum <= 500) {
				if (!test(out, testNum)) {
					break;
				}
			}
		}
		out.close();
	}

	private static boolean test(PrintWriter out, int testNum)
			throws FileNotFoundException, MathException {
		File fin = new File(testNum + ".in");
		if (!fin.exists()) {
			return false;
		}
		File fargs = new File(testNum + ".args");
		System.err.println("Testing on test #" + testNum);
		out.println("Testing on test #" + testNum);
		Scanner inargs = new Scanner(fargs);
		// String[] arguments = inargs.nextLine().split(" ");
		int n = inargs.nextInt();
		int dnaLength = inargs.nextInt();
		int contigLength1 = inargs.nextInt();
		int contigLength2 = inargs.nextInt();
		int d = inargs.nextInt();
		int meanInsertLength = inargs.nextInt();
		double relativeDeviation = inargs.nextDouble();
		int readLength = inargs.nextInt();
		inargs.close();
		List<Integer> d1List = new ArrayList<Integer>();
		List<Integer> d2List = new ArrayList<Integer>();
		Scanner in = new Scanner(fin);
		while (in.hasNext()) {
			d1List.add(in.nextInt());
			d2List.add(in.nextInt());
		}
		int[] d1 = new int[d1List.size()];
		int[] d2 = new int[d1.length];
		for (int i = 0; i < d1.length; i++) {
			d1[i] = d1List.get(i);
			d2[i] = d2List.get(i);
		}
		long time = System.currentTimeMillis();
		int ans1 = MostProbableDistance.getMostProbableDistanceFast(
				contigLength1, contigLength2, d1, d2, dnaLength, n);
		long fastTime = System.currentTimeMillis() - time;
		time = System.currentTimeMillis();
		int ans2 = MostProbableDistance.getMostProbableDistanceSlow(
				contigLength1, contigLength2, d1, d2, dnaLength, n);
		long slowTime = System.currentTimeMillis() - time;
		int ans3 = MostProbableDistance.getMostProbableDistanceAverage(
				contigLength1, contigLength2, d1, d2, dnaLength, n);
		out.println("realAns = " + d + ", fastAns = " + ans1 + ", slowAns = "
				+ ans2 + ", average = " + ans3);
		out.println("fastTime = " + fastTime + ", slowTime = " + slowTime);
		out.println();
		System.err.println("realAns = " + d + ", fastAns = " + ans1
				+ ", slowAns = " + ans2 + ", average = " + ans3);
		System.err.println("fastTime = " + fastTime + ", slowTime = "
				+ slowTime);
		return true;
	}

	public static PrintWriter out;

	static void test2() throws FileNotFoundException, MathException {
		out = new PrintWriter("test.log");
		for (int test = 0; test < 10; test++) {
			final int N = random(100000000, 250000000);
			final int dnaLength = random(1000000000, 2000000000);
			final int contigLength1 = random(1000, 2000);
			final int contigLength2 = random(1000, 2000);
			final int D = random(2000, 4000);
			final int meanInsertLength = 3000;
			final double relativeDeviation = 0.08;
			final int readLength = 36;
			ConnectionsGenerator.main(new String[] { N + "", dnaLength + "",
					contigLength1 + "", contigLength2 + "", D + "",
					meanInsertLength + "", relativeDeviation + "",
					readLength + "" });
			Scanner sc = new Scanner(new File("input.txt"));
			List<Integer> listd1 = new ArrayList<Integer>();
			List<Integer> listd2 = new ArrayList<Integer>();
			// List<Integer> listInsertLength = new ArrayList<Integer>();
			while (sc.hasNext()) {
				listd1.add(sc.nextInt());
				listd2.add(sc.nextInt());
				// listInsertLength.add(sc.nextInt());
			}
			sc.close();
			int[] d1 = new int[listd1.size()];
			int[] d2 = new int[listd2.size()];
			// int[] matePairLength = new int[listInsertLength.size()];
			for (int i = 0; i < d1.length; i++) {
				d1[i] = listd1.get(i);
				d2[i] = listd2.get(i);
				// matePairLength[i] = listInsertLength.get(i);
			}
			if (d1.length == 0) {
				continue;
			}
			// Solver.drawGraph(contigLength1, contigLength2, d1, d2, dnaLength,
			// N);
			out.println(d1.length + ":");
			out.println(D);
			// {long time = System.currentTimeMillis();
			// int slow = MostProbableDistance.getMostProbableDistanceSlow(
			// contigLength1, contigLength2, d1, d2, dnaLength, N);
			// long timeSlow = (System.currentTimeMillis() - time);
			// time = System.currentTimeMillis();
			// int fast = MostProbableDistance.getMostProbableDistanceFast(
			// contigLength1, contigLength2, d1, d2, dnaLength, N);
			// out.println(slow + " " + fast);
			// out.println("timeSlow = " + timeSlow + ", timeFast = "
			// + (System.currentTimeMillis() - time));}
			{
				long time = System.currentTimeMillis();
				int slow = MostProbableDistance.getMostProbableDistanceFast(
						contigLength1, contigLength2, d1, d2, dnaLength, N);
				long timeSlow = (System.currentTimeMillis() - time);
				time = System.currentTimeMillis();
				int fast = MostProbableDistance
						.getMostProbableDistanceFastVeryFast(contigLength1,
								contigLength2, d1, d2, dnaLength, N);
				out.println(slow + " " + fast);
				out.println("timeSlow = " + timeSlow + ", timeFast = "
						+ (System.currentTimeMillis() - time));
			}
			double sum = 0;
			for (int i = 0; i < d1.length; i++) {
				sum += meanInsertLength - d1[i] - d2[i] - 2 * readLength;
			}
			out.println((long) Math.round(sum / d1.length));
			out.flush();
		}
		out.close();
	}
}
