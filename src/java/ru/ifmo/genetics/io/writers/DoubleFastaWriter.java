package ru.ifmo.genetics.io.writers;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.LightDnaQ;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.utils.TextUtils;

import java.io.*;

public class DoubleFastaWriter {


    // ==== static write ====

    public static void writeToFileNow(Iterable<DnaQ> dnaqs, QualityFormat qf, String filePrefix)
            throws IOException {
        writeToFileNow(dnaqs, qf, filePrefix, false);
    }

    public static void writeToFileNow(Iterable<DnaQ> dnaqs, QualityFormat qf, String filePrefix, boolean printNs)
            throws IOException {
        PrintWriter out1 = new PrintWriter(new FileOutputStream(filePrefix + ".fasta"));
        PrintWriter out2 = new PrintWriter(new FileOutputStream(filePrefix + ".qual"));
        long i = 1;
        String baseName = filePrefix;
        int id = 1;
        if (filePrefix.endsWith("_1") || filePrefix.endsWith("_2")) {
            baseName = baseName.substring(0, baseName.length() - 2);
            id = filePrefix.endsWith("_2") ? 2 : 1;
        }
        for (DnaQ dnaQ : dnaqs) {
            out1.println(">" + baseName + ":" + i + "#0/" + id);
            out2.println(">" + baseName + ":" + i + "#0/" + id);
            ++i;

            out1.println(DnaTools.toString(dnaQ, printNs));
            for (int j = 0; j < dnaQ.length(); ++j) {
                out2.print(dnaQ.phredAt(j) + " ");
            }
            out2.println();
        }
        out1.close();
        out2.close();
    }

}
