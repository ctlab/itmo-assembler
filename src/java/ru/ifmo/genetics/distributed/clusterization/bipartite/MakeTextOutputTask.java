package ru.ifmo.genetics.distributed.clusterization.bipartite;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import ru.ifmo.genetics.distributed.clusterization.tasks.ComponentsTextOutputTask;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.Vertex;

import java.io.IOException;

/**
 * Author: Sergey Melnikov
 */
public class MakeTextOutputTask {
    public static void makeTextOutput(
            Path source,
            Path target,
            Class<? extends WritableComparable> keyClass,
            Class<? extends Writable> valueClass) throws IOException {
        final JobConf conf = new JobConf(ComponentsTextOutputTask.class);
        conf.setJobName("make text output");

        FileInputFormat.setInputPaths(conf, source);
        FileOutputFormat.setOutputPath(conf, target);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        conf.setOutputKeyClass(keyClass);
        conf.setOutputValueClass(valueClass);

        conf.setMapperClass(IdentityMapper.class);
        conf.setReducerClass(IdentityReducer.class);
        JobClient.runJob(conf);
    }
}
