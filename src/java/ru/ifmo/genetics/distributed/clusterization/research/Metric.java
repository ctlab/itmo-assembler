package ru.ifmo.genetics.distributed.clusterization.research;


import ru.ifmo.genetics.tools.distribution.FastaReader;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Metric {


    void checkCorrelation(int readLength, int k) throws IOException {
        FastaReader fastaReader = new FastaReader(new File("/home/melnikov/work/svn/bio/genome-de-novo/data/coli.fasta"));

        String s = fastaReader.nextRead();
        System.err.println(s.length());
        for (int i = 0; i < 100000; i++) {
            int p1 = random.nextInt(s.length() - 10 * readLength);
//        int p2 = random.nextInt(s.length() - readLength);
            int p2 = p1 + random.nextInt(5 * readLength) + 1;
            String r1 = s.substring(p1, p1 + readLength);
            String r2 = s.substring(p2, p2 + readLength);
            System.err.println(Math.abs(p1 - p2) + "\t" + ro(r1, r2, k));
        }
    }

    Random random = new Random();

    String random(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append((char) ('a' + random.nextInt(4)));
        }
        return sb.toString();
    }


    public int ro(String a, String b, int k) {
        Set<String> fa = getKmers(a, k);
        Set<String> fb = getKmers(b, k);
        fa.retainAll(fb);
//        return a.length() + b.length() - 2 * fa.calculateSize();
        return Math.max(a.length(), b.length()) - fa.size();
    }

    private Set<String> getKmers(String a, int k) {
        Set<String> fa = new HashSet<String>();
        for (int i = 0; i < a.length() - k; i++) {
            fa.add(a.substring(i, i + k));
        }
        return fa;
    }

    public void run() {
        int it = 0;
        while (true) {
            if (++it % 1000000 == 0) {
                System.err.println(it);
            }
            int k = random.nextInt(10) + 5;
            String a = random(random.nextInt(30) + 20);
            String b = random(random.nextInt(30) + 20);
            String c = random(random.nextInt(30) + 20);

            int roab = ro(a, b, k);
            int robc = ro(b, c, k);
            int roac = ro(a, c, k);
//            System.err.println(a + " " + b + " " + c + " " + roab + " " + robc + " " + roac);
            if (roab + robc - roac < 0) {
                System.err.println(a);
                System.err.println(b);
                System.err.println(c);
                return;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Metric().checkCorrelation(72, 15);
    }
}
