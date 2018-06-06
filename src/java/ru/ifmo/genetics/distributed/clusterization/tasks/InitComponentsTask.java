package ru.ifmo.genetics.distributed.clusterization.tasks;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentIdOrEdge;
import ru.ifmo.genetics.distributed.clusterization.types.Vertex;
import ru.ifmo.genetics.distributed.io.KmerIterable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;

import java.io.IOException;
import java.util.Random;

/**
 * Author: Sergey Melnikov
 */
public class InitComponentsTask {
    private static final String COMPONENTS_SELECT_PERCENT = "componentSelectPercent";
    private static final String DEFAULT_COMPONENTS_SELECT_PERCENT = "0.05";

    public static void initComponents(Path sourceFolder, Path componentsFolder, double percent) throws IOException {
        final JobConf conf = new JobConf(EdgesBuilderTask.class);

        conf.setJobName("Init components");
        conf.set(COMPONENTS_SELECT_PERCENT, "" + percent);

        FileInputFormat.setInputPaths(conf, sourceFolder);
        FileOutputFormat.setOutputPath(conf, componentsFolder);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setOutputKeyClass(Vertex.class);
        conf.setOutputValueClass(ComponentIdOrEdge.class);

        conf.setMapperClass(InitComponentsMapper.class);
        conf.setReducerClass(IdentityReducer.class);
        JobClient.runJob(conf);


    }

    public static class InitComponentsMapper<A extends KmerIterable> extends MapReduceBase implements Mapper<Int128WritableComparable, A, Vertex, ComponentIdOrEdge> {
        private final Vertex outKey = new Vertex();
        private final ComponentID outValueReal = new ComponentID();
        private final ComponentIdOrEdge outValue = new ComponentIdOrEdge(outValueReal);
        private final Random random = new Random(1);
        private double componentSelectPercent = Double.parseDouble(DEFAULT_COMPONENTS_SELECT_PERCENT);

        @Override
        public void configure(JobConf job) {
            componentSelectPercent = Double.parseDouble(job.get(COMPONENTS_SELECT_PERCENT, DEFAULT_COMPONENTS_SELECT_PERCENT));
            System.err.println("componentSelectPercent = " + componentSelectPercent);
        }

        @Override
        public void map(Int128WritableComparable key, A value, OutputCollector<Vertex, ComponentIdOrEdge> output, Reporter reporter) throws IOException {
            if (random.nextDouble() < componentSelectPercent) {
                outKey.copyFieldsFrom(key);
                outValueReal.copyFieldsFrom(key);
                output.collect(outKey, outValue);
            }
        }
    }
}


