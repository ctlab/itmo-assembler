package ru.ifmo.genetics.tools;

import ru.ifmo.genetics.tools.io.ToBinqConverter;
import ru.ifmo.genetics.transcriptome.TranscriptomeContigsAssembler;
import ru.ifmo.genetics.transcriptome.TranscriptomeKmerStatisticsGatherer;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.BoolParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;

import java.io.File;

public class TranscriptomeAssembler extends Tool {
    public static final String NAME = "transcriptome-assembler";
    public static final String DESCRIPTION = "transcriptome assembler";

    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("input-files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("reads to process")
            .create());

    public final Parameter<Integer> k = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k")
            .create());

    public final Parameter<Integer> maximalSubsNumber = addParameter(new IntParameterBuilder("maximal-subs-number")
            .withDefaultValue(0)
            .withDescription("maximal substitutions number per k-mer")
            .create());

    public final Parameter<Integer> maximalIndelsNumber = addParameter(new IntParameterBuilder("maximal-indels-number")
            .withDefaultValue(0)
            .withDescription("maximal indels number per k-mer")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .withDefaultValue(workDir.append("corrected"))
            .withShortOpt("o")
            .withDescription("directory for output files")
            .create());

    public final Parameter<Boolean> applyToOriginalReads = addParameter(new BoolParameterBuilder("apply-to-original")
            .optional()
            .withDescription("if set applies fixes to original not truncated reads")
            .create());

    public final ToBinqConverter converter = new ToBinqConverter();
    {
        setFix(converter.inputFiles, inputFiles);
        setFixDefault(converter.outputDir);
        addSubTool(converter);
    }
    /*
    public final BinqTruncater truncater = new BinqTruncater();
    {
        setFix(truncater.inputFiles, converter.convertedReadsOut);
        setFixDefault(truncater.outputDir);
        addSubTool(truncater);
    }
    */
    public final TranscriptomeKmerStatisticsGatherer gatherer = new TranscriptomeKmerStatisticsGatherer();
    {
        setFix(gatherer.inputFiles, /*truncater.truncatedReadsOut*/converter.convertedReadsOut);
        setFixDefault(gatherer.maxSize);
        setFix(gatherer.k, k);
        setFixDefault(gatherer.prefixesFile);
        setFixDefault(gatherer.outputDir);
        addSubTool(gatherer);
    }

    public final TranscriptomeContigsAssembler contigsAssebler = new TranscriptomeContigsAssembler();
    {
        setFix(contigsAssebler.folderNameWithBaskets,gatherer.outputDir);
        setFix(contigsAssebler.kMerSize, k);
        setFixDefault(contigsAssebler.memSize);
        setFix(contigsAssebler.inputDir,/*truncater.outputDir*/converter.outputDir);
        setFixDefault(contigsAssebler.outFile);
        setFixDefault(contigsAssebler.minContigLenght);
        setFixDefault(contigsAssebler.maxContigLenght);
        addSubTool(contigsAssebler);
    }

    @Override
    protected void runImpl() throws ExecutionFailedException {
        addStep(converter);
        //addStep(truncater);
        addStep(gatherer);
        addStep(contigsAssebler);

        //addStep(cleanAll);
        //addStep(fixesApplier);
    }

    @Override
    protected void cleanImpl() {
    }

    public TranscriptomeAssembler() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new TranscriptomeAssembler().mainImpl(args);
    }

}


