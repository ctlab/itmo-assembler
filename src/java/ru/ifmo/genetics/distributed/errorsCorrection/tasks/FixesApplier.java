package ru.ifmo.genetics.distributed.errorsCorrection.tasks;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;

import org.apache.hadoop.mapred.lib.MultipleInputs;
import org.apache.hadoop.mapred.lib.MultipleOutputs;
import ru.ifmo.genetics.distributed.errorsCorrection.types.PairedDnaFixWritable;
import ru.ifmo.genetics.distributed.errorsCorrection.types.PairedDnaQOrFix;
import ru.ifmo.genetics.distributed.io.formats.PairedFastqOutputFormat;
import ru.ifmo.genetics.distributed.io.formats.PairedBinqInputFormat;
import ru.ifmo.genetics.distributed.util.ArrayListWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;

import java.io.IOException;
import java.util.Iterator;

public class FixesApplier {

    /*
     * map: id
     */

    /*
     * reduce: (readId,  paired read + [fixes]) -> (readId, paired read)
     */
    public static class Reduce extends MapReduceBase
            implements Reducer<
            Int128WritableComparable, PairedDnaQOrFix,
            Int128WritableComparable, PairedDnaQWritable> {


        private ArrayListWritable<PairedDnaFixWritable> fixes = new ArrayListWritable<PairedDnaFixWritable>(32);

        @Override
        public void reduce(
                Int128WritableComparable key, Iterator<PairedDnaQOrFix> values,
                OutputCollector<Int128WritableComparable, PairedDnaQWritable> output,
                Reporter reporter) throws IOException {

            PairedDnaQWritable pdnaq = null;
            fixes.clear();

            while (values.hasNext()) {
                PairedDnaQOrFix value = values.next();

                switch (value.getType()) {
                    case 0:
                        pdnaq = value.getFirst();
                        break;
                    case 1:
                        fixes.add(value.getSecond());
                        assert value.getSecond().nucPosition() < 2000 : value.getSecond().nucPosition();
                        break;
                    default:
                        assert false;
                }
                
            }
            assert pdnaq != null;

            for (int i = 0; i < fixes.size(); ++i) {

                PairedDnaFixWritable fix = fixes.get(i);
                pdnaq = null;
                if (true) break;
                if (!fix.isSecondInPair) {
                    pdnaq.first.setNuc(fix.nucPosition(), fix.newNuc());
                } else {
                    pdnaq.second.setNuc(fix.nucPosition(), fix.newNuc());
                }
            }
            if (fixes.size() != 0) {
                reporter.incrCounter("reads", "fixed reads", 1);

            }
            reporter.incrCounter("reads", "total reads", 1);


            if (pdnaq != null) {
                output.collect(key, pdnaq);
            }
        }
    }


    public static class UpcastMap1 extends MapReduceBase
            implements Mapper<
            Int128WritableComparable, PairedDnaQWritable,
            Int128WritableComparable, PairedDnaQOrFix> {

        private PairedDnaQOrFix outValue = new PairedDnaQOrFix();

        @Override
        public void map(Int128WritableComparable key, PairedDnaQWritable value, OutputCollector<Int128WritableComparable, PairedDnaQOrFix> output, Reporter reporter) throws IOException {
            outValue.setFirst(value);
            output.collect(key, outValue);
        }
    }

    public static class UpcastMap2 extends MapReduceBase
            implements Mapper<
            Int128WritableComparable, PairedDnaFixWritable,
            Int128WritableComparable, PairedDnaQOrFix> {

        private PairedDnaQOrFix outValue = new PairedDnaQOrFix();

        @Override
        public void map(Int128WritableComparable key, PairedDnaFixWritable value, OutputCollector<Int128WritableComparable, PairedDnaQOrFix> output, Reporter reporter) throws IOException {
            outValue.setSecond(value);
            assert value.nucPosition() < 2000 : value.nucPosition();
            output.collect(key, outValue);
        }
    }

    public static void applyFixes(Path fixesDir, Path readsDir, Path destinationDir) throws IOException {
        JobConf conf = new JobConf(FixesApplier.class);

        conf.setJobName("Applying fixes");

        conf.setMapOutputValueClass(PairedDnaQOrFix.class);
        conf.setOutputKeyClass(Int128WritableComparable.class);
        conf.setOutputValueClass(PairedDnaQWritable.class);

        conf.setReducerClass(Reduce.class);

        MultipleInputs.addInputPath(conf, readsDir, PairedBinqInputFormat.class, UpcastMap1.class);
        MultipleInputs.addInputPath(conf, fixesDir, SequenceFileInputFormat.class, UpcastMap2.class);

//        conf.setOutputFormat(PairedFastqOutputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileOutputFormat.setOutputPath(conf, destinationDir);

        JobClient.runJob(conf);
    }
}
