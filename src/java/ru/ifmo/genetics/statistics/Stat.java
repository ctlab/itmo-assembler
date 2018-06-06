package ru.ifmo.genetics.statistics;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class Stat {
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        Map<Integer, Integer> hm = new TreeMap<Integer, Integer>();
        while (true) {
            String s = br.readLine();
            if (s.length() < 30)
                break;
            StringTokenizer st = new StringTokenizer(s);
            st.nextToken();
            int x = Integer.parseInt(st.nextToken());
            if (!hm.containsKey(x)) {
                hm.put(x, 0);
            }
            hm.put(x, hm.get(x) + 1);
        }
        PrintWriter out = new PrintWriter("ATATA.log");
        for (Map.Entry<Integer, Integer> e : hm.entrySet()) {
            out.println(e.getKey() + " " + e.getValue());
        }
        out.close();

    }
}
