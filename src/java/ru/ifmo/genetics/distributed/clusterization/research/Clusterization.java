package ru.ifmo.genetics.distributed.clusterization.research;

import java.io.DataInput;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Clusterization {
    final String edgeFile;
    final int edgesCount;
    final int readsCount;
    final int maxComponentCount;
    final int[] p;
    final int[] size;

    public Clusterization(String edgeFile, int edgesCount, int readsCount, int maxComponentCount) {
        this.edgeFile = edgeFile;
        this.edgesCount = edgesCount;
        this.readsCount = readsCount;
        this.maxComponentCount = maxComponentCount;
        p = new int[readsCount];
        for (int i = 0; i < p.length; i++) {
            p[i] = i;
        }
        size = new int[readsCount];
        Arrays.fill(size, 1);
    }

    int get(int v) {
        if (p[v] == v) {
            return v;
        }
        return p[v] = get(p[v]);
    }

    /*
        a and b MUST be roots
     */
    void union(int a, int b) {
        assert p[a] == a;
        assert p[b] == b;
        if (size[a] < size[b]) {
            p[a] = b;
            size[b] += size[a];
            size[a] = Integer.MIN_VALUE;
        } else {
            p[b] = a;
            size[a] += size[b];
            size[b] = Integer.MIN_VALUE;
        }
    }

    public void run() {
        try {
            DataInput input = new PlainTextFastDataInput(new FileReader(edgeFile));
            for (int i = 0; i < edgesCount; i++) {
                int count = input.readInt();
                //int dataset1 = input.readInt();
                //assert dataset1 == 0;
                int read1 = input.readInt();
                //int dataset2 = input.readInt();
                //assert dataset2 == 0;
                int read2 = input.readInt();
                int p1 = get(read1);
                int p2 = get(read2);
                if (p1 == p2) continue;
                if (size[p1] + size[p2] <= maxComponentCount) {
                    union(p1, p2);
                }
            }
            Map<Integer, PrintWriter> writers = new HashMap<Integer, PrintWriter>();
            for (int i = 0; i < p.length; i++) {
                if (size[p[i]] > 1000) {
                    if (!writers.containsKey(p[i])) {
                        writers.put(p[i], new PrintWriter("comp" + p[i]));
                    }
                    writers.get(p[i]).println(i);
                    if (p[i] == i) {
                        System.err.println(size[i] + " " + i);
                    }
                }
            }
            for (PrintWriter pw : writers.values()) {
                pw.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Clusterization(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3])).run();
    }

}
