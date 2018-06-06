package ru.ifmo.genetics.statistics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.StringTokenizer;

public class FixesStatistics {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: FixesStatistics <files>+");
            System.exit(1);
        }
        Timer timer = new Timer();
        Timer total = new Timer();

        total.start();

        Collection<Long> set = new HashSet<Long>();

        for (String file : args) {
            /*
            timer.start();
            Collection<Fix> fixes = Fix.loadFixes(file);
            System.err.println("loading from " + file + ": " + timer.getTime());

            timer.start();
            for (Fix f : fixes) {
                long magic = (((long) f.file) << 32) + f.line;
                set.add(magic);
            }
            fixes = null;
            System.err.println("iterating: " + timer.getTime());
            System.err.println("set.calculateSize: " + set.calculateSize());
            System.err.println();
            */
            timer.start();
            BufferedReader br = new BufferedReader(new FileReader(file));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                StringTokenizer st = new StringTokenizer(line);
                int f = Integer.parseInt(st.nextToken());
                int l = Integer.parseInt(st.nextToken());
                long magic = (((long) f) << 32) + l;
                set.add(magic);
            }
            System.err.println(file + ": " + timer.getTime());
            System.err.println("set.size: " + set.size());
        }

        System.err.println("total time: " + total.getTime());

        System.out.println("The number of reads fixed: " + set.size());

    }
}
