package ru.ifmo.genetics.statistics;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Scanner;

public class GenomeCoverageCalculator {

    final static long STEP = 1000000;

    public static void main(String[] args) throws IOException {
        int len = Integer.parseInt(args[0]);
        int[] ar = new int[len];
        Scanner sc = new Scanner(new BufferedInputStream(System.in));
        long processed = 0, last = 0;
        while (sc.hasNext()) {
            char c = sc.next().charAt(0);
            int pos = sc.nextInt();
            String read = sc.next();
            int clen = read.length();
            if (c == '+') {
                ar[pos] = Math.max(ar[pos], pos + clen);
            } else {
                ar[len - pos - clen] = Math.max(ar[len - pos - clen], len - pos);
            }

            ++processed;
            if (processed - last >= STEP) {
                last = processed;
                System.err.print("\rprocessed lines: " + processed);
            }
        }
        System.err.println("\rprocessed " + processed + "                         ");
        int covered = 0;
        int right = -1;
        for (int i = 0; i < ar.length; ++i) {
            right = Math.max(right, ar[i]);
            if (i < right) {
                ++covered;
            }
        }
        System.out.println("covered: " + covered + " / " + ar.length + " (" + 100. * covered / ar.length + "%)");
    }

}
