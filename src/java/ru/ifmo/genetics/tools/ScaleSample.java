package ru.ifmo.genetics.tools;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class ScaleSample {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: java ScaleSample <input file> <read size> <output file>");
            System.exit(1);
        }
        Scanner in = new Scanner(new FileReader(args[0]));
        int len = Integer.parseInt(args[1]);
        PrintWriter out = new PrintWriter(args[2]);

        int n = in.nextInt();
        double[][] ar = new double[n][41];
        double[] s = new double[41];
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < 41; ++j) {
                ar[i][j] = in.nextDouble();
                s[j] += ar[i][j];
            }
        }

        double[][] nr = new double[len][41];
        double[] sum = new double[len];
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < 41; ++j) {
                int k = i * len / n;
                nr[k][j] += ar[i][j];
                sum[k] += ar[k][j];
            }
        }

        out.println(len);
        for (int i = 0; i < len; ++i) {
            for (int j = 0; j < 41; ++j) {
                out.print(nr[i][j] / sum[i] + " ");
            }
            out.println();
        }
        out.close();
    }
}
