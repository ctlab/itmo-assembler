package ru.ifmo.genetics.distributed.clusterization.tasks;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import org.apache.hadoop.mapred.lib.IdentityReducer;

import java.io.IOException;

/**
 * Author: Sergey Melnikov
 */
public class Converter {
    public static void convert(Path sourceFolder, Path resultFolder, Class<?> keyClass, Class<?> valueClass) throws IOException {
        final JobConf conf = new JobConf(Converter.class);
        final FileSystem fs = FileSystem.get(conf);
        conf.setJobName("make text output");

        FileInputFormat.setInputPaths(conf, sourceFolder);
        FileOutputFormat.setOutputPath(conf, resultFolder);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        conf.setOutputKeyClass(keyClass);
        conf.setOutputValueClass(valueClass);

        conf.setMapperClass(IdentityMapper.class);
        conf.setReducerClass(IdentityReducer.class);
        JobClient.runJob(conf);

    }



}
