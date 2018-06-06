package ru.ifmo.genetics.distributed.clusterization.tasks;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.*;
import ru.ifmo.genetics.distributed.clusterization.types.Int128ArrayWritable;
import ru.ifmo.genetics.distributed.clusterization.types.Kmer;
import ru.ifmo.genetics.distributed.io.KmerIterable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class ReverseIndexTask {

    private static final String KMER_LENGTH = "KMER_LENGTH";
    private static final String DEFAULT_KMER_LENGTH = "15";
    public static final String MAXIMUM_OCCURRENCES_THRESHOLD = "MAXIMUM_OCCURRENCES_THRESHOLD";
    public static final String DEFAULT_MAXIMUM_OCCURRENCES_THRESHOLD = "60";
    public static final String MINIMUM_OCCURRENCES_THRESHOLD = "MINIMUM_OCCURRENCES_THRESHOLD";
    public static final String DEFAULT_MINIMUM_OCCURRENCES_THRESHOLD = "4";

    private static class Map<A extends KmerIterable> extends MapReduceBase implements Mapper<Int128WritableComparable, A, LongWritable, Int128WritableComparable> {
        private final LongWritable outKey = new LongWritable();
        private int kmerLength;

        @Override
        public void configure(JobConf job) {
            this.kmerLength = Integer.parseInt(job.get(KMER_LENGTH, DEFAULT_KMER_LENGTH));
        }

        @Override
        public void map(Int128WritableComparable key, A value, OutputCollector<LongWritable, Int128WritableComparable> output, Reporter reporter) throws IOException {
            Iterator<Kmer> kmerIterator = value.kmerIterator(kmerLength);
            while (kmerIterator.hasNext()) {
                outKey.set(kmerIterator.next().get());
                output.collect(outKey, key);
            }
        }
    }

    private static class Reduce extends MapReduceBase implements Reducer<LongWritable, Int128WritableComparable, LongWritable, Int128ArrayWritable> {
        private final Int128ArrayWritable law = new Int128ArrayWritable();
        private int maximumOccurrencesThreshold;
        private int minimumOccurrencesThreshold;

        @Override
        public void configure(JobConf job) {
            maximumOccurrencesThreshold = Integer.parseInt(job.get(MAXIMUM_OCCURRENCES_THRESHOLD, DEFAULT_MAXIMUM_OCCURRENCES_THRESHOLD));
            minimumOccurrencesThreshold = Integer.parseInt(job.get(MINIMUM_OCCURRENCES_THRESHOLD, DEFAULT_MINIMUM_OCCURRENCES_THRESHOLD));
        }

        @Override
        public void reduce(LongWritable key, Iterator<Int128WritableComparable> values, OutputCollector<LongWritable, Int128ArrayWritable> output, Reporter reporter) throws IOException {
            ArrayList<Int128WritableComparable> al = new ArrayList<Int128WritableComparable>();
            while (values.hasNext()) {
                Int128WritableComparable value = values.next();
                al.add(new Int128WritableComparable(value)); //TODO creating new object, fix me!
            }
            if (al.size() > maximumOccurrencesThreshold) {
                return;
            }
            if (al.size() < minimumOccurrencesThreshold) {
                return;
            }
            law.set(al.toArray(new Int128WritableComparable[al.size()]));

            output.collect(key, law);
        }

    }

    public static void buildReverseIndex(Path sourceDir, Path targetDir, int kmerLength, int minimumOccurrencesThreshold, int maximumOccurrencesThreshold) throws IOException {
        JobConf conf = new JobConf(ReverseIndexTask.class);
        conf.set(KMER_LENGTH, "" + kmerLength);
        conf.set(MAXIMUM_OCCURRENCES_THRESHOLD, "" + maximumOccurrencesThreshold);
        conf.set(MINIMUM_OCCURRENCES_THRESHOLD, "" + minimumOccurrencesThreshold);

        conf.setJobName("ReverseIndex");

        conf.setMapOutputValueClass(Int128WritableComparable.class);
        conf.setOutputKeyClass(LongWritable.class);
        conf.setOutputValueClass(Int128ArrayWritable.class);

        conf.setMapperClass(Map.class);
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
}