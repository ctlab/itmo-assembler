package ru.ifmo.genetics.statistics;

import ru.ifmo.genetics.utils.Misc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Class that can help you to answer the question
 *   "how mush times does certain element G present in input data?"
 */
public class QuickQuantitativeStatistics<G extends Comparable<G>> {
    HashMap<G, Long> map;

    public QuickQuantitativeStatistics() {
        map = new HashMap<G, Long>();
    }

    public void add(G g) {
        Misc.addLong(map, g, 1);
    }
    
    public void set(G g, long value) {
        map.put(g, value);
    }
    
    public long get(G g) {
        Long v = map.get(g);
        if (v == null) {
            v = 0L;
        }
        return v;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        Set<Map.Entry<G, Long>> set = map.entrySet();
        ArrayList<Map.Entry<G, Long>> array = new ArrayList<Map.Entry<G, Long>>(set);
        Collections.sort(array, new Comparator<Map.Entry<G, Long>>() {
            @Override
            public int compare(Map.Entry<G, Long> o1, Map.Entry<G, Long> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        for (Map.Entry<G, Long> e : array) {
            sb.append(e.getKey() + "\t" + e.getValue());
            sb.append('\n');
        }
        return sb.toString();
    }


    public void printToFile(File file) throws FileNotFoundException {
        printToFile(file, null);
    }

    /**
     * @param headerString may be null, in such case no header string are written to file
     */
    public void printToFile(File file, String headerString) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(file);
        if (headerString != null) {
            out.println(headerString);
        }
        out.println(toString());
        out.close();
    }

}
