package ru.ifmo.genetics.tools.ec.simple;

import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.*;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CleanAll extends Tool {
    public static final String NAME = "clean-all";
    public static final String DESCRIPTION = "runs cleaner on each prefix";

    public final Parameter<Integer> k = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size")
            .create());

    public final Parameter<Integer> maximalIndelsNumber = addParameter(new IntParameterBuilder("maximal-indels-number")
            .mandatory()
            .withDescription("maximal indels number")
            .create());

    public final Parameter<Integer> maximalSubsNumber = addParameter(new IntParameterBuilder("maximal-subs-number")
            .mandatory()
            .withDescription("maximal substitutions number")
            .create());

    public final Parameter<File> prefixesFile = addParameter(new FileParameterBuilder("prefix")
            .mandatory()
            .withDescription("prefix")
            .create());

    public final Parameter<File> kmersDir = addParameter(new FileParameterBuilder("kmers-dir")
            .withDefaultValue(workDir.append("kmers"))
            .withDescription("directory with good and bad kmer files")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .withDefaultValue(workDir.append("fixes"))
            .withShortOpt("o")
            .withDescription("directory for fixes")
            .create());

    public final Parameter<Long> badKmersNumber = addParameter(new LongParameterBuilder("bad-kmers-number")
            .mandatory()
            .withDescription("the number of bad kmers")
            .create());

    private InMemoryValue<File[]> fixesOutValue = new InMemoryValue<File[]>();
    public InValue<File[]> fixesOut = addOutput("fixes-files", fixesOutValue, File[].class);

    private List<Cleaner> list = new ArrayList<Cleaner>();

    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            BufferedReader br = new BufferedReader(new FileReader(prefixesFile.get()));
            while (true) {
                String prefix = br.readLine();
                if (prefix == null) {
                    break;
                }
                Cleaner c = new Cleaner(prefix);
                setFix(c.prefixParameter, prefix);
                setFix(c.k, k);
                setFix(c.kmersDir, kmersDir);
                setFix(c.outputDir, outputDir);
                setFix(c.maximalIndelsNumber, maximalIndelsNumber);
                setFix(c.maximalSubsNumber, maximalSubsNumber);
                setFix(c.badKmersNumber, badKmersNumber);
                addSubTool(c);
                addStep(c);
                list.add(c);
            }
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
    }

    @Override
    protected void cleanImpl() {
        int size = 0;
        for (Cleaner c : list) {
            size += c.fixesFilesOut.get().length;
        }
        File[] fixes = new File[size];
        int i = 0;
        for (Cleaner c : list) {
            for (File f : c.fixesFilesOut.get()) {
                fixes[i++] = f;
            }
        }
        fixesOutValue.set(fixes);
    }

    public CleanAll() {
        super(NAME, DESCRIPTION);
    }
}
