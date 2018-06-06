package ru.ifmo.genetics.distributed.clusterization.tasks;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.GenericWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import ru.ifmo.genetics.distributed.clusterization.types.PairedDnaQWithIdWritable;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentIdOrEdge;
import ru.ifmo.genetics.distributed.clusterization.types.Vertex;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;


import java.io.IOException;
import java.util.*;

/**
 * Author: Sergey Melnikov
 */
public class ExtractComponentTaskPairedDnaQ {

    public static void extractComponent(Path dnaFolder, Path sourceFolder, Path tmpFolder, Path resultFolder) throws IOException {
        Path tmpFolder1 = new Path(tmpFolder, "1");
        Path tmpFolder2 = new Path(tmpFolder, "2");
        convertReads(dnaFolder, tmpFolder1);
        convertBFSoutput(sourceFolder, tmpFolder2);
        mergeComponentAndReads(tmpFolder1, tmpFolder2, resultFolder);
    }

    public static void convertBFSoutput(Path source, Path target) throws IOException {
        final JobConf conf = new JobConf(ExtractComponentTaskPairedDnaQ.class);
        final FileSystem fs = FileSystem.get(conf);
        conf.setJobName("Convert BFS output");

        FileInputFormat.setInputPaths(conf, source);
        FileOutputFormat.setOutputPath(conf, target);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setMapOutputKeyClass(Vertex.class);
        conf.setMapOutputValueClass(ComponentID.class);

        conf.setOutputKeyClass(Vertex.class);
        conf.setOutputValueClass(ComponentIDOrPairedDnaQWithIdWritable.class);

        conf.setMapperClass(ConvertBFSOutputMap.class);
        conf.setReducerClass(ConvertBFSOutputMapReduce.class);
        JobClient.runJob(conf);
    }

    public static void convertReads(Path source, Path target) throws IOException {
        final JobConf conf = new JobConf(ExtractComponentTaskPairedDnaQ.class);
        final FileSystem fs = FileSystem.get(conf);
        conf.setJobName("Convert Reads");

        FileInputFormat.setInputPaths(conf, source);
        FileOutputFormat.setOutputPath(conf, target);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setOutputKeyClass(Vertex.class);
        conf.setOutputValueClass(ComponentIDOrPairedDnaQWithIdWritable.class);

        conf.setMapperClass(ConvertReadsMap.class);
        conf.setReducerClass(IdentityReducer.class);
        JobClient.runJob(conf);
    }

    public static void mergeComponentAndReads(Path sourceComponent, Path sourceReads, Path target) throws IOException {
        final JobConf conf = new JobConf(ExtractComponentTaskPairedDnaQ.class);
        final FileSystem fs = FileSystem.get(conf);
        conf.setJobName("Merge components and reads");

        FileInputFormat.setInputPaths(conf, sourceComponent, sourceReads);
        FileOutputFormat.setOutputPath(conf, target);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setMapOutputKeyClass(Vertex.class);
        conf.setMapOutputValueClass(ComponentIDOrPairedDnaQWithIdWritable.class);

        conf.setOutputKeyClass(ComponentID.class);
        conf.setOutputValueClass(PairedDnaQWithIdWritable.class);

        conf.setMapperClass(IdentityMapper.class);
        conf.setReducerClass(TurnReduce.class);
        JobClient.runJob(conf);
    }


    public static class ConvertBFSOutputMap extends MapReduceBase implements Mapper<Vertex, ComponentIdOrEdge, Vertex, ComponentID> {

        @Override
        public void map(Vertex key, ComponentIdOrEdge value, OutputCollector<Vertex, ComponentID> output, Reporter reporter) throws IOException {
            if (value.isFirst()) {
                output.collect(key, value.getFirst());
            }
        }
    }

