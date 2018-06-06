package ru.ifmo.genetics.distributed.util;

import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;

/**
 * Author: Sergey Melnikov
 */
public class Compress {
    private Compress() {
    }

    public static void enableFullCompression(JobConf conf) {
        FileOutputFormat.setCompressOutput(conf, true);
        FileOutputFormat.setOutputCompressorClass(conf, GzipCodec.class);
        conf.setCompressMapOutput(true);
        conf.setMapOutputCompressorClass(GzipCodec.class);
    }
}
