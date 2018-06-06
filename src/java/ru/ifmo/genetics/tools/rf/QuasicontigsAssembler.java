package ru.ifmo.genetics.tools.rf;

import ru.ifmo.genetics.dna.kmers.MutableBigKmerIteratorFactory;
import ru.ifmo.genetics.tools.ec.BinqTruncater;
import ru.ifmo.genetics.tools.ec.KmerStatisticsGatherer;
import ru.ifmo.genetics.tools.io.ToBinqConverter;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.*;
import ru.ifmo.genetics.utils.tool.values.InValue;
import ru.ifmo.genetics.utils.tool.values.Yielder;

import java.io.File;

public class QuasicontigsAssembler extends Tool {
    public static final String NAME = "quasicontigs-assembler";
    public static final String DESCRIPTION = "assembles quasicontigs from paired-end reads";

    public final Parameter<Integer> kParameter = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size")
            .create());

    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("input-files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("paired reads to process")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .optional()
            .withShortOpt("o")
            .withDescription("directory to output built quasicontigs")
            .withDefaultValue(workDir.append("quasicontigs"))
            .create());



    // tools
    public final ToBinqConverter converter = new ToBinqConverter();
    {
        setFix(converter.inputFiles, inputFiles);
        setFixDefault(converter.outputDir);
        addSubTool(converter);
    }

    public final BinqTruncater truncater = new BinqTruncater();
    {
        setFix(truncater.inputFiles, converter.convertedReadsOut);
        setFixDefault(truncater.outputDir);
        addSubTool(truncater);
    }

    public final KmerStatisticsGatherer kmerDumper = new KmerStatisticsGatherer();
    {
        setFix(kmerDumper.inputFiles, truncater.truncatedReadsOut);
        setFix(kmerDumper.k, new Yielder<Integer>() {
            @Override
            public Integer yield() {
                return kParameter.get() + 1;
            }

            @Override
            public String description() {
                return "k + 1";
            }
        });
        setFixDefault(kmerDumper.outputDir);
        setFixDefault(kmerDumper.outputPrefixesFile);
        setFixDefault(kmerDumper.maxSize);
        setFix(kmerDumper.maximalBadFrequency, 1);
        setFix(kmerDumper.kmerIteratorFactory, new MutableBigKmerIteratorFactory());
        addSubTool(kmerDumper);
    }

    public final GraphBuilder graphBuilder = new GraphBuilder();
    {
        setFix(graphBuilder.kParameter, kParameter);
        setFix(graphBuilder.kmersFiles, kmerDumper.goodKmersFilesOut);
        setFixDefault(graphBuilder.graphFile);
        addSubTool(graphBuilder);
    }

    public final ReadsFiller readsFiller = new ReadsFiller();
    {
        setFix(readsFiller.kParameter, kParameter);
        setFix(readsFiller.outputDir, outputDir);
        setFix(readsFiller.graphFile, graphBuilder.graphFile);
        setFix(readsFiller.readFiles, converter.convertedReadsOut);
        addSubTool(readsFiller);
    }


    // output parameters
    public final InValue<File[]> resultingQuasicontigsOut = addOutput("resulting-quasicontigs", readsFiller.resultingQuasicontigsOut, File[].class);



    @Override
    protected void runImpl() throws ExecutionFailedException {
        addStep(converter);
        addStep(truncater);
        addStep(kmerDumper);
        addStep(graphBuilder);
        addStep(readsFiller);

        progress.stepCoef = new double[]{0, 0.02, 0.02, 0, 0.96};
    }

    @Override
    protected void cleanImpl() {
    }

    @Override
    public String getStageName(int curLang) {
        return (curLang == 0) ? "Quasicontigs assembly" : "Сборка квазиконтигов";
    }

    public QuasicontigsAssembler() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new QuasicontigsAssembler().mainImpl(args);
    }
}
