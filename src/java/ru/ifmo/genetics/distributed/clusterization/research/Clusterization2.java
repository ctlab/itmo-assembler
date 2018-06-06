package ru.ifmo.genetics.distributed.clusterization.research;

import java.io.DataInput;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Clusterization2 {
    final String edgeFile;
    final int edgesCount;
    final int readsCount;
    
    class Edge {
        int to, weight;
        public Edge(int to, int weight) {
            this.to = to;
            this.weight = weight;
        }
    }
    ArrayList<Edge>[] edges;
    private int barrier;
    private int count;
    private int size;

    public Clusterization2(String edgeFile, int edgesCount, int readsCount, int size, int barrier, int count) {
        this.edgeFile = edgeFile;
        this.edgesCount = edgesCount;
        this.readsCount = readsCount;
        this.barrier = barrier;
        this.count = count;
        this.size = size;
        edges = new ArrayList[readsCount];
        for (int i = 0; i < readsCount; i++) {
            edges[i] = new ArrayList<Edge>(1);
        }
    }

    Random random = new Random(1);

    public void run() {
        try {
            DataInput input = new PlainTextFastDataInput(new FileReader(edgeFile));

            for (int i = 0; i < edgesCount; i++) {
                int count = input.readInt();
                int read1 = input.readInt();
                int read2 = input.readInt();
                edges[read1].add(new Edge(read2, count));
                edges[read2].add(new Edge(read1, count));
            }
            for (int it = 0; it < count; it++) {
              Set<Integer> component = new HashSet<Integer>();
              Queue<Integer> queue = new ArrayDeque<Integer>();
		      while (component.size() < size) {
                for (int w = barrier; w >= 1; w--) { 
                int start = random.nextInt(readsCount);
                component.add(start);
                queue.add(start);
                while (!queue.isEmpty() && component.size() < size) {
                    int v = queue.poll();
                    for (Edge e : edges[v]) {
                        if (e.weight >= w && component.size() < size && !component.contains(e.to)) {
                            component.add(e.to);
                            queue.add(e.to);
                        }
                    }
                }
                }  
              }	
                PrintWriter writer = new PrintWriter("comp" + it);
                for (int v : component) {
                    writer.println(v);
                }
                writer.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Clusterization2(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5])).run();
    }

}
