package ru.ifmo.genetics.distributed.clusterization.bipartite;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import ru.ifmo.genetics.distributed.clusterization.types.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Author: Sergey Melnikov
 */
public class BfsTask {
    private BfsTask() {
    }

    /**
     * @param index         directory with logical type <Vertex, Kmer>
     *                      and physical type <VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable>
     * @param reverseIndex  directory with logical type <Kmer, Vertex>
     *                      and physical type <VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable>
     * @param inComponents  directory with logical type <Vertex|Kmer, ComponentID>
     *                      and physical type <VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable>
     * @param outComponents directory with logical type <Vertex|Kmer, ComponentID>
     *                      and physical type <VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable>
     */

    public static void bfsTurn(Path index, Path reverseIndex, Path inComponents,
                               Path outComponents) throws IOException {
        JobConf conf = new JobConf(IndexesTask.class);

        conf.setJobName("make BFS turn");

        conf.setOutputKeyClass(VertexOrKmerWritableComparable.class);
        conf.setOutputValueClass(VertexOrKmerOrComponentIDWritable.class);

        conf.setMapperClass(IdentityMapper.class);
        conf.setReducerClass(BfsTurnReduce.class);


        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(conf, index, reverseIndex, inComponents);
        FileOutputFormat.setOutputPath(conf, outComponents);

        JobClient.runJob(conf);
    }

    private static class BfsTurnReduce extends MapReduceBase implements
            Reducer<VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable,
                    VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable> {

        VertexOrKmerWritableComparable outKey = new VertexOrKmerWritableComparable();
        VertexOrKmerOrComponentIDWritable outValue = new VertexOrKmerOrComponentIDWritable();

        Set<Vertex> vertexes = new HashSet<Vertex>();
        Set<Kmer> kmers = new HashSet<Kmer>();
        Set<ComponentID> componentIDs = new HashSet<ComponentID>();

        @Override
        public void reduce(VertexOrKmerWritableComparable key, Iterator<VertexOrKmerOrComponentIDWritable> values,
                           OutputCollector<VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable> output,
                           Reporter reporter) throws IOException {
            // TODO optimize it! reuse different objects
            vertexes.clear();
            kmers.clear();

            componentIDs.clear();

            while (values.hasNext()) {
                VertexOrKmerOrComponentIDWritable value = values.next();
                if (value.isFirst()) {
                    vertexes.add(new Vertex(value.getFirst()));
                } else if (value.isSecond()) {
                    kmers.add(new Kmer(value.getSecond()));
                } else if (value.isThird()) {
                    componentIDs.add(new ComponentID(value.getThird()));
                }
            }

//            if (componentIDs.size() > 0) {
//                System.err.println(
//                        "key = " + key + ", vertexes.calculateSize() = " + vertexes.size() +
//                                ", kmers.calculateSize() = " + kmers.size() +
//                                ", componentIDs.calculateSize() = " + componentIDs.size());
//            }


            for (ComponentID componentID : componentIDs) {
                outValue.setThird(componentID);
                for (Kmer kmer : kmers) {
                    outKey.setSecond(kmer);
                    output.collect(outKey, outValue);
                }
                for (Vertex vertex : vertexes) {
                    outKey.setFirst(vertex);
                    output.collect(outKey, outValue);
                }
            }
        }
    }
}
