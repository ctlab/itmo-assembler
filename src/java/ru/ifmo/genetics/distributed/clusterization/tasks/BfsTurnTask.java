package ru.ifmo.genetics.distributed.clusterization.tasks;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentIdOrEdge;
import ru.ifmo.genetics.distributed.clusterization.types.DirectEdge;
import ru.ifmo.genetics.distributed.clusterization.types.Vertex;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Author: Sergey Melnikov
 */
public class BfsTurnTask {
    public static void makeBfsTurn(Path edgesFolder, Path componentFolder, Path resultFolder) throws IOException {
        final JobConf conf = new JobConf(BfsTurnTask.class);
        final FileSystem fs = FileSystem.get(conf);
        conf.setJobName("BFS turn");

        FileInputFormat.setInputPaths(conf, componentFolder, edgesFolder);

        FileOutputFormat.setOutputPath(conf, resultFolder);
        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setOutputKeyClass(Vertex.class);
        conf.setOutputValueClass(ComponentIdOrEdge.class);

        conf.setMapperClass(new IdentityMapper<Vertex, ComponentIdOrEdge>().getClass());
        conf.setReducerClass(BfsTurnReducer.class);

//        FileOutputFormat.setCompressOutput(conf, true);
//        FileOutputFormat.setOutputCompressorClass(conf, GzipCodec.class);
//        conf.setCompressMapOutput(true);
//        conf.setMapOutputCompressorClass(GzipCodec.class);

        JobClient.runJob(conf);

    }

    public static class BfsTurnReducer extends MapReduceBase implements Reducer<Vertex, ComponentIdOrEdge, Vertex, ComponentIdOrEdge> {
        ComponentIdOrEdge componentIdOrEdge = new ComponentIdOrEdge();
        Vertex vertex = new Vertex();

        @Override
        public void reduce(Vertex key, Iterator<ComponentIdOrEdge> values, OutputCollector<Vertex, ComponentIdOrEdge> output, Reporter reporter) throws IOException {
            Set<ComponentID> componentIDs = new HashSet<ComponentID>();
            Set<DirectEdge> directEdges = new HashSet<DirectEdge>();
            while (values.hasNext()) {
                ComponentIdOrEdge componentIdOrEdge = values.next();
                if (componentIdOrEdge.isFirst()) {
                    componentIDs.add(componentIdOrEdge.getFirst());
                } else {
                    directEdges.add(componentIdOrEdge.getSecond());
                }
            }
            for (ComponentID componentID : componentIDs) {
                componentIdOrEdge.setFirst(componentID);
                output.collect(key, componentIdOrEdge);
                for (DirectEdge directEdge : directEdges) {
                    vertex.copyFieldsFrom(directEdge.getTo());
                    output.collect(vertex, componentIdOrEdge);
                }
            }
        }
    }
}
