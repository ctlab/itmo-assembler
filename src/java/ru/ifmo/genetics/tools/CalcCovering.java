package ru.ifmo.genetics.tools;

import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.statistics.Timer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CalcCovering {

    public static final long MAGIC = (long) 1e9 + 9;

    private static long[] pow = new long[601];

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: CalcCovering <kmers file> <genome file> [<output file>]");
            System.exit(666);
        }

        pow[0] = 1;
        for (int i = 1; i < pow.length; ++i) {
            pow[i] = pow[i - 1] * MAGIC;
        }

        BufferedReader gr = new BufferedReader(new FileReader(args[1]));
        String genome = gr.readLine();
        gr.close();
        boolean[] ar = new boolean[genome.length()];

        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        int badStrings = 0;
        int k = 0;

        Collection<Long> hs = new HashSet<Long>();

        Timer t = new Timer();
        t.start();

        while (true) {
            String s = br.readLine();
            if (s == null) {
                break;
            }
            long h0 = hash(s);
            hs.add(h0);
            if ((k & 4095) == 0) {
                System.err.print("\r" + k);
            }
            ++k;
        }
        br.close();
        System.err.println();
        System.err.println("read " + hs.size() + " sequences");

        System.err.println("reading took: " + t.getTime());

        t.start();

        Set<Long> present = new HashSet<Long>();

        for (int i = 0; i < genome.length(); i++) {
            long h = 0;
            int ml = 0;
            for (int l = 400; (l <= 600) && (i + l <= genome.length()); l++) {
                h = (l == 400) ? hash(genome, i, i + l) : rehash(genome, i, i + l - 1, h, 0, 1);
                if (hs.contains(h)) {
                    ml = l;
                    present.add(h);
                }
            }
            for (int j = 0; j < ml; ++j) {
                ar[i + j] = true;
            }
            if ((i & 4095) == 0) {
                System.err.print("\r" + i);
            }
        }
        System.err.println();
        System.err.println("checking took: " + t.getTime());
        System.err.println((hs.size() - present.size()) + " sequences aren't present in genome");

        int covered = 0;
        for (int i = 0; i < genome.length(); ++i) {
            if (ar[i]) {
                ++covered;
            }
        }
        System.err.println("covered: " + covered + "/" + genome.length() + "  " +
                100 * ((double)covered) / genome.length() + "%");
        if (args.length >= 3) {
            PrintWriter pw = new PrintWriter(args[2]);
            for (int i = 0; i < ar.length; ++i) {
                pw.print(ar[i] ? '1' : '0');
            }
            pw.close();
        }

    }

    public static long hash(String s) {
        return hash(s, 0, s.length());
    }

    public static long hash(String s, int b, int e) {
        long res = 0;
        for (int i = b; i < e; i++) {
            res += pow[e - i - 1] * DnaTools.fromChar(s.charAt(i));
        }
        return res;
    }

    public static long rehash(String s, int b, int e, long h, int bs, int es) {
        for (int i = 0; i < bs; i++) {
            h -= pow[e - b - i - 1] * DnaTools.fromChar(s.charAt(b + i));
        }
        for (int i = 0; i < es; i++) {
            h *= MAGIC;
            h += DnaTools.fromChar(s.charAt(e + i));
        }
        return h;
    }

    /*

    public static long div(long a, long b) {
        return (a * inv(b)) % MAGIC;
    }

    public static long inv(long a) {
        long b = MAGIC;
        long c11 = 1, c12 = 0, c21 = 0, c22 = 1;
        while (a > 0) {
            long d = b / a;
            b -= d * a;
            c21 -= d * c11;
            c22 -= d * c12;
            long t;
            t = a; a = b; b = t;
            t = c11; c11 = c21; c21 = t;
            t = c12; c12 = c22; c22 = t;
        }
        return c21;
    }

    public static long pow(long a, long b) {
        long res = 1;
        while (b > 0) {
            if ((b & 1) == 0) {
                --b;
                res = (res * a) % MAGIC;
                continue;
            }
            a = (a * a) % MAGIC;
            b >>= 1;
        }
        return res;
    }
    */
}

