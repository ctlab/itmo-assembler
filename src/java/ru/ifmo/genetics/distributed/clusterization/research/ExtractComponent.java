package ru.ifmo.genetics.distributed.clusterization.research;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class ExtractComponent {
    String indexFilename;
    String fastqFilename;
    String outputFilename;

    public ExtractComponent(String indexFilename, String fastqFilename, String outputFilename) {
        this.indexFilename = indexFilename;
        this.fastqFilename = fastqFilename;
        this.outputFilename = outputFilename;
    }

    public static void main(String[] args) {
        new ExtractComponent(args[0], args[1], args[2]).run();
    }

    private void run() {
        try {
            Set<Integer> indices = readIndices();
            BufferedReader br = new BufferedReader(new FileReader(fastqFilename));
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFilename)));
            String[] s = new String[4];
            int it = 0;
            wh:
            while (true) {
                for (int i = 0; i < s.length; i++) {
                    s[i] = br.readLine();
                    if (s[i] == null) break wh;
                }
                if (indices.contains(it++)) {
                    for (int i = 0; i < s.length; i++) {
                        out.println(s[i]);
                    }
                }
            }
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<Integer> readIndices() throws IOException {
        BufferedReader indicesBR = new BufferedReader(new FileReader(indexFilename));
        Set<Integer> indices = new HashSet<Integer>();
        while (true) {
            String s = indicesBR.readLine();
            if (s == null) {
                break;
            }
            indices.add(Integer.parseInt(s));
        }
        indicesBR.close();
        return indices;
    }
}
