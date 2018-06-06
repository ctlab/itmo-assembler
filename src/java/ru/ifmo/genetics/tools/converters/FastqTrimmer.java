package ru.ifmo.genetics.tools.converters;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.DnaQView;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.io.sources.PairSource;
import ru.ifmo.genetics.utils.pairs.UniPair;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

public class FastqTrimmer extends Tool {
    public static String NAME = "fastq-trimmer";
    public static String DESCRIPTION = "trims paired fastq files to the same length";


    // input params
    public final Parameter<File> in1 = addParameter(new FileParameterBuilder("in1")
            .mandatory()
            .withDescription("first fastq file")
            .create());

    public final Parameter<File> in2 = addParameter(new FileParameterBuilder("in2")
            .mandatory()
            .withDescription("second fastq file")
            .create());

    public final Parameter<Integer> trimLengthParam = addParameter(new IntParameterBuilder("trim-length")
            .mandatory()
            .withDefaultValue(25)
            .withDescription("trim length")
            .create());


    @Override
    public void runImpl() throws IOException {
        int trimLength = trimLengthParam.get();
        NamedSource<DnaQ> s1 = ReadersUtils.readDnaQLazy(in1.get());
        NamedSource<DnaQ> s2 = ReadersUtils.readDnaQLazy(in2.get());

        PrintWriter outOriginal1 = new PrintWriter(workDir.append(s1.name() + ".original.fq").get());
        PrintWriter outOriginal2 = new PrintWriter(workDir.append(s2.name() + ".original.fq").get());
        PrintWriter out1 = new PrintWriter(workDir.append(s1.name() + ".trimmed.fq").get());
        PrintWriter out2 = new PrintWriter(workDir.append(s2.name() + ".trimmed.fq").get());

        int i = 0;

        for (UniPair<DnaQ> pair: new PairSource<DnaQ>(s1, s2)) {
            DnaQ first = pair.first();
            DnaQ second = pair.second();

            if (first.length() < trimLength || second.length() < trimLength) {
                continue;
            }

            DnaQView firstTrimmed = new DnaQView(first, 0, trimLength);
            DnaQView secondTrimmed = new DnaQView(second, 0, trimLength);


            outOriginal1.printf("@%d\n", i);
            outOriginal1.printf("%s\n", DnaTools.toString(first));
            outOriginal1.printf("+\n");
            outOriginal1.printf("%s\n", DnaTools.toPhredString(first));

            outOriginal2.printf("@%d\n", i);
            outOriginal2.printf("%s\n", DnaTools.toString(second));
            outOriginal2.printf("+\n");
            outOriginal2.printf("%s\n", DnaTools.toPhredString(second));

            out1.printf("@%d\n", i);
            out1.printf("%s\n", DnaTools.toString(firstTrimmed));
            out1.printf("+\n");
            out1.printf("%s\n", DnaTools.toPhredString(firstTrimmed));

            out2.printf("@%d\n", i);
            out2.printf("%s\n", DnaTools.toString(secondTrimmed));
            out2.printf("+\n");
            out2.printf("%s\n", DnaTools.toPhredString(secondTrimmed));
            i++;
        }

        outOriginal1.close();
        outOriginal2.close();
        out1.close();
        out2.close();
    }

    public static void main(String[] args) throws ExecutionFailedException, FileNotFoundException {
        new FastqTrimmer().mainImpl(args);
    }

    @Override
    public void cleanImpl() {
    }


    public FastqTrimmer() {
        super(NAME, DESCRIPTION);
    }
}
