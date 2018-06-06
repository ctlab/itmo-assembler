package ru.ifmo.genetics.distributed.clusterization.tasks;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import ru.ifmo.genetics.distributed.clusterization.types.*;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;


import java.io.IOException;
import java.util.Iterator;

public class EdgesBuilderTask {


    public static final String MINIMUM_EDGE_WEIGHT = "MINIMUM_EDGE_WEIGHT";
    public static final String DEFAULT_MINIMUM_EDGE_WEIGHT = "1";

    public static void convertEdgesToDirect(Path oldEdgesFolder, Path newEdgesFolder) throws IOException {
        final JobConf conf = new JobConf(EdgesBuilderTask.class);
        conf.setJobName("Convert edges");
        FileInputFormat.setInputPaths(conf, oldEdgesFolder);
        FileOutputFormat.setOutputPath(conf, newEdgesFolder);
        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setOutputKeyClass(Vertex.class);
        conf.setOutputValueClass(ComponentIdOrEdge.class);

        conf.setMapperClass(ConvertEdgeToDirect.class);
        conf.setReducerClass(IdentityReducer.class);

//        FileOutputFormat.setCompressOutput(conf, true);
//        FileOutputFormat.setOutputCompressorClass(conf, GzipCodec.class);
//        conf.setCompressMapOutput(true);
//        conf.setMapOutputCompressorClass(GzipCodec.class);


        JobClient.runJob(conf);
    }

    private static class Map extends MapReduceBase implements Mapper<LongWritable, Int128ArrayWritable, UndirectUnweightEdge, IntWritable> {

        final UndirectUnweightEdge e = new UndirectUnweightEdge();
        final IntWritable one = new IntWritable(1);
        int numberOfFirst = 20;

        @Override
        public void map(LongWritable key, Int128ArrayWritable value, OutputCollector<UndirectUnweightEdge, IntWritable> output, Reporter reporter) throws IOException {
            int sz = value.get().length;
            Object[] array = value.get();
            int to = sz;
//            int to = Math.min(sz, numberOfFirst);
            for (int i = 0; i < to; i++) {
                for (int j = i + 1; j < sz; j++) {
                    Int128WritableComparable a = ((Int128WritableComparable) array[i]);
                    Int128WritableComparable b = ((Int128WritableComparable) array[j]);
                    if (a.compareTo(b) > 0) {
                        e.first = b;
                        e.second = a;
                    } else {
                        e.first = a;
                        e.second = b;
                    }
                    output.collect(e, one);
                }
            }
        }
    }

    private static class Reduce extends MapReduceBase implements Reducer<UndirectUnweightEdge, IntWritable, UndirectUnweightEdge, IntWritable> {
        IntWritable iw = new IntWritable();

        @Override
        public void reduce(UndirectUnweightEdge key, Iterator<IntWritable> values, OutputCollector<UndirectUnweightEdge, IntWritable> output, Reporter reporter) throws IOException {
            int count = 0;
            while (values.hasNext()) {
                count += values.next().get();
            }
            iw.set(count);
            output.collect(key, iw);
        }
    }

    public static void buildEdges(Path sourceDir, Path targetDir, int minimumEdgeWeight) throws IOException {
        JobConf conf = new JobConf(EdgesBuilderTask.class);
        conf.set(MINIMUM_EDGE_WEIGHT, "" + minimumEdgeWeight);
        conf.setJobName("UndirectUnweightEdge builder");

        conf.setOutputKeyClass(UndirectUnweightEdge.class);
        conf.setOutputValueClass(IntWritable.class);

        conf.setMapperClass(Map.class);
        conf.setCombinerClass(Reduce.class);
        conf.setReducerClass(Reduce.class);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(conf, sourceDir);
        FileOutputFormat.setOutputPath(conf, targetDir);

//        FileOutputFormat.setCompressOutput(conf, true);
//        FileOutputFormat.setOutputCompressorClass(conf, GzipCodec.class);
//        conf.setCompressMapOutput(true);
//        conf.setMapOutputCompressorClass(GzipCodec.class);

        JobClient.runJob(conf);
    }

    public static class ConvertEdgeToDirect extends MapReduceBase implements Mapper<UndirectUnweightEdge, IntWritable, Vertex, ComponentIdOrEdge> {

        final Vertex vertex = new Vertex();
        final DirectEdge directEdge = new DirectEdge();
        final ComponentIdOrEdge componentIdOrEdge = new ComponentIdOrEdge(directEdge);
        private int minimumEdgeWeight;

        @Override
        public void configure(JobConf job) {
            minimumEdgeWeight = Integer.parseInt(job.get(MINIMUM_EDGE_WEIGHT, DEFAULT_MINIMUM_EDGE_WEIGHT));
        }

        @Override
        public void map(UndirectUnweightEdge key, IntWritable value, OutputCollector<Vertex, ComponentIdOrEdge> output, Reporter reporter) throws IOException {
            if (value.get() >= minimumEdgeWeight) {
                directEdge.setWeight(value.get());

                vertex.copyFieldsFrom(key.first);
                directEdge.setTo(key.second);
                output.collect(vertex, componentIdOrEdge);

                vertex.copyFieldsFrom(key.second);
                directEdge.setTo(key.first);
                output.collect(vertex, componentIdOrEdge);
            }
        }
    }
}
