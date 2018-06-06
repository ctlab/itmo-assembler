package ru.ifmo.genetics.tools;

import ru.ifmo.genetics.tools.ec.BinqTruncater;
import ru.ifmo.genetics.tools.ec.KmerStatisticsGatherer;
import ru.ifmo.genetics.tools.io.ToBinqConverter;
import ru.ifmo.genetics.tools.transcriptome.ConnectedComponentsAssembler;
import ru.ifmo.genetics.tools.transcriptome.SmallComponentsAssembler;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.Yielder;

import java.io.File;


public class SmallTranscriptsAssembler extends Tool {
    public static final String NAME = "small-transcripts-assembler";
    public static final String DESCRIPTION = "assembles obvious transcripts";

    // input params

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
            .withDefaultValue(workDir.append("transcriptome"))
            .create());


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
                                       /*
    public final TranscriptomeKmerStatisticsGatherer kmerDumper = new TranscriptomeKmerStatisticsGatherer();
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
        setFixDefault(kmerDumper.prefixesFile);
        setFixDefault(kmerDumper.maxSize);
        setFix(kmerDumper.maximalBadFrequency, 1);
        setFix(kmerDumper.kmerIteratorFactory, new MutableBigKmerIteratorFactory());
        addSubTool(kmerDumper);
    }                                    */


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
        setFix(kmerDumper.outputCounts,true);
        setFix(kmerDumper.ignoreBadKmers,true);
        addSubTool(kmerDumper);
    }

    public final ConnectedComponentsAssembler componentsSearcher = new ConnectedComponentsAssembler();
    {
        setFix(componentsSearcher.kParameter, kParameter);
        setFix(componentsSearcher.kmersFiles, kmerDumper.outputDir.get().listFiles());
        setFixDefault(componentsSearcher.outFilePrefix);
        addSubTool(componentsSearcher);
    }

    public final SmallComponentsAssembler smallTrAssembler = new SmallComponentsAssembler();
    {
        setFix(smallTrAssembler.kParameter, kParameter);
        setFix(smallTrAssembler.filePrefix, componentsSearcher.outFilePrefix);
        addSubTool(smallTrAssembler);
    }

    @Override
    protected void runImpl() throws ExecutionFailedException {
        addStep(converter);
        addStep(truncater);
        addStep(kmerDumper);
        addStep(componentsSearcher);
        addStep(smallTrAssembler);
    }

    @Override
    protected void cleanImpl() {

    }

    public SmallTranscriptsAssembler() {
        super(NAME, DESCRIPTION);
    }

    // ----------------------------------------------------------------------------------------------------------------
    public static void main(String[] args) {
        new SmallTranscriptsAssembler().mainImpl(args);
    }

}


