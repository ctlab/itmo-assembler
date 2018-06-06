package ru.ifmo.genetics.statistics;

import ru.ifmo.genetics.utils.Misc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class MergeKmersStatistics {
    public static void main(String[] args) throws IOException  {
        Map<Integer, Long> map = new TreeMap<Integer, Long>();
        for (String file : args) {
            System.err.print(file + "... ");
            BufferedReader br = new BufferedReader(new FileReader(file));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                StringTokenizer st = new StringTokenizer(line);
                int x = Integer.parseInt(st.nextToken());
                long y = Long.parseLong(st.nextToken());
                Misc.addLong(map, x, y);
            }
            System.err.println("done");
        }
        for (Map.Entry<Integer, Long> e : map.entrySet()) {
            System.out.println(e.getKey() + " " + e.getValue());
        }
    }
}
