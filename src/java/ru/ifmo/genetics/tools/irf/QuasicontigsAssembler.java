package ru.ifmo.genetics.tools.irf;

import ru.ifmo.genetics.dna.kmers.MutableBigKmerIteratorFactory;
import ru.ifmo.genetics.tools.ec.KmerStatisticsGatherer;
import ru.ifmo.genetics.tools.io.ToBinqConverter;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InValue;
import ru.ifmo.genetics.utils.tool.values.Yielder;

import java.io.File;

public class QuasicontigsAssembler extends Tool {
    public static final String NAME = "indel-quasicontigs-assembler";
    public static final String DESCRIPTION = "assembles quasicontigs from paired-end reads with indels";

    public final Parameter<Integer> kParameter = addParameter(new IntParameterBuilder("k-mer")
            .withShortOpt("k")
            .withDefaultValue(19)
            .withDescriptionShort("k-mer size")
            .withDescription("k-mer size (used in de Bruijn graph, quasicontigs assembler)")
            .withDescriptionRuShort("Длина k-мера")
            .withDescriptionRu("Длина k-мера (используется в графе де Брёйна, сборка квазиконтигов)")
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

    public final Parameter<Integer> minGoodFrequency = addParameter(new IntParameterBuilder("min-good-frequency")
            .optional()
            .withShortOpt("g")
            .withDefaultValue(2)
            .withDescriptionShort("Min. good frequency in quasicontigs assembler")
            .withDescription("minimal k-mer frequency to be counted as good in quasicontigs assembler")
            .withDescriptionRuShort("Мин. частота хорошего k-mer'а")
            .withDescriptionRu("Минимальная частота k-mer'a для принятия его как хорошего в сборке квазиконтигов")
            .create());


    // steps
    public final ToBinqConverter inputConverter = new ToBinqConverter();
    {
        setFix(inputConverter.inputFiles, inputFiles);
        setFixDefault(inputConverter.outputDir);
        addSubTool(inputConverter);
    }

    public final KmerStatisticsGatherer kmerDumper = new KmerStatisticsGatherer();
    {
        setFix(kmerDumper.inputFiles, inputConverter.convertedReadsOut);
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
        setFix(kmerDumper.outputCounts, true);
        setFix(kmerDumper.ignoreBadKmers, true);
        setFix(kmerDumper.kmerIteratorFactory, new MutableBigKmerIteratorFactory());
        addSubTool(kmerDumper);
    }

    public final GraphBuilder graphBuilder = new GraphBuilder();
    {
        setFix(graphBuilder.kParameter, kParameter);
        setFix(graphBuilder.kmersFiles, kmerDumper.goodKmersFilesOut);
        setFixDefault(graphBuilder.graphFile);
        setFix(graphBuilder.minWeightToReallyAdd, minGoodFrequency);
        addSubTool(graphBuilder);
    }

    public final ReadsFiller readsFiller = new ReadsFiller();
    {
        setFix(readsFiller.kParameter, kParameter);
        setFix(readsFiller.outputDir, outputDir);
        setFix(readsFiller.graphFile, graphBuilder.graphFile);
        setFix(readsFiller.readFiles, inputConverter.convertedReadsOut);
        addSubTool(readsFiller);
    }


    // output parameters
    public final InValue<File[]> resultingQuasicontigsOut = addOutput("resulting-quasicontigs", readsFiller.resultingQuasicontigsOut, File[].class);


    @Override
    protected void runImpl() throws ExecutionFailedException {
        addStep(inputConverter);
        addStep(kmerDumper);
        addStep(graphBuilder);
        addStep(readsFiller);

        progress.stepCoef = new double[]{0.01, 0.02, 0.02, 0.95};
    }

    public static void main(String[] args) {
        new QuasicontigsAssembler().mainImpl(args);
    }

    @Override
    protected void cleanImpl() {
    }

    public QuasicontigsAssembler() {
        super(NAME, DESCRIPTION);
    }
}
