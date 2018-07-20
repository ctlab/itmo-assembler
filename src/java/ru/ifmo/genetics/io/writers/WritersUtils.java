package ru.ifmo.genetics.io.writers;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.dna.LightDnaQ;
import ru.ifmo.genetics.io.formats.Illumina;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.IteratorUtils;

import java.io.*;
import java.util.zip.GZIPOutputStream;

public class WritersUtils {

//    ======================== Dna Writers (direct write) =============================

    public static void writeDnasToFastaFile(Iterable<? extends LightDna> data, File file) throws IOException {
        writeDnasToFastaFile(null, data, file, false);
    }

    public static void writeDnasToFastaFile(Iterable<? extends LightDna> data, File file, boolean compress) throws IOException {
        writeDnasToFastaFile(null, data, file, compress);
    }

    public static void writeDnasToFastaFile(Iterable<String> comments, Iterable<? extends LightDna> data,
                                      File file, boolean compress) throws IOException {
        if (comments == null) {
            comments = new DataCounter();
        }

        PrintWriter out;
        if (compress) {
            file = new File(FileUtils.addExtensionIfNot(file.getPath(), ".gz"));
            out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
        } else {
            out = new PrintWriter(file);
        }

        FastaDedicatedWriter.writeData(comments, data, out, false);
        out.close();
    }



//    ======================== DnaQ Writers (direct write) =============================


    public static void writeDnaQsToFastqFile(Iterable<? extends LightDnaQ> data, File file) throws IOException {
        writeDnaQsToFastqFile(null, data, Illumina.instance, file, false);
    }

    public static void writeDnaQsToFastqFile(Iterable<? extends LightDnaQ> data, QualityFormat qf, File file) throws IOException {
        writeDnaQsToFastqFile(null, data, qf, file, false);
    }

    public static void writeDnaQsToFastqFile(Iterable<? extends LightDnaQ> data, QualityFormat qf, File file, boolean compress) throws IOException {
        writeDnaQsToFastqFile(null, data, qf, file, compress);
    }

    public static void writeDnaQsToFastqFile(Iterable<String> comments, Iterable<? extends LightDnaQ> data, QualityFormat qf,
                                      File file, boolean compress) throws IOException {
        if (comments == null) {
            comments = new DataCounter();
        }

        PrintWriter out;
        if (compress) {
            file = new File(FileUtils.addExtensionIfNot(file.getPath(), ".gz"));
            out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
        } else {
            out = new PrintWriter(file);
        }

        FastqDedicatedWriter.writeData(comments, data, out, qf, false);
        out.close();
    }


    public static void writeDnaQsToBinqFile(Iterable<DnaQ> data, File file) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        BinqDedicatedWriter.writeDataStatic(data, out, true);       // throws IllegalArgumentException on empty file
        out.close();
    }

}
