package ru.ifmo.genetics.distributed.clusterization.tasks;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.GenericWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.mapred.lib.MultipleInputs;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentIdOrEdge;
import ru.ifmo.genetics.distributed.clusterization.types.Vertex;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.Union2Writable;
import ru.ifmo.genetics.distributed.util.JobUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Author: Sergey Melnikov
 */
public class ExtractComponentTaskDna {

    public static void extractComponent(Path readsFolder, Path componentsFolder, Path resultFolder) throws IOException {
        final JobConf conf = new JobConf(ExtractComponentTaskDna.class);
        conf.setJobName("Extract reads for components");

        MultipleInputs.addInputPath(conf, readsFolder, SequenceFileInputFormat.class, ConvertReadsMap.class);
        MultipleInputs.addInputPath(conf, componentsFolder, SequenceFileInputFormat.class, ConvertBFSOutputMap.class);

        FileOutputFormat.setOutputPath(conf, resultFolder);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setMapOutputKeyClass(Vertex.class);
        conf.setMapOutputValueClass(ComponentIDOrDnaWritable.class);

        conf.setOutputKeyClass(ComponentID.class);
        conf.setOutputValueClass(DnaWritable.class);

        conf.setReducerClass(TurnReduce.class);
        JobClient.runJob(conf);
    }


    public static class ConvertBFSOutputMap extends MapReduceBase implements Mapper<Vertex, ComponentIdOrEdge, Vertex, ComponentIDOrDnaWritable> {
        private final ComponentIDOrDnaWritable outValue = new ComponentIDOrDnaWritable();

        @Override
        public void map(Vertex key, ComponentIdOrEdge value, OutputCollector<Vertex, ComponentIDOrDnaWritable> output, Reporter reporter) throws IOException {
            if (value.isFirst()) {
                outValue.setSecond(value.getFirst());
                output.collect(key, outValue);
            }
        }
    }

    public static class ConvertReadsMap extends MapReduceBase implements Mapper<Int128WritableComparable, DnaWritable, Vertex, ComponentIDOrDnaWritable> {
        final ComponentIDOrDnaWritable componentIDOrDnaWritable = new ComponentIDOrDnaWritable();
        Vertex vertex = new Vertex();

        @Override
        public void map(Int128WritableComparable key, DnaWritable value, OutputCollector<Vertex, ComponentIDOrDnaWritable> output, Reporter reporter) throws IOException {
            componentIDOrDnaWritable.setFirst(value);
            vertex.copyFieldsFrom(key);
            output.collect(vertex, componentIDOrDnaWritable);
        }
    }


    public static class TurnReduce extends MapReduceBase implements Reducer<Vertex, ComponentIDOrDnaWritable, ComponentID, DnaWritable> {

        @Override
        public void reduce(Vertex key, Iterator<ComponentIDOrDnaWritable> values, OutputCollector<ComponentID, DnaWritable> output, Reporter reporter) throws IOException {
            HashSet<ComponentID> componentIDs = new HashSet<ComponentID>();
            ArrayList<DnaWritable> dnaWritables = new ArrayList<DnaWritable>();
            while (values.hasNext()) {
                ComponentIDOrDnaWritable componentIDOrDnaWritable = values.next();
                if (componentIDOrDnaWritable.isSecond()) {
                    componentIDs.add(componentIDOrDnaWritable.getSecond());
                } else {
                    dnaWritables.add(componentIDOrDnaWritable.getFirst());
                }
            }
            for (ComponentID componentID : componentIDs) {
                for (DnaWritable dnaWritable : dnaWritables) {
                    output.collect(componentID, dnaWritable);
                }
            }
        }
    }


    /**
     * Author: Sergey Melnikov
     */
    public static class ComponentIDOrDnaWritable extends Union2Writable<DnaWritable, ComponentID> {
        public ComponentIDOrDnaWritable() {
            super(new DnaWritable(), new ComponentID(), (byte) 0);
        }
    }
}
