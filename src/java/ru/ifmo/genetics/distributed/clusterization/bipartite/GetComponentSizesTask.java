package ru.ifmo.genetics.distributed.clusterization.bipartite;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.PairedDnaQWithIdWritable;

import java.io.IOException;
import java.util.Iterator;

/**
 * Author: Sergey Melnikov
 */
public class GetComponentSizesTask {


    private static class Reduce extends MapReduceBase
            implements Reducer<ComponentID, PairedDnaQWithIdWritable, ComponentID, LongWritable> {

        LongWritable value = new LongWritable();

        @Override
        public void reduce(ComponentID key, Iterator<PairedDnaQWithIdWritable> values,
                           OutputCollector<ComponentID, LongWritable> output,
                           Reporter reporter) throws IOException {
            int count = 0;
            while (values.hasNext()) {
                values.next();
                count++;
            }
            value.set(count);
            output.collect(key, value);

        }
    }

    /**
     * @param source directory with <ComponentID, PairedDnaQWithIdWritable>
     * @param result directory with <ComponentID, LongWritable>
     */
    public static void getComponentSizes(Path source, Path result) throws IOException {
        JobConf conf = new JobConf(GetComponentSizesTask.class);


        conf.setJobName("component sizes task");

        conf.setOutputKeyClass(ComponentID.class);
        conf.setOutputValueClass(LongWritable.class);

        conf.setMapOutputKeyClass(ComponentID.class);
        conf.setMapOutputValueClass(PairedDnaQWithIdWritable.class);

        conf.setMapperClass(IdentityMapper.class);
        conf.setReducerClass(Reduce.class);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(conf, source);
        FileOutputFormat.setOutputPath(conf, result);

        JobClient.runJob(conf);
    }

}
