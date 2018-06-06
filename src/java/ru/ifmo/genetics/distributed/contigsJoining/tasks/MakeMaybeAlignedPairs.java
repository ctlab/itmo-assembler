package ru.ifmo.genetics.distributed.contigsJoining.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.MultipleInputs;
import ru.ifmo.genetics.distributed.io.formats.SplitBowtieMapInputFormat;
import ru.ifmo.genetics.distributed.io.formats.SplitPairFastqInputFormat;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;
import ru.ifmo.genetics.io.readers.FastqRecordReader;
import ru.ifmo.genetics.tools.microassembly.types.PairedBowtieAlignmentWritable;
import ru.ifmo.genetics.tools.microassembly.types.PairedMaybeAlignedDnaQWritable;
import ru.ifmo.genetics.utils.TextUtils;

import java.io.IOException;
import java.util.Iterator;

public class MakeMaybeAlignedPairs {
    public static final String TRIMMING = "trimming";

    private static Log log = LogFactory.getLog(MakeMaybeAlignedPairs.class);
    public static class AlignsMap extends MapReduceBase
            implements Mapper<
            LongWritable, PairedBowtieAlignmentWritable,
            Text, PairedMaybeAlignedDnaQWritable> {

        PairedMaybeAlignedDnaQWritable outValue = new PairedMaybeAlignedDnaQWritable();

        Text outKey = new Text();


        @Override
        public void map(LongWritable longWritable,
                        PairedBowtieAlignmentWritable pairedBowtieAlignmentWritable,
                        OutputCollector<Text, PairedMaybeAlignedDnaQWritable> collector,
                        Reporter reporter) throws IOException {
            outValue.first.dnaq.clear();
            outValue.first.isAligned = false;

            outValue.second.dnaq.clear();
            outValue.second.isAligned = false;

            Text key;

            if (pairedBowtieAlignmentWritable.firstNotNull()) {
                outValue.first.isAligned = true;
                outValue.first.alignment.copyFieldsFrom(pairedBowtieAlignmentWritable.first());
                key = pairedBowtieAlignmentWritable.first().readId;
            } else {
                assert pairedBowtieAlignmentWritable.seconNotNull();
                outValue.second.isAligned = true;
                outValue.second.alignment.copyFieldsFrom(pairedBowtieAlignmentWritable.second());
                key = pairedBowtieAlignmentWritable.second().readId;
            }

            if (TextUtils.endsWith(key, "/1") || TextUtils.endsWith(key, "/2")) {
                outKey.set(key.getBytes(), 0, key.getLength() - "/1".length());
            } else {
                outKey.set(key);
            }
            collector.collect(outKey, outValue);
        }
    }

    public static class ReadsMap extends MapReduceBase
            implements Mapper<
            Text, PairedDnaQWritable,
            Text, PairedMaybeAlignedDnaQWritable> {

        PairedMaybeAlignedDnaQWritable outValue = new PairedMaybeAlignedDnaQWritable();
        DnaQWritable emptyDnaq = new DnaQWritable();

        Text outKey = new Text();

        @Override
        public void map(
                Text key, PairedDnaQWritable value,
                OutputCollector<Text, PairedMaybeAlignedDnaQWritable> outputCollector,
                Reporter reporter) throws IOException {

            outValue.first.dnaq = emptyDnaq;
            outValue.first.isAligned = false;

            outValue.second.dnaq = emptyDnaq;
            outValue.second.isAligned = false;

            if (value.first.length() > 0) {
                outValue.first.dnaq = value.first;
            } else {
                outValue.second.dnaq = value.second;
            }

            if (TextUtils.endsWith(key, "/1") || TextUtils.endsWith(key, "/2")) {
                outKey.set(key.getBytes(), 0, key.getLength() - "/1".length());
            } else {
                outKey.set(key);
            }

            outputCollector.collect(outKey, outValue);
        }
    }
    
    public static class Reduce extends MapReduceBase
            implements Reducer<
                Text, PairedMaybeAlignedDnaQWritable,
                Text, PairedMaybeAlignedDnaQWritable> {

        private static Log log = LogFactory.getLog(Reduce.class);

        private PairedMaybeAlignedDnaQWritable outValue = new PairedMaybeAlignedDnaQWritable();

        // length of trim from right end
        private int trimming;

        @Override
        public void configure(JobConf job) {
            super.configure(job);
            trimming = Integer.parseInt(job.get(TRIMMING));
        }

        @Override
        public void reduce(Text key, Iterator<PairedMaybeAlignedDnaQWritable> values, OutputCollector<Text, PairedMaybeAlignedDnaQWritable> collector, Reporter reporter) throws IOException {
            outValue.first.dnaq.clear();
            outValue.first.isAligned = false;

            outValue.second.dnaq.clear();
            outValue.second.isAligned = false;

            while (values.hasNext()) {
                PairedMaybeAlignedDnaQWritable value = values.next();
                if (value.first.dnaq.length() > 0) {
                    outValue.first.dnaq.copyFieldsFrom(value.first.dnaq);
                } else if (value.second.dnaq.length() > 0) {
                    outValue.second.dnaq.copyFieldsFrom(value.second.dnaq);
                } else if (value.first.isAligned) {
                    outValue.first.isAligned = true;
                    outValue.first.alignment.copyFieldsFrom(value.first.alignment);
                } else if (value.second.isAligned) {
                    outValue.second.isAligned = true;
                    outValue.second.alignment.copyFieldsFrom(value.second.alignment);
                } else {
                    log.warn("got empty alignment");
                }
            }

            // Offset is wrong for reverse strand alignment because of trimming
            if (outValue.first.isAligned && !outValue.first.alignment.onForwardStrand) {
                outValue.first.alignment.offset -= trimming;
            }
            if (outValue.second.isAligned && !outValue.second.alignment.onForwardStrand) {
                outValue.second.alignment.offset -= trimming;
            }

            if (outValue.first.isAligned || outValue.second.isAligned) {
                collector.collect(key, outValue);
            }
        }
    }
    
    public static void make(Path mapDir, Path fastqDir, String qf, Path destination, int trimming) throws IOException {
        log.info("Starting joining microassembly and reads");
        JobConf conf = new JobConf(MakeMaybeAlignedPairs.class);

        conf.set(TRIMMING, "" + trimming);

        conf.setJobName("Joining aligns and reads");

        conf.setMapOutputKeyClass(Text.class);
        conf.setMapOutputValueClass(PairedMaybeAlignedDnaQWritable.class);
        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(PairedMaybeAlignedDnaQWritable.class);

        conf.set(FastqRecordReader.QUALITY_FORMAT, qf);

        MultipleInputs.addInputPath(conf, mapDir, SplitBowtieMapInputFormat.class, AlignsMap.class);
        MultipleInputs.addInputPath(conf, fastqDir, SplitPairFastqInputFormat.class, ReadsMap.class);
        conf.setReducerClass(Reduce.class);

        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileOutputFormat.setOutputPath(conf, destination);

        JobClient.runJob(conf);
        log.info("Joining microassembly and reads finished");
    }
}
