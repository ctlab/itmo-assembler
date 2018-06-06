package ru.ifmo.genetics.distributed.clusterization.bipartite;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.MultipleInputs;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.VertexOrKmerOrComponentIDWritable;
import ru.ifmo.genetics.distributed.clusterization.types.VertexOrKmerWritableComparable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.Union2Writable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Author: Sergey Melnikov
 */
public class ExtractReadsForComponentsTask {
    private ExtractReadsForComponentsTask() {
    }

    private static final String REAL_A_CLASS = "realAClass";
    private static final String CLASS_OF_ComponentIDOrA = "classOfComponentIDOrA";

    /**
     * @param reads      directory with type <Int128WritableComparable, A>
     * @param components directory with logical type <Vertex|Kmer, ComponentID>
     *                   and physical type <VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable>
     * @param result     directory with result of Reducer, type <Int128WritableComparable, A>
     * @param readsClass type token of reads(A) in reads directory
     */
    public static void extractReadsForComponentsTask(Path reads, Path components, Path result,
                                                     Class<? extends Writable> readsClass) throws IOException {
        JobConf conf = new JobConf(IndexesTask.class);
        Class<? extends ComponentIDOrA> componentIDOrAClass;
        if (readsClass.equals(PairedDnaQWritable.class)) {
            componentIDOrAClass = ComponentIDOrPairedDnaQWritable.class;
        } else if (readsClass.equals(DnaWritable.class)) {
            componentIDOrAClass = ComponentIDOrDnaWritable.class;
        } else {
            throw new UnsupportedOperationException("Can't create ComponentIDorA for " + readsClass.getName());
        }
        conf.set(CLASS_OF_ComponentIDOrA, componentIDOrAClass.getName());
        conf.set(REAL_A_CLASS, readsClass.getName());
        conf.setJobName("extract reads for components task");

        conf.setOutputKeyClass(ComponentID.class);
        conf.setOutputValueClass(readsClass);

        conf.setMapOutputKeyClass(Int128WritableComparable.class);
        conf.setMapOutputValueClass(componentIDOrAClass);

        conf.setReducerClass(Reduce.class);

        MultipleInputs.addInputPath(conf, components, SequenceFileInputFormat.class, UpcastMap1.class);
        MultipleInputs.addInputPath(conf, reads, SequenceFileInputFormat.class, UpcastMap2.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileOutputFormat.setOutputPath(conf, result);

        JobClient.runJob(conf);
    }


    public static class ComponentIDOrPairedDnaQWritable extends ComponentIDOrA<PairedDnaQWritable> {
        public ComponentIDOrPairedDnaQWritable() throws InstantiationException, IllegalAccessException {
            super(PairedDnaQWritable.class);
        }
    }

    public static class ComponentIDOrDnaWritable extends ComponentIDOrA<DnaWritable> {
        public ComponentIDOrDnaWritable() throws InstantiationException, IllegalAccessException {
            super(DnaWritable.class);
        }
    }

    private static class ComponentIDOrA<A extends Writable> extends Union2Writable<ComponentID, A> {
        public ComponentIDOrA(Class<A> clazz) throws IllegalAccessException, InstantiationException {
            super(new ComponentID(), clazz.newInstance(), (byte) 0);
        }
    }

    public static class UpcastMap1<A extends Writable> extends MapReduceBase implements
            Mapper<VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable,
                    Int128WritableComparable, ComponentIDOrA<A>> {
        private final Int128WritableComparable outKey = new Int128WritableComparable();
        private ComponentIDOrA outValue;

        @Override
        public void configure(JobConf job) {
            try {
                outValue = (ComponentIDOrA) Class.forName(job.get(CLASS_OF_ComponentIDOrA)).newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void map(VertexOrKmerWritableComparable key, VertexOrKmerOrComponentIDWritable value,
                        OutputCollector<Int128WritableComparable, ComponentIDOrA<A>> output,
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

    public static class UpcastMap2<A extends Writable> extends MapReduceBase implements
            Mapper<Int128WritableComparable, A, Int128WritableComparable, ComponentIDOrA<A>> {
        private ComponentIDOrA outValue;

        @Override
        public void configure(JobConf job) {
            try {
                outValue = (ComponentIDOrA) Class.forName(job.get(CLASS_OF_ComponentIDOrA)).newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void map(Int128WritableComparable key, A value,
                        OutputCollector<Int128WritableComparable, ComponentIDOrA<A>> output,
                        Reporter reporter) throws IOException {
            outValue.setSecond(value);
            output.collect(key, outValue);
        }
    }


    public static class Reduce<A extends Writable> extends MapReduceBase implements
            Reducer<Int128WritableComparable, ComponentIDOrA<A>, ComponentID, A> {

        @Override
        public void reduce(Int128WritableComparable key, Iterator<ComponentIDOrA<A>> values,
                           OutputCollector<ComponentID, A> output, Reporter reporter) throws IOException {
            ArrayList<ComponentID> componentIDs = new ArrayList<ComponentID>();
            ArrayList<A> as = new ArrayList<A>();
            while (values.hasNext()) {
                ComponentIDOrA value = values.next();
                if (value.isFirst()) {
                    componentIDs.add(new ComponentID((Int128WritableComparable) value.getFirst()));
                } else {
                    as.add((A) value.getSecond());
                }
            }
//            System.err.println("componentIDs.calculateSize() = " + componentIDs.calculateSize() + ", as.calculateSize() = " + as.calculateSize());
            assert as.size() == 1;
            A value = as.get(0);
            for (ComponentID componentID : componentIDs) {
                output.collect(componentID, value);
            }
        }
    }
}
