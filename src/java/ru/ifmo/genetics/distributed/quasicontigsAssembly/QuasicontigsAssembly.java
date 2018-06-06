package ru.ifmo.genetics.distributed.quasicontigsAssembly;


import org.apache.hadoop.fs.Path;
import ru.ifmo.genetics.distributed.clusterization.bipartite.MakeTextOutputTask;
import ru.ifmo.genetics.distributed.clusterization.bipartite.Runner;
import ru.ifmo.genetics.distributed.clusterization.bipartite.BfsCleanerTask;
import ru.ifmo.genetics.distributed.clusterization.tasks.*;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.PairedDnaQWithIdWritable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.quasicontigsAssembly.task.ReadsFillerTask;
import ru.ifmo.genetics.distributed.util.JobUtils;

import java.io.IOException;

/**
 * Author: Sergey Melnikov
 */
public class QuasicontigsAssembly {


    public static void main(String[] args) throws Exception {
        final Path sourceFolder = new Path(args[0]);
        final Path workDir = new Path(args[1]);
        makeClusterizationAndReadsFilling(sourceFolder, workDir);
    }

    /**
     * @param sourceFolder directory with <Int128WritableComparable, PairedDnaQWritable>
     * @param workDir      some directory
     * @return directory with <Int128WritableComparable, DnaWritable>
     */
    public static Path makeClusterizationAndReadsFilling(Path sourceFolder, Path workDir) throws IOException {
        Path componentsWithReads = makeClusterization(sourceFolder, workDir);
//        Path componentsWithReads = Runner.makeClusterization(sourceFolder, workDir);

        Path nonUniqueResult = new Path(workDir, "8_non_unique");
        if (!JobUtils.jobSucceededOrRemove(nonUniqueResult)) {
            ReadsFillerTask.fillReads(componentsWithReads, nonUniqueResult);
        }
        Path result = nonUniqueResult;
//        Path result = new Path(workDir, "8_result");
//        if (!JobUtils.jobSucceededOrRemove(result)) {
//            ReadsFillerTask.makeFilledReadsUniqueAndDropPart(nonUniqueResult, result, 1.0);
//        }
        Path textResult = new Path(workDir, "8_result_text");
        if (!JobUtils.jobSucceededOrRemove(textResult)) {
            MakeTextOutputTask.makeTextOutput(result, textResult, Int128WritableComparable.class, DnaWritable.class);
        }
        return result;
    }

    /**
     * @param sourceFolder directory with <Int128WritableComparable, PairedDnaQWritable>
     * @param workDir
     * @return directory with <ComponentID, PairedDnaQWithIdWritable>
     */
    private static Path makeClusterization(Path sourceFolder, Path workDir) throws IOException {
        final Path reverseIndex = new Path(workDir, "1_reverseIndex");
        final Path undirectedEdges = new Path(workDir, "2_undirectedEdges");
        final Path edges = new Path(workDir, "3_edges");
        Path components = new Path(workDir, "4_components_0");

        if (!JobUtils.jobSucceededOrRemove(components)) {
            InitComponentsTask.initComponents(sourceFolder, components, 0.05);
        }

        if (!JobUtils.jobSucceededOrRemove(reverseIndex)) {
            ReverseIndexTask.buildReverseIndex(sourceFolder, reverseIndex, 17, 5, 30);
        }
        //Converter.convert(reverseIndex, new Path(workDir, "1_reverseIndex_text"), LongWritable.class, LongArrayWritable.class);
        if (!JobUtils.jobSucceededOrRemove(undirectedEdges)) {
            EdgesBuilderTask.buildEdges(reverseIndex, undirectedEdges, 1);
        }
        if (!JobUtils.jobSucceededOrRemove(edges)) {
            EdgesBuilderTask.convertEdgesToDirect(undirectedEdges, edges);
        }

        //Converter.convert(edges, new Path(workDir, "3_edges_text"), Vertex.class, ComponentIdOrEdge.class);
        for (int i = 0; i < 6; i++) {
            final Path newComponents = new Path(workDir, "5_components_" + i);
            if (!JobUtils.jobSucceededOrRemove(newComponents)) {
                BfsTurnTask.makeBfsTurn(edges, components, newComponents);
            }
            if (i >= 3) {
                final Path newComponentsClean = new Path(workDir, "5_components_" + i + "_clean");
                if (!JobUtils.jobSucceededOrRemove(newComponentsClean)) {
                    // BfsCleanerTask.cleanBfsData(newComponents, newComponentsClean, 10000);
                    // did you mean dropLargeComponents?
                    BfsCleanerTask.dropLargeComponents(newComponents, newComponentsClean, 10000);
                }
                components = newComponentsClean;
            } else {
                components = newComponents;


//            if (i % 4 == 3) {
//                Path componentsWithReads = new Path(workDir, "5_components_" + i + "_withReads");
//                if (!JobUtils.jobSucceededOrRemove(componentsWithReads)) {
//                    Path tmpFolder = new Path(workDir, "5_" + i + "_tmp");
//                    JobUtils.remove(tmpFolder);
//                    ExtractComponentTaskPairedDnaQ.extractComponent(sourceFolder, components, tmpFolder, componentsWithReads);
//                }
//
//                Path textComponentsWithReads = new Path(workDir, "5_components_" + i + "_withReads_text");
//                if (!JobUtils.jobSucceededOrRemove(textComponentsWithReads)) {
//                    MakeTextOutputTask.makeTextOutput(componentsWithReads, textComponentsWithReads, ComponentID.class, PairedDnaQWithIdWritable.class);
//                }
            }
        }
        final Path textComponents = new Path(workDir, "6_textComponents");
        if (!JobUtils.jobSucceededOrRemove(textComponents)) {
            ComponentsTextOutputTask.makeTextOutput(components, textComponents);
        }
        //ComponentsStatisticTask.countStatistics(components, new Path(workDir, "7_statistics"));

        Path componentsWithReads = new Path(workDir, "7_components");
        if (!JobUtils.jobSucceededOrRemove(componentsWithReads)) {
            Path tmpFolder = new Path(workDir, "7_tmp");
            JobUtils.remove(tmpFolder);
            ExtractComponentTaskPairedDnaQ.extractComponent(sourceFolder, components, tmpFolder, componentsWithReads);
        }
        Path textComponentsWithReads = new Path(workDir, "7_components_text");
        if (!JobUtils.jobSucceededOrRemove(textComponentsWithReads)) {
            MakeTextOutputTask.makeTextOutput(componentsWithReads, textComponentsWithReads, ComponentID.class, PairedDnaQWithIdWritable.class);
        }
        return componentsWithReads;
    }

    // Map = nothing
    // Vertex -> List<Id>|DirectEdge
}

