package ru.ifmo.genetics.tools.distribution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Prints to file read length distribution of reads from FASTA files in current folder
 * @author fedor.tsarev
 *
 */
public class DistributionCalculator {
	private static final int READS_LOGGING_STEP = 10000;

	public static void main(String[] args) throws IOException {
		
		if (args.length == 0) {
			System.err.println("specify FASTA filenames as command-line parameters");
			System.exit(1);
		}

		// TODO: change to good file name

		String outputFilename = "read-length-distribution-" + System.currentTimeMillis() + ".txt";
		PrintWriter out = new PrintWriter(new FileWriter(new File(outputFilename)));
		
		// TODO: print service information

		HashMap<Integer, Integer> distribution = new HashMap<Integer, Integer>();
	    
		File workingDirectory = new File(".");

		/*
		File[] fastaFiles = workingDirectory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				return name.endsWith(".fasta");
			}
		});
		*/

		System.out.println("Calculating distribution of filled reads lengths (" + args.length + " files)" );
		System.out.println();
		
		System.out.println("Files to process: " + args.length);
		
		int filesProcessed = 0;
		
		int totalReadsCount = 0;
		for (String filename : args) {
			System.out.println();
			System.out.println("Processing file: " + filename);
			FastaReader fastaReader = new FastaReader(new File(filename));
			String currentRead = fastaReader.nextRead();
			while (currentRead != null) {
				int len = currentRead.length();
				Integer x = distribution.get(len);
				if (x == null) {
					x = 0;
				}
				distribution.put(len, x + 1);
				totalReadsCount++;
				if (totalReadsCount % READS_LOGGING_STEP == 0) {
					System.out.println("   " + totalReadsCount + " reads total processed");
				}
				currentRead = fastaReader.nextRead();
			}
			filesProcessed++;
			System.out.println("Finished processing file: " + filename + ", " + filesProcessed + " out of " + args.length);
		}
		
		System.out.println();
		System.out.println("Total number of reads: " + totalReadsCount);
		
		ArrayList<int[]> histogram = new ArrayList<int[]>();
		for (Integer len : distribution.keySet()) {
			histogram.add(new int[] {len, distribution.get(len)});
		}
		Collections.sort(histogram, new Comparator<int[]>() {
			@Override
			public int compare(int[] x, int[] y) {
				return x[0] - y[0];
			}
		});
		int prevLength = 0;
		for (int[] a : histogram) {
			if (a[0] != prevLength + 1) {
				for (int i = prevLength + 1; i < a[0]; i++) {
					out.println(i + " 0 0");
				}
			}
			prevLength = a[0];
			out.println(a[0] + " " + a[1] + " " + (1.0 * a[1]) / totalReadsCount);
		}
		
		out.close();
	}
}
