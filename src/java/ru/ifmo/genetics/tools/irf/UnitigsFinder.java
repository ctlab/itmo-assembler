package ru.ifmo.genetics.tools.irf;

import ru.ifmo.genetics.structures.debriujn.WeightedDeBruijnGraph;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;

import java.io.*;

public class UnitigsFinder extends Tool {
    private static final String DESCRIPTION = "pints unitigs in weighted de Bruijn graph";
    private static final String NAME = "unitigs-finder";

    public final Parameter<Integer> kParameter = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size (vertex, not edge)")
            .create());

    public final Parameter<File> graphFile = addParameter(new FileParameterBuilder("graph-file")
            .mandatory()
            .withShortOpt("g")
            .withDescription("file with weighted De Bruijn graph")
            .create());

    public UnitigsFinder() {
        super(NAME, DESCRIPTION);
    }

    private WeightedDeBruijnGraph graph;
    private int k;

    @Override
    public void cleanImpl() {
        graph = null;
    }

    @Override
    public void runImpl() throws ExecutionFailedException {
        k = kParameter.get();
        info("Loading graph...");
        try {
            FileInputStream fis = new FileInputStream(graphFile.get());
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
            graph = new WeightedDeBruijnGraph();
            graph.readFields(dis);
            dis.close();
        } catch (IOException e) {
            throw new ExecutionFailedException("Can't load graph", e);
        }

        info("Loading graph done");
    }

//    public static void main(String[] args) {
//        new UnitigsFinder().mainImpl(args);
//    }
}
