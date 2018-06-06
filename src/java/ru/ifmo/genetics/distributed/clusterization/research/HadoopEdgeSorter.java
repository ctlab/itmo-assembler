package ru.ifmo.genetics.distributed.clusterization.research;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import ru.ifmo.genetics.distributed.clusterization.tasks.ReverseIndexTask;
import ru.ifmo.genetics.distributed.clusterization.types.UndirectUnweightEdge;

import java.io.IOException;

public class HadoopEdgeSorter {

    public static class Map extends MapReduceBase implements Mapper<UndirectUnweightEdge, IntWritable, IntWritable, UndirectUnweightEdge> {


        @Override
        public void map(UndirectUnweightEdge key, IntWritable value, OutputCollector<IntWritable, UndirectUnweightEdge> output, Reporter reporter) throws IOException {
            output.collect(value, key);
        }
    }


    public static void main(String[] args) throws Exception {
        JobConf conf = new JobConf(ReverseIndexTask.class);
        FileSystem fs = FileSystem.get(conf);


        conf.setJobName("Edge sorter");

        conf.setOutputKeyClass(IntWritable.class);
        conf.setOutputValueClass(UndirectUnweightEdge.class);

        conf.setMapperClass(Map.class);
        conf.setReducerClass(IdentityReducer.class);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        JobClient.runJob(conf);
    }
}
