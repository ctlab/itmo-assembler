package ru.ifmo.genetics.tools.ec.simple;

import ru.ifmo.genetics.tools.ec.BinqTruncater;
import ru.ifmo.genetics.tools.ec.KmerStatisticsGatherer;
import ru.ifmo.genetics.tools.io.ToBinqConverter;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.BoolParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.IfYielder;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.File;

public class SimpleErrorCorrector extends Tool {
    public static final String NAME = "error-corrector-simple";
    public static final String DESCRIPTION = "corrects errors";

    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("input-files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("reads to process")
            .create());

    public final Parameter<Integer> k = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size")
            .create());

    public final Parameter<Integer> maximalSubsNumber = addParameter(new IntParameterBuilder("maximal-subs-number")
            .withDefaultValue(1)
            .withDescriptionShort("Max substitutions per k-mer")
            .withDescription("maximal substitutions number per k-mer")
            .withDescriptionRuShort("Макс. число исправлений в k-мере")
            .withDescriptionRu("Максимальное число исправлений замены в k-мере")
            .create());

    public final Parameter<Integer> maximalIndelsNumber = addParameter(new IntParameterBuilder("maximal-indels-number")
            .withDefaultValue(0)
            .withDescriptionShort("Max indels per k-mer")
            .withDescription("maximal indels number per k-mer")
            .withDescriptionRuShort("Макс. число вст./удал. в k-мере")
            .withDescriptionRu("Максимальное число исправлений вставки/удаления в k-мере")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .withDefaultValue(workDir.append("corrected"))
            .withShortOpt("o")
            .withDescription("directory for output files")
            .create());

    public final Parameter<Boolean> applyToOriginalReads = addParameter(new BoolParameterBuilder("apply-to-original")
            .withDefaultValue(true)
            .withDescription("if set applies fixes to original not truncated reads")
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

    public final KmerStatisticsGatherer gatherer = new KmerStatisticsGatherer();
    {
        setFix(gatherer.inputFiles, truncater.truncatedReadsOut);
        setFixDefault(gatherer.maxSize);
        setFix(gatherer.k, k);
        setFixDefault(gatherer.outputPrefixesFile);
        setFixDefault(gatherer.outputDir);
        addSubTool(gatherer);
    }

    public final CleanAll cleanAll = new CleanAll();
    {
        setFix(cleanAll.prefixesFile, gatherer.outputPrefixesFile);
        setFix(cleanAll.k, k);
        setFix(cleanAll.kmersDir, gatherer.outputDir);
        setFix(cleanAll.maximalIndelsNumber, maximalIndelsNumber);
        setFix(cleanAll.maximalSubsNumber, maximalSubsNumber);
        setFix(cleanAll.badKmersNumber, gatherer.badKmersNumberOut);
        setFixDefault(cleanAll.outputDir);
        addSubTool(cleanAll);
    }

    public FixesApplier fixesApplier = new FixesApplier();
    {
        setFix(fixesApplier.fixes, cleanAll.fixesOut);
        setFix(fixesApplier.k, k);
        setFix(fixesApplier.reads,
                new IfYielder<File[]>(
                        applyToOriginalReads,
                        converter.convertedReadsOut,
                        truncater.truncatedReadsOut
                ));
        setFix(fixesApplier.outputDir, outputDir);
        setFix(fixesApplier.readsNumber, gatherer.readsNumberOut);
        addSubTool(fixesApplier);
    }


    // output parameters
    public final InValue<File[]> resultingReadsOut = addOutput("resulting-reads", fixesApplier.resultingReadsOut, File[].class);



    @Override
    protected void runImpl() throws ExecutionFailedException {
        info("K-mer size = " + k.get());

        addStep(converter);
        addStep(truncater);
        addStep(gatherer);
        addStep(cleanAll);
        addStep(fixesApplier);
    }

    @Override
    protected void cleanImpl() {
    }
    
    @Override
    public String getStageName(int curLang) {
        return (curLang == 0) ? "Error correcting" : "Исправление ошибок";
    }

    public SimpleErrorCorrector() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new SimpleErrorCorrector().mainImpl(args);
    }

}
