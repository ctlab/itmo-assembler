package ru.ifmo.genetics.distributed.clusterization.bipartite;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import ru.ifmo.genetics.distributed.clusterization.types.PairedDnaQWithIdWritable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class UniqueQuasiContigsTask {

    private static class Reduce extends MapReduceBase
            implements Reducer<Int128WritableComparable, DnaWritable, Int128WritableComparable, DnaWritable> {
        private final Queue<PairedDnaQWithIdWritable> pool = new ArrayDeque<PairedDnaQWithIdWritable>();

        @Override
        public void reduce(Int128WritableComparable key, Iterator<DnaWritable> values,
                           OutputCollector<Int128WritableComparable, DnaWritable> output,
                           Reporter reporter) throws IOException {
            int count = 1;
            output.collect(key, values.next());
            while (values.hasNext()) {
                count++;
                values.next();
            }
            System.err.println("quasicontig " + key + " assembled " + count + " times");

        }
    }

    /**
     * @param source directory with <Int128WritableComparable, DnaWritable>
     * @param result directory with <Int128WritableComparable, DnaWritable>
     */
    public static void makeQuasiContigsUnique(Path source, Path result) throws IOException {
        JobConf conf = new JobConf(UniqueQuasiContigsTask.class);

        conf.setJobName("QuasiContigs Unique task");

        conf.setOutputKeyClass(Int128WritableComparable.class);
        conf.setOutputValueClass(DnaWritable.class);

        conf.setMapperClass(IdentityMapper.class);
        conf.setReducerClass(Reduce.class);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(conf, source);
        FileOutputFormat.setOutputPath(conf, result);

        JobClient.runJob(conf);
    }

}
