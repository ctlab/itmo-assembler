package ru.ifmo.genetics.tools.converters;

import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.NumUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.LongParameterBuilder;

import java.io.*;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import static ru.ifmo.genetics.utils.NumUtils.groupDigits;

public class Thinner extends Tool {
    public static final String NAME = "thinner";
    public static final String DESCRIPTION = "thins quasi-contigs";

    // input params
    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("input-files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("input files with quasi-contigs")
            .create());

    public final Parameter<File> outputFile = addParameter(new FileParameterBuilder("output-file")
            .withDefaultValue(workDir.append("reads.thinned.fasta"))
            .withDescription("output fasta file with thinned quasi-contigs")
            .create());


    public static final long maxReadsSizeValue =
            NumUtils.highestBits((Misc.availableMemory() * 9 / 10 - (long) 100e6) / 4, 3);

    public final Parameter<Long> maxReadsSize = addParameter(new LongParameterBuilder("max-reads-size")
            .withDefaultValue(maxReadsSizeValue)
            .memoryParameter()
            .withDefaultComment("auto")
            .withDescription("size of thinned quasicontigs (maximal summary length in nucleotides)")
            .create());


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            long newSize = maxReadsSize.get();

            Map<Integer, Long> hm = new TreeMap<Integer, Long>(new Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return b - a;
                }
            });
            for (File f : inputFiles.get()) {
                NamedSource<Dna> reader = ReadersUtils.readDnaLazy(f);
                for (Dna d : reader) {
                    int l = d.length();
                    Misc.incrementLong(hm, l);
                }
            }

            long curSize = 0;
            int minLength = 0;
            long last = 0;
            for (Map.Entry<Integer, Long> e : hm.entrySet()) {
                if (curSize + e.getKey() * e.getValue() > newSize) {
                    minLength = e.getKey();
                    last = (newSize - curSize) / e.getKey();
                    break;
                }
                curSize += e.getKey() * e.getValue();
            }

            long sumSize = 0;
            for (Map.Entry<Integer, Long> e : hm.entrySet()) {
                sumSize += e.getKey() * e.getValue();
            }

            PrintWriter out = new PrintWriter(new FileOutputStream(outputFile.get()));
            long resSize = 0;
            long oldReads = 0, newReads = 0;
            for (File f : inputFiles.get()) {
                NamedSource<Dna> reader = ReadersUtils.readDnaLazy(f);
                for (Dna d : reader) {
                    oldReads++;
                    int l = d.length();
                    if ((l > minLength) || (l == minLength && last > 0)) {
                        if (l == minLength) {
                            --last;
                        }
                        out.println("> " + newReads);
                        out.println(d.toString());
                        newReads++;
                        resSize += d.length();
                    }
                }
            }
            info("Old reads number = " + groupDigits(oldReads) + ", doubled = " + groupDigits(oldReads * 2));
            info("New reads number = " + groupDigits(newReads) + " = " +
                    String.format("%.2f", newReads * 100.0 / oldReads) + "% of all, " +
                    "doubled = " + groupDigits(newReads * 2));
            info("Resulting reads size = " + groupDigits(resSize) + " = " +
                            String.format("%.2f", resSize * 100.0 / sumSize) + " % of all " +
                    "(" + groupDigits(sumSize) + ")");
            debug("maxReadsSize = " + groupDigits(maxReadsSize.get()));
            out.close();
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
    }

    @Override
    protected void cleanImpl() {
    }

    public Thinner() {
        super(NAME, DESCRIPTION);
    }
}
