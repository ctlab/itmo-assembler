package ru.ifmo.genetics.distributed.contigsAssembly;


import org.apache.hadoop.fs.Path;
import ru.ifmo.genetics.distributed.clusterization.bipartite.MakeTextOutputTask;
import ru.ifmo.genetics.distributed.clusterization.tasks.*;
import ru.ifmo.genetics.distributed.contigsAssembly.tasks.ContigsAssemblyTask;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.quasicontigsAssembly.task.ReadsFillerTask;
import ru.ifmo.genetics.distributed.util.JobUtils;

import java.io.IOException;

/**
 * Author: Sergey Melnikov
 */
public class ContigsAssembly {


    public static void main(String[] args) throws Exception {
        final Path sourceFolder = new Path(args[0]);
        final Path workDir = new Path(args[1]);
        makeContigs(sourceFolder, workDir);
    }

    public static Path makeContigs(Path sourceFolder, Path workDir) throws IOException {
        final Path reverseIndex = new Path(workDir, "1_reverseIndex");
        final Path undirectedEdges = new Path(workDir, "2_undirectedEdges");
        final Path edges = new Path(workDir, "3_edges");
        Path components = new Path(workDir, "4_components_0");

        if (!JobUtils.jobSucceededOrRemove(components)) {
            InitComponentsTask.initComponents(sourceFolder, components, 0.3);
        }

        if (!JobUtils.jobSucceededOrRemove(reverseIndex)) {
            ReverseIndexTask.buildReverseIndex(sourceFolder, reverseIndex, 19, 2, 25);
        }
        //Converter.convert(reverseIndex, new Path(workDir, "1_reverseIndex_text"), LongWritable.class, LongArrayWritable.class);
        if (!JobUtils.jobSucceededOrRemove(undirectedEdges)) {
            EdgesBuilderTask.buildEdges(reverseIndex, undirectedEdges, 1);
        }
        if (!JobUtils.jobSucceededOrRemove(edges)) {
            EdgesBuilderTask.convertEdgesToDirect(undirectedEdges, edges);
        }
        //Converter.convert(edges, new Path(workDir, "3_edges_text"), Vertex.class, ComponentIdOrEdge.class);
        for (int i = 0; i < 2; i++) {
            final Path newComponents = new Path(workDir, "5_components_" + i);
            if (!JobUtils.jobSucceededOrRemove(newComponents)) {
                BfsTurnTask.makeBfsTurn(edges, components, newComponents);
            }
//            if (i % 5 == 0) {
//                Path textComponents = new Path(workDir, "5_components_" + i + "_text");
//                if (!JobUtils.jobSucceededOrRemove(textComponents)) {
//                    ComponentsTextOutputTask.makeTextOutput(newComponents, textComponents);
//                }
//            }
            components = newComponents;
        }
        final Path textComponents = new Path(workDir, "6_textComponents");
        if (!JobUtils.jobSucceededOrRemove(textComponents)) {
            ComponentsTextOutputTask.makeTextOutput(components, textComponents);
        }
//        final Path statistics = new Path(workDir, "7_statistics");
//        if (!JobUtils.jobSucceededOrRemove(statistics)) {
//            ComponentsStatisticTask.countStatistics(components, statistics);
//        }
        Path componentsWithQuasicontigs = new Path(workDir, "7_components");
        if (!JobUtils.jobSucceededOrRemove(componentsWithQuasicontigs)) {
            ExtractComponentTaskDna.extractComponent(sourceFolder, components, componentsWithQuasicontigs);
        }


        Path resultNonUnique = new Path(workDir, "8_result_nonUnique");
        if (!JobUtils.jobSucceededOrRemove(resultNonUnique)) {
            ContigsAssemblyTask.assembleContigs(componentsWithQuasicontigs, resultNonUnique);
        }
        Path result = new Path(workDir, "8_result");
        if (!JobUtils.jobSucceededOrRemove(result)) {
            ReadsFillerTask.makeFilledReadsUniqueAndDropPart(resultNonUnique, result, 1.0);
        }


        Path textResult = new Path(workDir, "8_result_text");
        if (!JobUtils.jobSucceededOrRemove(textResult)) {
            MakeTextOutputTask.makeTextOutput(result, textResult, Int128WritableComparable.class, DnaWritable.class);
        }


        return result;
    }

}