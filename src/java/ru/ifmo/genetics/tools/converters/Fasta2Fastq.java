package ru.ifmo.genetics.tools.converters;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.io.readers.DnaQReaderFromDnaSource;
import ru.ifmo.genetics.io.readers.FastaWithNsReader;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.io.writers.WritersUtils;

import java.io.File;

public class Fasta2Fastq {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: Fasta2Fastq <phred> <fasta file> <fastq file>");
            System.exit(1);
        }

        int phred = Integer.parseInt(args[0]);

        File sourceFile = new File(args[1]);
        NamedSource<String> source = new FastaWithNsReader(sourceFile);
        NamedSource<DnaQ> dnaqSource = new DnaQReaderFromDnaSource(source, phred);
        WritersUtils.writeDnaQsToFastqFile(dnaqSource, new File(args[2]));
    }
}
