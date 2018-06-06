package ru.ifmo.genetics.tools.ec.olcBased;

import ru.ifmo.genetics.tools.ec.*;
import ru.ifmo.genetics.tools.io.ToBinqConverter;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InValue;
import ru.ifmo.genetics.utils.tool.values.ListFilesYielder;

import java.io.File;

public class ErrorCorrector extends Tool {
    public static final String NAME = "error-corrector-olc-based";
    public static final String DESCRIPTION = "corrects errors using olc-based method";

    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("input-files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("reads to process")
            .create());

    public final Parameter<Integer> anchorLen = addParameter(new IntParameterBuilder("anchor-length")
            .withShortOpt("a")
            .withDefaultValue(19)
            .withDescriptionShort("Anchor length in error correction")
            .withDescription("anchor length in error correction")
            .withDescriptionRuShort("Длина якоря в исправл. ошибок")
            .withDescriptionRu("Длина якоря в исправлении ошибок")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .withDefaultValue(workDir.append("corrected"))
            .withShortOpt("o")
            .withDescription("directory for corrected files")
            .create());



    public final ToBinqConverter converter = new ToBinqConverter();
    {
        setFix(converter.inputFiles, inputFiles);
        setFixDefault(converter.outputDir);
        addSubTool(converter);
    }

    public final KmerStatisticsGatherer gatherer = new KmerStatisticsGatherer();
    {
        setFix(gatherer.k, anchorLen);
        setFix(gatherer.inputFiles, converter.convertedReadsOut);
        setFixDefault(gatherer.outputDir);
        setFixDefault(gatherer.outputPrefixesFile);
        addSubTool(gatherer);
    }

    public final DeBruijnGraphAnalyzer analyzer = new DeBruijnGraphAnalyzer();
    {
        setFix(analyzer.goodKmerFiles, new ListFilesYielder(gatherer.outputDir, ".*\\.good.*"));
        setFix(analyzer.anchorLen, anchorLen);
        setFixDefault(analyzer.outputChainFile);
        addSubTool(analyzer);
    }

    public final Kmer2ReadIndexBuilder builder = new Kmer2ReadIndexBuilder();
    {
        setFix(builder.anchorLen, anchorLen);
        setFix(builder.chainFile, analyzer.outputChainFile);
        setFix(builder.goodKmerFiles, analyzer.goodKmerFiles);
        setFix(builder.inputFiles, converter.convertedReadsOut);
        setFix(builder.outputDir, outputDir);
        setFixDefault(builder.outputIndexFile);
        addSubTool(builder);
    }

    // output parameters
    public final InValue<File[]> resultingReadsOut = addOutput("resulting-reads", builder.resultingReadsOut, File[].class);



    @Override
    protected void runImpl() throws ExecutionFailedException {
        addStep(converter);
        addStep(gatherer);
        addStep(analyzer);
        addStep(builder);

        progress.stepCoef = new double[]{0.01, 0.02, 0.02, 0.95};
    }

    @Override
    protected void cleanImpl() {
    }
    
    @Override
    public String getStageName(int curLang) {
        return (curLang == 0) ? "Error correcting" : "Исправление ошибок";
    }

    public ErrorCorrector() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new ErrorCorrector().mainImpl(args);
    }

}
