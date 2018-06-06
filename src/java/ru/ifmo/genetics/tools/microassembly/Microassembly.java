package ru.ifmo.genetics.tools.microassembly;

import ru.ifmo.genetics.tools.olc.AssemblyStatistics;
import ru.ifmo.genetics.tools.olc.layouter.ConsensusMaker;
import ru.ifmo.genetics.tools.olc.layouter.Layouter;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.StringParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.StringUnionYielder;

import java.io.File;

public class Microassembly extends Tool {
    public static final String NAME = "microassembly";
    public static final String DESCRIPTION = "runs microassembly";


    // input parameters
    public final Parameter<File[]> originalContigsFiles = addParameter(new FileMVParameterBuilder("original-contigs-files")
            .mandatory()
            .withShortOpt("C")
            .withDescription("files with original contigs")
            .create());

    public final Parameter<File> allContigsFile = addParameter(new FileParameterBuilder("all-contigs-files")
            .withDefaultValue(workDir.append("contigs-all.fasta"))
            .withDescription("files with merged contigs")
            .create());

    public final Parameter<File[]> readsFiles = addParameter(new FileMVParameterBuilder("reads-files")
            .mandatory()
            .withShortOpt("r")
            .withDescription("reads files")
            .create());


    public final Parameter<String> hdfsWorkDirPrefix = addParameter(new StringParameterBuilder("hdfs-work-dir-prefix")
            .withShortOpt("H")
            .withDefaultValue("workDir")
            .withDescription("work directory prefix in HDFS")
            .create());

    public final Parameter<File> contigsFile = addParameter(new FileParameterBuilder("contigs-file")
            .optional()
            .withDefaultValue(workDir.append("contigs.fasta"))
            .withDescription("file with resulting contigs")
            .create());


    // internal variables
    public FastaFilesMerger contigsMerger = new FastaFilesMerger();
    {
        setFix(contigsMerger.fastaFiles, originalContigsFiles);
        setFix(contigsMerger.resultingFile, allContigsFile);
        addSubTool(contigsMerger);
    }

    public BowtieIndexBuilder bowtieIndexBuilder = new BowtieIndexBuilder();
    {
        setFix(bowtieIndexBuilder.allContigsFile, allContigsFile);
        setFixDefault(bowtieIndexBuilder.resultingIndexFile);
        addSubTool(bowtieIndexBuilder);
    }

    public ReadsMapper readsMapper = new ReadsMapper();
    {
        setFix(readsMapper.indexFile, bowtieIndexBuilder.resultingIndexFile);
        setFix(readsMapper.readsFiles, readsFiles);
        setFixDefault(readsMapper.resultingMapDir);
        addSubTool(readsMapper);
    }

    public PreparingToHadoopJoinContigs preparing = new PreparingToHadoopJoinContigs();
    {
        setFix(preparing.readsFiles, readsFiles);
        setFix(preparing.allContigsFile, allContigsFile);
        setFix(preparing.alignsDir, readsMapper.resultingMapDir);
        setFix(preparing.hdfsWorkDirPrefix, hdfsWorkDirPrefix);
        setFixDefault(preparing.hdfsReadsDir);
        setFixDefault(preparing.hdfsAlignsDir);
        setFixDefault(preparing.hdfsContigsDir);
        setFixDefault(preparing.hdfsRealWorkDir);
        addSubTool(preparing);
    }

    public HadoopJoinContigsRunner joinContigs = new HadoopJoinContigsRunner();
    {
        setFix(joinContigs.hdfsWorkDir, preparing.hdfsRealWorkDir);
        setFix(joinContigs.hdfsReadsDir, preparing.hdfsReadsDir);
        setFix(joinContigs.hdfsAlignsDir, preparing.hdfsAlignsDir);
        setFix(joinContigs.hdfsContigsDir, preparing.hdfsContigsDir);
        setFix(joinContigs.qualityFormat, preparing.qualityFormatOut);
        setFix(joinContigs.trimming, readsMapper.trimming);
        addSubTool(joinContigs);
    }
    
    public PostProcessingAfterHadoopJoinContigs postprocessing = new PostProcessingAfterHadoopJoinContigs();
    {
        setFix(postprocessing.hdfsHolesDir, new StringUnionYielder(joinContigs.hdfsWorkDir, "/" + HadoopJoinContigs.FILLED_HOLES_DIR));
        setFix(postprocessing.holesDir, workDir.append("filled_holes"));
        addSubTool(postprocessing);
    }

    public HolesToOverlapsConverter converter = new HolesToOverlapsConverter();
    {
        setFix(converter.holesDir, postprocessing.holesDir);
        setFix(converter.allContigsFile, allContigsFile);
        setFixDefault(converter.resultingHolesFile);
        setFixDefault(converter.resultingOverlapsFile);
        addSubTool(converter);
    }

    public Layouter layouter = new Layouter();
    {
        setFix(layouter.readsFile, allContigsFile);
        setFix(layouter.overlapsFile, converter.resultingOverlapsFile);
        setFixDefault(layouter.layoutFile);
        setFixDefault(layouter.mergeLength);
        setFix(layouter.tipsDepth, -1);
        setFixDefault(layouter.readsNumberParameter);
        setFix(layouter.finalOverlapsFile, layouter.workDir.append("overlaps.final"));
        addSubTool(layouter);
    }

    public ConsensusMaker consensus = new ConsensusMaker();
    {
        setFix(consensus.readsFile, layouter.readsFile);
        setFix(consensus.layoutFile, layouter.layoutFile);
        setFix(consensus.contigsFile, contigsFile);
        setFix(consensus.minReadsInContig, 1);
        setFix(consensus.holesFile, converter.resultingHolesFile);
        addSubTool(consensus);
    }

    public AssemblyStatistics statistics = new AssemblyStatistics();
    {
        setFix(statistics.readsFile, contigsFile);
        addSubTool(statistics);
    }

    // output parameters


    @Override
    protected void runImpl() throws ExecutionFailedException {
        addStep(contigsMerger);
        addStep(bowtieIndexBuilder);
        addStep(readsMapper);
        addStep(preparing);
        addStep(joinContigs);
        addStep(postprocessing);
        addStep(converter);
        addStep(layouter);
        addStep(consensus);
        addStep(statistics);

        progress.stepCoef = new double[]{0, 0, 0.10, 0.05, 0.85, 0, 0, 0, 0, 0};
    }



    @Override
    protected void cleanImpl() {
    }

    @Override
    public String getStageName(int curLang) {
        return (curLang == 0) ? "Microassembly" : "Микросборка";
    }

    public Microassembly() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new Microassembly().mainImpl(args);
    }
}
