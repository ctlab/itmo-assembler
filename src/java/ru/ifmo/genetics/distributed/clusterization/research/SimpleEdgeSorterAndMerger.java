package ru.ifmo.genetics.distributed.clusterization.research;

import java.io.*;
import java.util.Arrays;

public class SimpleEdgeSorterAndMerger {
    private final String inputFileName;
    private final String outputFileName;
    private long[] data;

    public SimpleEdgeSorterAndMerger(String inputFileName, String outputFileName) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
    }

    public void run() {
        try {
            loadInput();
            Arrays.sort(data);
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFileName)));
            int count = 0;
            for (int i = 0; i < data.length; i++) {
                if (i == 0 || data[i] == data[i - 1]) {
                    count++;
                } else {
                    output.writeLong(data[i - 1]);
                    output.writeInt(count);
                    count = 1;
                }
            }
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void loadInput() throws IOException {
        File inputFile = new File(inputFileName);
        DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
        data = new long[(int) (inputFile.length() / 8)];
        for (int i = 0; i < data.length; i++) {
            data[i] = input.readLong();
        }
        input.close();
    }


    public static void main(String[] args) {
        new SimpleEdgeSorterAndMerger(args[0], args[1]).run();
    }
}
