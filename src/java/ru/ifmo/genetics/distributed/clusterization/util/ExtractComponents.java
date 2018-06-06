package ru.ifmo.genetics.distributed.clusterization.util;


import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ExtractComponents {
    public static void main(String[] args) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        Map<String, PrintWriter> files = new HashMap<String, PrintWriter>();
        while (true) {
            String s = input.readLine();
            if (s == null) break;
            String fn = s.substring(0, s.indexOf('\t'));
            if (!files.containsKey(fn)) {
                for (PrintWriter pw : files.values()) {
                    pw.close();
                }
                files.clear();
                new File(fn.substring(0, 2)).mkdir();
                files.put(fn, new PrintWriter(fn.substring(0, 2) + "/" + fn.substring(2)));
            }
            files.get(fn).println(s.substring(s.indexOf('\t') + 1));
        }

    }
}
