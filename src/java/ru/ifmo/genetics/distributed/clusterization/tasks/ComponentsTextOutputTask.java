package ru.ifmo.genetics.distributed.clusterization.tasks;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.*;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentIdOrEdge;
import ru.ifmo.genetics.distributed.clusterization.types.Vertex;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Author: Sergey Melnikov
 */
public class ComponentsTextOutputTask {
    public static void makeTextOutput(Path sourceFolder, Path resultFolder) throws IOException {
        final JobConf conf = new JobConf(ComponentsTextOutputTask.class);
        conf.setJobName("make text output");

        FileInputFormat.setInputPaths(conf, sourceFolder);
        FileOutputFormat.setOutputPath(conf, resultFolder);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        conf.setOutputKeyClass(Vertex.class);
        conf.setOutputValueClass(ComponentID.class);

        conf.setMapperClass(MakeTextOutputMapper.class);
        conf.setReducerClass(MakeTextOutputReducer.class);
        JobClient.runJob(conf);

    }

    public static class MakeTextOutputMapper extends MapReduceBase implements Mapper<Vertex, ComponentIdOrEdge, Vertex, ComponentID> {
        @Override
        public void map(Vertex key, ComponentIdOrEdge value, OutputCollector<Vertex, ComponentID> output, Reporter reporter) throws IOException {
            if (value.isFirst()) {
                output.collect(key, value.getFirst());
            }
        }
    }

    public static class MakeTextOutputReducer extends MapReduceBase implements Reducer<Vertex, ComponentID, Vertex, ComponentID> {

        Set<ComponentID> componentIDs = new HashSet<ComponentID>();

        @Override
        public void reduce(Vertex key, Iterator<ComponentID> values, OutputCollector<Vertex, ComponentID> output, Reporter reporter) throws IOException {
            componentIDs.clear();
            while (values.hasNext()) {
                ComponentID value = values.next();
                if (componentIDs.add(new ComponentID(value))) {
                    output.collect(key, value);
                }
            }
        }
    }
}