    public static class ConvertBFSOutputMapReduce extends MapReduceBase implements Reducer<Vertex, ComponentID, Vertex, ComponentIDOrPairedDnaQWithIdWritable> {
        final ComponentIDOrPairedDnaQWithIdWritable componentIDOrPairedDnaQWithIdWritable = new ComponentIDOrPairedDnaQWithIdWritable();
        final ComponentID componentID = new ComponentID();

        @Override
        public void reduce(Vertex key, Iterator<ComponentID> values, OutputCollector<Vertex, ComponentIDOrPairedDnaQWithIdWritable> output, Reporter reporter) throws IOException {
            HashSet<Int128WritableComparable> components = new HashSet<Int128WritableComparable>();
            while (values.hasNext()) {
                components.add(new Int128WritableComparable(values.next()));
            }
            componentIDOrPairedDnaQWithIdWritable.set(componentID);
            for (Int128WritableComparable value : components) {
                componentID.copyFieldsFrom(value);
                output.collect(key, componentIDOrPairedDnaQWithIdWritable);
            }
        }
    }

    public static class ConvertReadsMap extends MapReduceBase
            implements Mapper<Int128WritableComparable, PairedDnaQWritable,
            Vertex, ComponentIDOrPairedDnaQWithIdWritable> {
        final ComponentIDOrPairedDnaQWithIdWritable componentIDOrPairedDnaQWithIdWritable =
                new ComponentIDOrPairedDnaQWithIdWritable();
        final PairedDnaQWithIdWritable pairedDnaQWithIdWritable = new PairedDnaQWithIdWritable();
        Vertex vertex = new Vertex();

        @Override
        public void map(Int128WritableComparable key, PairedDnaQWritable value,
                        OutputCollector<Vertex, ComponentIDOrPairedDnaQWithIdWritable> output,
                        Reporter reporter) throws IOException {
            componentIDOrPairedDnaQWithIdWritable.set(pairedDnaQWithIdWritable);
            pairedDnaQWithIdWritable.first = key;
            pairedDnaQWithIdWritable.second = value;
            vertex.copyFieldsFrom(key);
            output.collect(vertex, componentIDOrPairedDnaQWithIdWritable);
        }
    }

    public static class TurnReduce extends MapReduceBase implements Reducer<Vertex, ComponentIDOrPairedDnaQWithIdWritable, ComponentID, PairedDnaQWithIdWritable> {

        @Override
        public void reduce(Vertex key, Iterator<ComponentIDOrPairedDnaQWithIdWritable> values, OutputCollector<ComponentID, PairedDnaQWithIdWritable> output, Reporter reporter) throws IOException {
            ArrayList<ComponentID> componentIDs = new ArrayList<ComponentID>();
            ArrayList<PairedDnaQWithIdWritable> pairedDnaQWithIdWritables = new ArrayList<PairedDnaQWithIdWritable>();
            while (values.hasNext()) {
                ComponentIDOrPairedDnaQWithIdWritable componentIDOrPairedDnaQWithIdWritable = values.next();
                Writable writable = componentIDOrPairedDnaQWithIdWritable.get();
                if (writable instanceof ComponentID) {
                    componentIDs.add((ComponentID) writable);
                } else {
                    pairedDnaQWithIdWritables.add((PairedDnaQWithIdWritable) writable);
                }
            }
            for (ComponentID componentID : componentIDs) {
                for (PairedDnaQWithIdWritable pairedDnaQWithIdWritable : pairedDnaQWithIdWritables) {
                    output.collect(componentID, pairedDnaQWithIdWritable);
                }
            }
        }
    }

    /**
     * Author: Sergey Melnikov
     */
    public static class ComponentIDOrPairedDnaQWithIdWritable extends GenericWritable {
        public ComponentIDOrPairedDnaQWithIdWritable() {
        }

        public ComponentIDOrPairedDnaQWithIdWritable(Writable w) {
            set(w);
        }


        private static final Class[] CLASSES = {
                PairedDnaQWithIdWritable.class,
                ComponentID.class
        };

        @Override
        protected Class[] getTypes() {
            return CLASSES;
        }
    }
}
