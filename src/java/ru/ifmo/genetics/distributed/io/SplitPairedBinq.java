package ru.ifmo.genetics.distributed.io;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.mapred.lib.MultipleInputs;
import ru.ifmo.genetics.distributed.io.formats.PairedBinqInputFormat;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;

import java.io.IOException;

public class SplitPairedBinq {
    public static void main(String[] args) throws IOException {
        Path inputDir = new Path(args[0]);
        Path workDir = new Path(args[1]);

        JobConf conf = new JobConf(SplitPairedBinq.class);

        conf.setJobName("Splitting binq");

        conf.setOutputKeyClass(Int128WritableComparable.class);
        conf.setOutputValueClass(PairedDnaQWritable.class);

        conf.setReducerClass(IdentityReducer.class);

        MultipleInputs.addInputPath(conf, inputDir, PairedBinqInputFormat.class, IdentityMapper.class);

        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileOutputFormat.setOutputPath(conf, new Path(workDir, "result"));

        JobClient.runJob(conf);

    }
}
