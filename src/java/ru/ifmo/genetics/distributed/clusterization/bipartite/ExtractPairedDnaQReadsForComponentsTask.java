package ru.ifmo.genetics.distributed.clusterization.bipartite;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.MultipleInputs;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.PairedDnaQWithIdWritable;
import ru.ifmo.genetics.distributed.clusterization.types.VertexOrKmerOrComponentIDWritable;
import ru.ifmo.genetics.distributed.clusterization.types.VertexOrKmerWritableComparable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.Union2Writable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Author: Sergey Melnikov
 */
public class ExtractPairedDnaQReadsForComponentsTask {
    private ExtractPairedDnaQReadsForComponentsTask() {
    }

    /**
     * @param reads      directory with type <Int128WritableComparable, PairedDnaQWritable>
     * @param components directory with logical type <Vertex|Kmer, ComponentID>
     *                   and physical type <VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable>
     * @param result     directory with result of Reducer, type <ComponentID, PairedDnaQWithIdWritable>
     */
    public static void extractPairedDnaQReadsForComponentsTask(Path reads, Path components,
                                                               Path result) throws IOException {
        JobConf conf = new JobConf(IndexesTask.class);
        conf.setJobName("extract reads for components task");

        conf.setOutputKeyClass(ComponentID.class);
        conf.setOutputValueClass(PairedDnaQWithIdWritable.class);

        conf.setMapOutputKeyClass(Int128WritableComparable.class);
        conf.setMapOutputValueClass(ComponentIDOrRead.class);

        conf.setReducerClass(Reduce.class);

        MultipleInputs.addInputPath(conf, components, SequenceFileInputFormat.class, UpcastMap1.class);
        MultipleInputs.addInputPath(conf, reads, SequenceFileInputFormat.class, UpcastMap2.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileOutputFormat.setOutputPath(conf, result);

        JobClient.runJob(conf);
    }

    public static class ComponentIDOrRead extends Union2Writable<ComponentID, PairedDnaQWithIdWritable> {
        public ComponentIDOrRead() {
            super(new ComponentID(), new PairedDnaQWithIdWritable(), (byte) 0);
        }
    }


    public static class UpcastMap1 extends MapReduceBase implements
            Mapper<VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable,
                    Int128WritableComparable, ComponentIDOrRead> {
        private final Int128WritableComparable outKey = new Int128WritableComparable();
        private ComponentIDOrRead outValue = new ComponentIDOrRead();


        @Override
        public void map(VertexOrKmerWritableComparable key, VertexOrKmerOrComponentIDWritable value,
                        OutputCollector<Int128WritableComparable, ComponentIDOrRead> output,
                        Reporter reporter) throws IOException {
//            System.err.println(
//                    "key:" + key.getType() + " " + key.isFirst() + ", value:" + value.getType() + " " + value.isThird());
            if (key.isFirst() && value.isThird()) {
                outKey.copyFieldsFrom(key.getFirst());
                outValue.setFirst(value.getThird());
                output.collect(outKey, outValue);
            }
        }
    }

    public static class UpcastMap2 extends MapReduceBase implements
            Mapper<Int128WritableComparable, PairedDnaQWritable, Int128WritableComparable, ComponentIDOrRead> {
        private PairedDnaQWithIdWritable pairedDnaQWithIdWritable = new PairedDnaQWithIdWritable();
        private ComponentIDOrRead outValue = new ComponentIDOrRead();


        @Override
        public void map(Int128WritableComparable key, PairedDnaQWritable value,
                        OutputCollector<Int128WritableComparable, ComponentIDOrRead> output,
                        Reporter reporter) throws IOException {
            pairedDnaQWithIdWritable.first = key;
            pairedDnaQWithIdWritable.second = value;
            outValue.setSecond(pairedDnaQWithIdWritable);
            output.collect(key, outValue);
        }
    }


    public static class Reduce extends MapReduceBase implements
            Reducer<Int128WritableComparable, ComponentIDOrRead, ComponentID, PairedDnaQWithIdWritable> {

        @Override
        public void reduce(Int128WritableComparable key, Iterator<ComponentIDOrRead> values,
                           OutputCollector<ComponentID, PairedDnaQWithIdWritable> output,
                           Reporter reporter) throws IOException {
            ArrayList<ComponentID> componentIDs = new ArrayList<ComponentID>();
            ArrayList<PairedDnaQWithIdWritable> as = new ArrayList<PairedDnaQWithIdWritable>();
            while (values.hasNext()) {
                ComponentIDOrRead value = values.next();
                if (value.isFirst()) {
                    componentIDs.add(new ComponentID(value.getFirst()));
                } else {
                    as.add(value.getSecond());
                }
            }
//            System.err.println("componentIDs.calculateSize() = " + componentIDs.calculateSize() + ", as.calculateSize() = " + as.calculateSize());
            assert as.size() == 1;
            System.err.println(key + " in " + componentIDs.size() + " components");
            PairedDnaQWithIdWritable value = as.get(0);
            for (ComponentID componentID : componentIDs) {
                output.collect(componentID, value);
            }
        }
    }
}
