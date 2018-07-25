package ru.ifmo.genetics.tools;

import ru.ifmo.genetics.tools.converters.Thinner;
import ru.ifmo.genetics.tools.ec.simple.SimpleErrorCorrector;
import ru.ifmo.genetics.tools.olc.ContigsAssembler;
import ru.ifmo.genetics.tools.microassembly.Microassembly;
import ru.ifmo.genetics.tools.rf.QuasicontigsAssembler;
import ru.ifmo.genetics.utils.NumUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.BoolParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.FilesFromOneFileYielder;

import java.io.File;

public class Assembler extends Tool {
    public static final String NAME = "assembler";
    public static final String DESCRIPTION = "assembles contigs from paired-end reads with indels";


    // input params
    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("input-files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("reads to process")
            .create());

    public final Parameter<Integer> kParameter = addParameter(new IntParameterBuilder("k-mer")
            .withShortOpt("k")
            .withDefaultValue(21)
            .withDescriptionShort("k-mer size")
            .withDescription("k-mer size (used in de Bruijn graph, quasicontigs assembler)")
            .withDescriptionRuShort("Длина k-мера")
            .withDescriptionRu("Длина k-мера (используется в графе де Брёйна при сборке)")
            .create());

    public final Parameter<File> contigsFile = addParameter(new FileParameterBuilder("contigs-file")
            .optional()
            .withShortOpt("o")
            .withDefaultValue(workDir.append("contigs.fasta"))
            .withDescription("file with assembled contigs")
            .create());

    public final Parameter<Boolean> runMicroassembly = addParameter(new BoolParameterBuilder("run-microassembly")
            .optional()
            .withShortOpt("M")
            .withDefaultValue(false)
            .withDescriptionShort("Run microassembly")
            .withDescription("run microassembly (bowtie and hadoop are required)")
            .withDescriptionRuShort("Запустить микросборку")
            .withDescriptionRu("Запустить микросборку после сборки контигов (использует bowtie и hadoop)")
            .create());

    public final Parameter<Boolean> withoutEC = addParameter(new BoolParameterBuilder("without-error-correction")
            .optional()
            .withDefaultValue(false)
            .withDescriptionShort("Without error correction")
            .withDescription("run assembly without error correction")
            .withDescriptionRuShort("Без исправления ошибок")
            .withDescriptionRu("Запустить сборку без исправления ошибок")
            .create());



    // steps
    public final SimpleErrorCorrector errorCorrector = new SimpleErrorCorrector();
    {
        setFix(errorCorrector.inputFiles, inputFiles);
        setFix(errorCorrector.k, kParameter);
        setFixDefault(errorCorrector.outputDir);
        setFixDefault(errorCorrector.applyToOriginalReads);
        setFixDefault(errorCorrector.gatherer.ignoreBadKmers);
        setFixDefault(errorCorrector.gatherer.outputCounts);
        addSubTool(errorCorrector);
    }

    public final QuasicontigsAssembler quasicontigsAssembler = new QuasicontigsAssembler();
    {
        setFix(quasicontigsAssembler.kParameter, kParameter);
        setFix(quasicontigsAssembler.inputFiles, errorCorrector.resultingReadsOut);
        setFixDefault(quasicontigsAssembler.outputDir);
        setFix(quasicontigsAssembler.truncater.phredThreshold, errorCorrector.truncater.phredThreshold);
        setFix(quasicontigsAssembler.readsFiller.maxSumOutputLength, Thinner.maxReadsSizeValue * 2);
        addSubTool(quasicontigsAssembler);
    }

    public final ContigsAssembler contigsAssembler = new ContigsAssembler();
    {
        setFix(contigsAssembler.inputFiles, quasicontigsAssembler.resultingQuasicontigsOut);
        setFix(contigsAssembler.consensus.holesFile, (File) null);
        setFixDefault(contigsAssembler.thinner.maxReadsSize);
        setFixDefault(contigsAssembler.divider.bucketCharsNumberIn);
        setFixDefault(contigsAssembler.sorter.smallBucketCharsNumberIn);
        setFixDefault(contigsAssembler.contigsFile);
        addSubTool(contigsAssembler);
    }

    public final Microassembly microassembly = new Microassembly();
    {
        setFix(microassembly.originalContigsFiles, new FilesFromOneFileYielder(contigsAssembler.contigsFile));
        setFixDefault(microassembly.allContigsFile);
        setFix(microassembly.readsFiles, inputFiles);
        setFixDefault(microassembly.hdfsWorkDirPrefix);
        setFix(microassembly.contigsFile, contigsFile);
        setFix(microassembly.joinContigs.minLength, quasicontigsAssembler.readsFiller.minInsertSize);
        setFix(microassembly.joinContigs.maxLength, quasicontigsAssembler.readsFiller.maxInsertSize);
        setFixDefault(microassembly.preparing.qualityFormatIn);
        addSubTool(microassembly);
    }



    @Override
    protected void runImpl() throws ExecutionFailedException {
        double[] coef = new double[2 + (withoutEC.get()? 0 : 1) + (runMicroassembly.get()? 1 : 0)];
        int j = 0;

        if (withoutEC.get()) {
            setFix(quasicontigsAssembler.inputFiles, inputFiles);
        } else {
            addStep(errorCorrector);
            coef[j++] = 0.25;
        }

        addStep(quasicontigsAssembler);
        coef[j++] = 0.15;
        addStep(contigsAssembler);
        coef[j++] = 0.30;

        if (runMicroassembly.get()) {
            addStep(microassembly);
            coef[j++] = 0.30;
        } else {
            contigsAssembler.contigsFile.set(contigsFile);
        }


        progress.stepCoef = NumUtils.normalize(coef);
    }


    @Override
    protected void cleanImpl() {
    }

    public Assembler() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new Assembler().mainImpl(args);
    }

}
