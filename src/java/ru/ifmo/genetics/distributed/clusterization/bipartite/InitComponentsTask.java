package ru.ifmo.genetics.distributed.clusterization.bipartite;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.Vertex;
import ru.ifmo.genetics.distributed.clusterization.types.VertexOrKmerOrComponentIDWritable;
import ru.ifmo.genetics.distributed.clusterization.types.VertexOrKmerWritableComparable;
import ru.ifmo.genetics.distributed.io.KmerIterable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;

import java.io.IOException;
import java.util.Random;

/**
 * Author: Sergey Melnikov
 */
public class InitComponentsTask {
    private InitComponentsTask() {
    }

    private static final String COMPONENTS_SELECT_PERCENT = "componentSelectPercent";
    private static final String DEFAULT_COMPONENTS_SELECT_PERCENT = "0.05";

    /**
     * @param reads                  directory with <Int128WritableComparable, A>
     * @param components             directory with logical type <Vertex, ComponentID>
     *                               and physical type <VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable>
     * @param componentSelectPercent probability to take read as component
     */
    public static void initComponents(Path reads, Path components, double componentSelectPercent) throws IOException {
        JobConf conf = new JobConf(IndexesTask.class);
        conf.set(COMPONENTS_SELECT_PERCENT, "" + componentSelectPercent);
        conf.setJobName("init component");

        conf.setOutputKeyClass(VertexOrKmerWritableComparable.class);
        conf.setOutputValueClass(VertexOrKmerOrComponentIDWritable.class);

        conf.setMapperClass(InitComponentsMap.class);
        conf.setReducerClass(IdentityReducer.class);


        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(conf, reads);
        FileOutputFormat.setOutputPath(conf, components);

        JobClient.runJob(conf);
    }

    private static class InitComponentsMap<A extends KmerIterable> extends MapReduceBase
            implements Mapper<Int128WritableComparable, A,
            VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable> {
        private double componentSelectPercent;
        private Vertex vertex = new Vertex();
        private ComponentID componentID= new ComponentID ();
        private VertexOrKmerWritableComparable outKey = new VertexOrKmerWritableComparable(vertex);
        private VertexOrKmerOrComponentIDWritable outValue = new VertexOrKmerOrComponentIDWritable(componentID);
        private Random random = new Random(1);

        @Override
        public void configure(JobConf job) {
            componentSelectPercent = Double.parseDouble(
                    job.get(COMPONENTS_SELECT_PERCENT, DEFAULT_COMPONENTS_SELECT_PERCENT));
        }

        @Override
        public void map(Int128WritableComparable key, A value,
                        OutputCollector<VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable> output,
                        Reporter reporter) throws IOException {
            if (random.nextDouble() < componentSelectPercent) {
                vertex.copyFieldsFrom(key);
                componentID.copyFieldsFrom(key);
                output.collect(outKey, outValue);
            }
        }
    }
}
