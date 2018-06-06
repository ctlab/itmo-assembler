package ru.ifmo.genetics.distributed.io;

import java.io.*;

public class TwoSingleFastqToOnePairedFastq {
    public static void main(String[] args) throws IOException {
        String filename1 = args[0];
        String filename2 = args[1];
        String outputFilename = args[2];
        BufferedReader br1 = new BufferedReader(new FileReader(filename1));
        BufferedReader br2 = new BufferedReader(new FileReader(filename2));
        PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(outputFilename)));
        long it = 0;
        wh:
        while (true) {
            String[] a1 = new String[4];
            for (int i = 0; i < a1.length; i++) {
                a1[i] = br1.readLine();
                if (a1[i] == null) {
                    break wh;
                }
            }
            a1[0] += "#" + it + "#1";
            for (int i = 0; i < a1.length; i++) {
                output.println(a1[i]);
            }
            String[] a2 = new String[4];
            for (int i = 0; i < a2.length; i++) {
                a2[i] = br2.readLine();
            }
            a2[0] += "#" + it + "#2";
            for (int i = 0; i < a2.length; i++) {
                output.println(a2[i]);
            }
            it++;
        }
        output.close();
        br1.close();
        br2.close();
    }
}
