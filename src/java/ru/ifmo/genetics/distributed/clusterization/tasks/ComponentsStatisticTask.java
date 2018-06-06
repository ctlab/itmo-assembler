package ru.ifmo.genetics.distributed.clusterization.tasks;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentIdOrEdge;
import ru.ifmo.genetics.distributed.clusterization.types.Vertex;

import java.io.IOException;
import java.util.Iterator;

/**
 * Author: Sergey Melnikov
 */
public class ComponentsStatisticTask {
    public static void countStatistics(Path componentFolder, Path resultFolder) throws IOException {
        Path tmp = new Path(resultFolder, "tmp");
        firstReduce(componentFolder, tmp);
        secondReduce(tmp, new Path(resultFolder, "result"));
    }

    private static void firstReduce(Path source, Path target) throws IOException {
        final JobConf conf = new JobConf(ComponentsStatisticTask.class);
        conf.setJobName("ComponentsStatistic Task 1");

        FileInputFormat.setInputPaths(conf, source);

        FileOutputFormat.setOutputPath(conf, target);
        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setMapOutputKeyClass(Vertex.class);
        conf.setMapOutputValueClass(LongWritable.class);
        conf.setOutputKeyClass(LongWritable.class);
        conf.setOutputValueClass(LongWritable.class);

        conf.setMapperClass(Map.class);
        conf.setReducerClass(FirstReduce.class);

        FileOutputFormat.setCompressOutput(conf, true);
        FileOutputFormat.setOutputCompressorClass(conf, GzipCodec.class);
        conf.setCompressMapOutput(true);
        conf.setMapOutputCompressorClass(GzipCodec.class);

        JobClient.runJob(conf);

    }

    private static void secondReduce(Path source, Path target) throws IOException {
        final JobConf conf = new JobConf(ComponentsStatisticTask.class);
        conf.setJobName("ComponentsStatistic Task 2");

        FileInputFormat.setInputPaths(conf, source);

        FileOutputFormat.setOutputPath(conf, target);
        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        conf.setOutputKeyClass(LongWritable.class);
        conf.setOutputValueClass(LongWritable.class);

        conf.setMapperClass(IdentityMapper.class);
        conf.setReducerClass(SecondReduce.class);

        JobClient.runJob(conf);

    }

    public static class Map extends MapReduceBase implements Mapper<Vertex, ComponentIdOrEdge, Vertex, LongWritable> {
        private final LongWritable ONE = new LongWritable(1);

        @Override
        public void map(Vertex key, ComponentIdOrEdge value, OutputCollector<Vertex, LongWritable> output, Reporter reporter) throws IOException {
            output.collect(key, ONE);
        }
    }

    public static class FirstReduce extends MapReduceBase implements Reducer<Vertex, LongWritable, LongWritable, LongWritable> {
        final LongWritable longWritable = new LongWritable();
        final LongWritable ONE = new LongWritable(1);


        @Override
        public void reduce(Vertex key, Iterator<LongWritable> values, OutputCollector<LongWritable, LongWritable> output, Reporter reporter) throws IOException {
            long sum = 0;
            while (values.hasNext()) {
                sum += values.next().get();
            }
            longWritable.set(sum);
            output.collect(longWritable, ONE);
        }
    }

    private static class SecondReduce extends MapReduceBase implements Reducer<LongWritable, LongWritable, LongWritable, LongWritable> {
        final LongWritable longWritable = new LongWritable();

        @Override
        public void reduce(LongWritable key, Iterator<LongWritable> values, OutputCollector<LongWritable, LongWritable> output, Reporter reporter) throws IOException {
            long sum = 0;
            while (values.hasNext()) {
                sum += values.next().get();
            }
            longWritable.set(sum);
            output.collect(key, longWritable);
        }
    }
}
