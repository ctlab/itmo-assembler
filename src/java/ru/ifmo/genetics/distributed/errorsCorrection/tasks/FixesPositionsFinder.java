package ru.ifmo.genetics.distributed.errorsCorrection.tasks;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import ru.ifmo.genetics.distributed.errorsCorrection.types.DnaFixWritable;
import ru.ifmo.genetics.distributed.errorsCorrection.types.KmerPositionOrFix;
import ru.ifmo.genetics.distributed.errorsCorrection.types.KmerPositionWritable;
import ru.ifmo.genetics.distributed.errorsCorrection.types.PairedDnaFixWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;

import java.io.IOException;
import java.util.Iterator;

public class FixesPositionsFinder {
    
    public static final String K = FixesFinder.K;

    /*
     *  map: id
     */

    /*
     *   reduce: (kmer, kmerFix + position) -> (readsId, fix)
     */

    public static class Reduce extends
            Reducer<LongWritable, KmerPositionOrFix, Int128WritableComparable, PairedDnaFixWritable> {
        
        private int k;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            super.setup(context);
            k = context.getConfiguration().getInt("K", -1); // TODO handle the "-1" case
        }

        private Int128WritableComparable outPosition = new Int128WritableComparable();
        private PairedDnaFixWritable outFix = new PairedDnaFixWritable();

        @Override()
        protected void reduce(
                LongWritable key, Iterable<KmerPositionOrFix> values,
                Context context) throws IOException, InterruptedException {

            System.err.println("\n===findFixesPositions()===\nd");

//            System.err.println("reduce key: " + key);
            KmerPositionWritable kmerPosition = null;
            DnaFixWritable kmerFix = null;
            Iterator<KmerPositionOrFix> iterator = values.iterator();

            while (iterator.hasNext()) {
                KmerPositionOrFix value = iterator.next();
//                System.err.println("value_type: " + value.getType());
//                System.err.println("value1: " + value.getFirst());
//                System.err.println("value2: " + value.getSecond());
                switch (value.getType()) {
                    case 0:
                        kmerPosition = value.getFirst();
                        break;
                    case 1:
                        kmerFix = value.getSecond();
                        break;
                    default:
                        throw new RuntimeException("Unexpected type " + value.getType());
                }
            }

            assert kmerPosition != null;
            if (kmerFix == null) {
                return;
            }
            assert (0 <= kmerFix.nucPosition) && (kmerFix.nucPosition < k) : kmerFix.nucPosition;

            outPosition = kmerPosition.getPairReadId();
            outFix.isSecondInPair = kmerPosition.isSecondInPair();

            if (kmerPosition.isRc()) {
                kmerFix.newNuc ^= 3;
                kmerFix.nucPosition = k - 1 - kmerFix.nucPosition;
            }

            outFix.setNucPosition(kmerFix.nucPosition + kmerPosition.getLeftmostPosition());
            outFix.setNewNuc(kmerFix.newNuc);
            assert 0 <= outFix.nucPosition() && outFix.nucPosition() < 2000 :
                    outFix.nucPosition() + " = " + kmerFix.nucPosition + " + " + kmerPosition.getLeftmostPosition();

            context.write(outPosition, outFix);
        }

    }
    
    public static void findFixesPositions(Path sourceDir, Path destinationDir, int k)
            throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
        conf.set("K", "" + k);

        Job job = new Job(conf, "Finding fixes positions");

        job.setMapOutputKeyClass(LongWritable.class);
        job.setMapOutputValueClass(KmerPositionOrFix.class);
        job.setOutputKeyClass(Int128WritableComparable.class);
        job.setOutputValueClass(PairedDnaFixWritable.class);

        job.setReducerClass(Reduce.class);

        job.setInputFormatClass(SequenceFileInputFormat.class);
        SequenceFileInputFormat.setInputPaths(job, sourceDir);

        job.setOutputFormatClass(SequenceFileOutputFormat.class);
        SequenceFileOutputFormat.setOutputPath(job, destinationDir);

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
