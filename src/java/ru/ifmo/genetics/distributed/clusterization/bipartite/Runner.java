package ru.ifmo.genetics.distributed.clusterization.bipartite;

import org.apache.hadoop.fs.Path;
import ru.ifmo.genetics.distributed.clusterization.types.VertexOrKmerOrComponentIDWritable;
import ru.ifmo.genetics.distributed.clusterization.types.VertexOrKmerWritableComparable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.util.JobUtils;

import java.io.IOException;


/**
 * Author: Sergey Melnikov
 */
public class Runner {
    private Runner() {
    }


    public static void main(String[] args) throws Exception {
        final Path reads = new Path(args[0]);
        final Path workDir = new Path(args[1]);
        final Path quasiContigs = new Path(workDir, "quasiContigs");
        Path path = makeClusterization(reads, new Path(workDir, "quasiContigsWork"));
        final Path contigs = new Path(workDir, "quasiContigs");
        makeContigs(quasiContigs, new Path(workDir, "contigsWork"), contigs);
    }

    /**
     * @param source  directory with <Int128WritableComparable, DnaWritable>
     * @param workDir directory with other directories
     * @param result  directory with <Int128WritableComparable, DnaWritable>
     */
    private static void makeContigs(Path source, Path workDir, Path result) throws IOException {
        final Path index = new Path(workDir, "1_index");
        final Path reverseIndex = new Path(workDir, "1_reverseIndex");
        Path components = new Path(workDir, "2_initialComponents");
//
        IndexesTask.makeIndex(source, index, 15);
        IndexesTask.makeReverseIndex(source, reverseIndex, 15, 0, 100);
        InitComponentsTask.initComponents(source, components, 0.1);


        for (int i = 0; i < 8; i++) {
            final Path newComponents = new Path(workDir, "3_components_" + i);
            BfsTask.bfsTurn(index, reverseIndex, components, newComponents);
            MakeTextOutputTask.makeTextOutput(newComponents,
                    new Path(workDir, "3_components_" + i + "_text"),
                    VertexOrKmerWritableComparable.class,
                    VertexOrKmerOrComponentIDWritable.class);
            components = newComponents;
        }
        final Path textComponents = new Path(workDir, "4_textComponents");
        MakeTextOutputTask.makeTextOutput(components, textComponents, VertexOrKmerWritableComparable.class,
                VertexOrKmerOrComponentIDWritable.class);
        final Path readsByComponents = new Path(workDir, "5_readsByComponents");
        ExtractReadsForComponentsTask.extractReadsForComponentsTask(source, components, readsByComponents,
                DnaWritable.class);


        //TODO run contigs assembler
    }

    /**
     * @param reads   directory with <Int128WritableComparable, PairedDnaQWritable>
     * @param workDir directory with other directories
     * @returns directory with <ComponentID, PairedDnaQWithIdWritable>
     */
    public static Path makeClusterization(Path reads, Path workDir) throws IOException {
        final int K = 17;
        final Path index = new Path(workDir, "1_index");
        if (!JobUtils.jobSucceededOrRemove(index)) {
            IndexesTask.makeIndex(reads, index, K);
        }
        final Path reverseIndex = new Path(workDir, "1_reverseIndex");
        if (!JobUtils.jobSucceededOrRemove(reverseIndex)) {
            IndexesTask.makeReverseIndex(reads, reverseIndex, K, 5, 30);
        }
        Path components = new Path(workDir, "2_initialComponents");
        if (!JobUtils.jobSucceededOrRemove(components)) {
            InitComponentsTask.initComponents(reads, components, 0.05);
        }


        for (int i = 0; i < 12; i++) {
            final Path newComponents = new Path(workDir, "3_components_" + i);
            if (!JobUtils.jobSucceededOrRemove(newComponents)) {
                BfsTask.bfsTurn(index, reverseIndex, components, newComponents);
            }
            final Path smallComponents = new Path(workDir, "3_components_" + i + "_small");
            if (!JobUtils.jobSucceededOrRemove(smallComponents)) {
                BfsCleanerTask.dropLargeComponents(newComponents, smallComponents, 10000);
            }
            components = smallComponents;
        }
        final Path readsByComponents = new Path(workDir, "5_readsByComponents");
        if (!JobUtils.jobSucceededOrRemove(readsByComponents)) {
            ExtractPairedDnaQReadsForComponentsTask.extractPairedDnaQReadsForComponentsTask(reads, components,
                    readsByComponents);
        }
//        MakeTextOutputTask.makeTextOutput(readsByComponents, new Path(workDir, "5_readsByComponents_text"), ComponentID.class, PairedDnaQWithIdWritable.class);
        return readsByComponents;
    }


}
