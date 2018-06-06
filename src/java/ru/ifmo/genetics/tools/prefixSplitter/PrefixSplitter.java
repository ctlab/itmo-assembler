package ru.ifmo.genetics.tools.prefixSplitter;

import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.pairs.MutablePair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PrefixSplitter {

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.err.println("Usage: PrefixSplitter <file> <bucket size> [human-readable]");
            System.exit(1);
        }

        boolean printNumbers = (args.length > 2);

        String file = args[0];
        long size = Long.parseLong(args[1]) / 4;

        System.err.println("splitting by buckets of size " + size);
        
        Map<String, Long> map = new TreeMap<String, Long>();

        BufferedReader br = new BufferedReader(new FileReader(file));
        long s1 = 0, s2 = 0;
        for (;;) {
            String line = br.readLine();
            if (line == null)
                break;
            int len = line.indexOf(' ');
            String prefix = line.substring(0, len);
            line = line.substring(len + 1);
            long value = Long.parseLong(line);
            s1 += value;

            for (int i = 1; i <= prefix.length(); ++i) {
                Misc.addLong(map, prefix.substring(0, i), value);
            }
        }
        map.put("", s1);

        int maxLen = 64;
        boolean flag = false;

        List<MutablePair<String, Long>> temp = new ArrayList<MutablePair<String, Long>>();
        
        for (Map.Entry<String, Long> e : map.entrySet()) {
            if (flag && (e.getKey().length() > maxLen)) {
                continue;
            }
            flag = false;
            if (e.getValue() <= size) {
                maxLen = e.getKey().length();
                flag = true;
                // System.out.println(e.getKey() + " " + e.getValue());
                temp.add(MutablePair.make(e.getKey(), e.getValue()));
                s2 += e.getValue();
                continue;
            }
        }

        for (MutablePair<String, Long> p : temp) {
            System.err.println(p.first + " " + p.second);
        }

        if (s1 != s2) {
            System.err.println("Sums are different:");
            System.err.println("s1 = " + s1 + "; s2 = " + s2);
            System.exit(1);
        }

        long curSum = 0;
        for (int i = 0; i < temp.size(); ++i) {
            if (temp.get(i) == null) {
                continue;
            }
            curSum = temp.get(i).second;
            System.out.print(temp.get(i).first + " ");
            temp.set(i, null);
            for (int j = i + 1; j < temp.size(); ++j) {
                if (temp.get(j) == null) {
                    continue;
                }
                if (curSum + temp.get(j).second <= size) {
                    curSum += temp.get(j).second;
                    System.out.print(temp.get(j).first + " ");
                    temp.set(j, null);
                }
            }
            if (printNumbers) {
                System.out.print(curSum);
            }
            System.out.println();
        }

    }

}
