package ru.ifmo.genetics.tools.olc;

import ru.ifmo.genetics.tools.converters.Thinner;
import ru.ifmo.genetics.tools.olc.gluedDnasString.DnaStringGluer;
import ru.ifmo.genetics.tools.olc.layouter.ConsensusMaker;
import ru.ifmo.genetics.tools.olc.layouter.Layouter;
import ru.ifmo.genetics.tools.olc.optimizer.CoveredReadsRemover;
import ru.ifmo.genetics.tools.olc.optimizer.OverlapsOptimizer;
import ru.ifmo.genetics.tools.olc.optimizer.OverlapsSlicer;
import ru.ifmo.genetics.tools.olc.overlapper.Overlapper;
import ru.ifmo.genetics.tools.olc.suffixArray.BucketsDivider;
import ru.ifmo.genetics.tools.olc.suffixArray.BucketsSorter;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.FilesFromOneFileYielder;
import ru.ifmo.genetics.utils.tool.values.ListFilesYielder;

import java.io.File;

public class ContigsAssembler extends Tool {
    public static final String NAME = "contigs-assembler";
    public static final String DESCRIPTION = "assembles contigs from quasicontigs";


    // input params
    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("input-files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("input files with quasi-contigs")
            .create());

    public final Parameter<File> contigsFile = addParameter(new FileParameterBuilder("contigs-file")
            .optional()
            .withDefaultValue(workDir.append("contigs.fasta"))
            .withDescription("file with assembled contigs")
            .create());



    // tools
    public Thinner thinner = new Thinner();
    {
        setFix(thinner.inputFiles, inputFiles);
        setFixDefault(thinner.outputFile);
        addSubTool(thinner);
    }

    public DnaStringGluer gluer = new DnaStringGluer();
    {
        setFix(gluer.readsFile, thinner.outputFile);
        addSubTool(gluer);
    }

    public BucketsDivider divider = new BucketsDivider();
    {
        setFix(divider.fullStringFile, gluer.fullStringFile);
        setFixDefault(divider.bucketsDir);
        addSubTool(divider);
    }

    public BucketsSorter sorter = new BucketsSorter();
    {
        setFix(sorter.fullStringFile, gluer.fullStringFile);
        setFix(sorter.bucketsDir, divider.bucketsDir);
        setFix(sorter.bucketCharsNumberIn, divider.bucketCharsNumberOut);
        setFixDefault(sorter.sortedBucketsDir);
        addSubTool(sorter);
    }

    public Overlapper overlapper = new Overlapper();
    {
        setFix(overlapper.fullStringFile, gluer.fullStringFile);
        setFix(overlapper.sortedBucketsDir, sorter.sortedBucketsDir);
        setFix(overlapper.bucketCharsNumberIn, divider.bucketCharsNumberOut);
        setFix(overlapper.bucketsNumberIn, divider.bucketsNumberOut);
        setFixDefault(overlapper.overlapsDir);
        addSubTool(overlapper);
    }

    public CoveredReadsRemover remover = new CoveredReadsRemover();
    {
        setFix(remover.readsFile, thinner.outputFile);
        setFix(remover.overlapsFiles, new ListFilesYielder(overlapper.overlapsDir));
        setFixDefault(remover.outOverlapsFile);
        addSubTool(remover);
    }

    public OverlapsSlicer slicer = new OverlapsSlicer();
    {
        setFix(slicer.readsFile, thinner.outputFile);
        setFix(slicer.overlapsFiles, new FilesFromOneFileYielder(remover.outOverlapsFile));
        setFixDefault(slicer.outReadsFile);
        setFixDefault(slicer.outOverlapsFile);
        addSubTool(slicer);
    }

    public OverlapsOptimizer optimizer = new OverlapsOptimizer();
    {
        setFix(optimizer.readsFile, slicer.outReadsFile);
        setFix(optimizer.overlapsFile, slicer.outOverlapsFile);
        setFixDefault(optimizer.optimizedOverlapsFile);
        addSubTool(optimizer);
    }

    public Layouter layouter = new Layouter();
    {
        setFix(layouter.readsFile, optimizer.readsFile);
        setFix(layouter.overlapsFile, optimizer.optimizedOverlapsFile);
        setFixDefault(layouter.layoutFile);
        setFixDefault(layouter.readsNumberParameter);
        setFix(layouter.finalOverlapsFile, layouter.workDir.append("overlaps.final"));
        addSubTool(layouter);
    }
    
    public ConsensusMaker consensus = new ConsensusMaker();
    {
        setFix(consensus.readsFile, layouter.readsFile);
        setFix(consensus.layoutFile, layouter.layoutFile);
        setFix(consensus.contigsFile, contigsFile);
        addSubTool(consensus);
    }

    public AssemblyStatistics statistics = new AssemblyStatistics();
    {
        setFix(statistics.readsFile, contigsFile);
        addSubTool(statistics);
    }


    @Override
    protected void runImpl() throws ExecutionFailedException {
        addStep(thinner);
        addStep(gluer);
        addStep(divider);
        addStep(sorter);
        addStep(overlapper);
        addStep(remover);
        addStep(slicer);
        addStep(optimizer);
        addStep(layouter);
        addStep(consensus);
        addStep(statistics);
        
        progress.stepCoef = new double[]{0.05, 0.01, 0.05, 0.35, 0.35, 0.1, 0.04, 0.04, 0.01, 0, 0};
    }


    @Override
    protected void cleanImpl() {
    }

    @Override
    public String getStageName(int curLang) {
        return (curLang == 0) ? "Contigs assembly" : "Сборка контигов";
    }

    public ContigsAssembler() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new ContigsAssembler().mainImpl(args);
    }
}
