package ru.ifmo.genetics.tools.irf;

import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.structures.debriujn.WeightedDeBruijnGraph;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.NumUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;

import java.io.*;

public class GraphBuilder extends Tool {
    public static final String NAME = "WDBGraph-builder";
    public static final String DESCRIPTION = "builds weighted De Bruijn graph from a set of reads";

    // input params
    public final Parameter<File[]> kmersFiles = addParameter(new FileMVParameterBuilder("kmers-files")
            .mandatory()
            .withDescription("files with (k+1)-mers to add")
            .create());

    public final Parameter<Integer> kParameter = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size (vertex, not edge)")
            .create());

    public final Parameter<File> graphFile = addParameter(new FileParameterBuilder("graph-file")
            .withShortOpt("g")
            .withDescription("file to output graph at")
            .withDefaultValue(workDir.append("graph"))
            .create());

    public final Parameter<Integer> minWeightToReallyAdd = addParameter(new IntParameterBuilder("min-weight")
            .optional()
            .withDefaultValue(1)
            .withDescription("minimal weight of k-mer to be counted as added")
            .create());

    // internal vars
    private int k;
    private WeightedDeBruijnGraph graph;

    @Override
    protected void runImpl() throws ExecutionFailedException {
        k = kParameter.get();
        Timer timer = new Timer();
        info("Building graph...");
        try {
            buildGraph();
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
        info("Building graph done, it took " + timer);

        timer.start();

        info("Dumping...");
        try {
            FileOutputStream fos = new FileOutputStream(graphFile.get());
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos));
            graph.write(dos);
            dos.close();
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
        info("Done, it took " + timer);
    }

    private void buildGraph() throws IOException, ExecutionFailedException {
        long totalToRead = 0;
        for (File kmersFile : kmersFiles.get()) {
            FileInputStream fileIn = new FileInputStream(kmersFile);
            long toRead = fileIn.getChannel().size() / 12;
            totalToRead += toRead;
            fileIn.close();
        }

        debug("have to read " + totalToRead + " k-mers");

        long graphSizeBytes = Math.min(totalToRead * 18, (long)(Misc.availableMemory() * 0.85));
        debug("Graph size = " + graphSizeBytes + " bytes");

        graph = new WeightedDeBruijnGraph(k, graphSizeBytes, minWeightToReallyAdd.get());

        progress.setTotalTasks(totalToRead);
        long done = 0;
        progress.createProgressBar();
        for (File kmersFile : kmersFiles.get()) {
            FileInputStream fileIn = new FileInputStream(kmersFile);
            DataInputStream in = new DataInputStream(new BufferedInputStream(fileIn));
            long toRead = fileIn.getChannel().size() / 12;
            for (long i = 0; i < toRead; ++i) {
                long kmerHash = in.readLong();
                int weight = in.readInt();
                graph.putEdge(kmerHash, weight);
            }
            fileIn.close();

            done += toRead;
            progress.updateDoneTasksGreatly(done);
        }
        progress.destroyProgressBar();
        info("Graph size: " + NumUtils.groupDigits(graph.edgesSize()) + " edges");
    }

    @Override
    protected void cleanImpl() {
        graph = null;

    }

    public GraphBuilder() {
        super(NAME, DESCRIPTION);
    }

    // ----------------------------------------------------------------------------------------------------------------
    public static void main(String[] args) {
        new GraphBuilder().mainImpl(args);
    }
 }
