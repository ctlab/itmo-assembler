package ru.ifmo.genetics.distributed.errorsCorrection.tasks;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import ru.ifmo.genetics.distributed.errorsCorrection.types.*;
import ru.ifmo.genetics.distributed.io.formats.PairedBinqInputFormat;
import ru.ifmo.genetics.distributed.io.formats.PairedFastqInputFormat;
import ru.ifmo.genetics.distributed.io.writable.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.Map.Entry;

public class FixesFinder {
    
    public static final String K = "k";
    public static final String PREFIX_LENGTH = "prefixLength";

    /*
     * map: (id, reads) -> [(prefix, (k-mer, nucPosition))]
     */

    public static class Map extends MapReduceBase
            implements Mapper<
                Int128WritableComparable, PairedDnaQWritable,
                LongWritable, LongAndKmerPosition> {

        private int k;
        private int prefixLength;
        private long kmerMask;
        private int prefixShift;
        private long rcShift;

        @Override
        public void configure(JobConf job) {
            super.configure(job);
            k = Integer.parseInt(job.get(K));
            prefixLength = Integer.parseInt(job.get(PREFIX_LENGTH));
            kmerMask =  (1L << (2 * k)) - 1;
            prefixShift = 2 * (k - prefixLength);
            rcShift = 2 * (k - 1);
        }

        private LongWritable curPrefix = new LongWritable();
        private LongWritable curKmer = new LongWritable();
        private KmerPositionWritable curPosition = new KmerPositionWritable();
        private LongAndKmerPosition curValue = new LongAndKmerPosition(curKmer, curPosition);

        private void processOneDnaq(
                DnaQWritable dnaq,
                OutputCollector<LongWritable, LongAndKmerPosition> output) throws IOException {
            
            long fwKmer = 0;
            long rcKmer = 0;
            

            for (int i = 0; i < k - 1; ++i) {
                fwKmer <<= 2;
                fwKmer |= dnaq.nucAt(i);
                rcKmer >>>= 2;
                rcKmer |= (long)(dnaq.nucAt(i) ^ 3) << rcShift;
            }
            
            for (int i = k - 1; i < dnaq.length(); ++i) {
                curPosition.setLeftmostPosition(i - (k - 1));

                fwKmer <<= 2;
                fwKmer |= dnaq.nucAt(i);
                fwKmer &= kmerMask;

                curKmer.set(fwKmer);
                curPosition.unsetRcFlag();
                curPrefix.set(fwKmer >>> prefixShift);
                output.collect(curPrefix, curValue);

                rcKmer >>>= 2;
                rcKmer |= (long)(dnaq.nucAt(i) ^ 3) << rcShift;

                curKmer.set(rcKmer);
                curPosition.setRcFlag();
                curPrefix.set(rcKmer >>> prefixShift);
                output.collect(curPrefix, curValue);
            }

        }

        @Override
        public void map(
                Int128WritableComparable key, PairedDnaQWritable value,
                OutputCollector<LongWritable, LongAndKmerPosition> output, Reporter reporter) throws IOException {
            curPosition.copyPairReadId(key);

            curPosition.unsetSecondInPairFlag();
            processOneDnaq(value.first, output);
            curPosition.setSecondInPairFlag();
            processOneDnaq(value.second, output);
        }
    }

    /*
     * reduce: (prefix, [(k-kmer, nucPosition)])) -> [[(kmer, nucPosition) + (kmer, kmerFix)]]
     */

    public static class Reduce extends MapReduceBase
            implements Reducer<
                LongWritable, LongAndKmerPosition,
                Int128WritableComparable, PairedDnaFixWritable> {
//                LongWritable, IntWritable> {

        private int k;
        private int prefixLength;

        @Override
        public void configure(JobConf job) {
            super.configure(job);
            k = Integer.parseInt(job.get(K));
            prefixLength = Integer.parseInt(job.get(PREFIX_LENGTH));
        }

        private final static int MAXIMAL_BAD_FREQUENCY_UPPER_BOUND = 100;
        private int[] frequencyStat = new int[MAXIMAL_BAD_FREQUENCY_UPPER_BOUND];

        private HashMap<MutableLong, MutableInt> kmersStat = new HashMap<MutableLong, MutableInt>();
        private HashMap<MutableLong, ArrayList<KmerPositionWritable>> kmersPositions = new HashMap<MutableLong, ArrayList<KmerPositionWritable>>();

        private Int128WritableComparable outKey = new Int128WritableComparable();
        private DnaFixWritable kmerFix = new DnaFixWritable();
        private PairedDnaFixWritable outValue = new PairedDnaFixWritable();

        private MutableLong tempLong = new MutableLong();

        
        private int findMaxBadFrequency() {
            Arrays.fill(frequencyStat, 0);

            for (MutableInt e: kmersStat.values()) {
                int frequency = e.intValue();
                if (frequency < frequencyStat.length)
                    frequencyStat[frequency]++;
            }
            frequencyStat[0] = Integer.MAX_VALUE;

            int maxBadFrequency = 0;

            while (maxBadFrequency < frequencyStat.length - 2 &&
                    frequencyStat[maxBadFrequency + 1] >= frequencyStat[maxBadFrequency + 2]) {
                ++maxBadFrequency;
            }
            return maxBadFrequency;
        }

        private void outFix(long kmer, DnaFixWritable kmerFix, OutputCollector<Int128WritableComparable, PairedDnaFixWritable> output) throws IOException {
//        private void outFix(long kmer, DnaFixWritable kmerFix, OutputCollector<LongWritable, PairedDnaFixWritable> output) throws IOException {
            tempLong.setValue(kmer);
            for (KmerPositionWritable kmerPosition: kmersPositions.get(tempLong)) {

                assert (0 <= kmerFix.nucPosition) && (kmerFix.nucPosition < k) : kmerFix.nucPosition;

                outKey = kmerPosition.getPairReadId();
                outValue.isSecondInPair = kmerPosition.isSecondInPair();

                if (kmerPosition.isRc()) {
                    kmerFix.newNuc ^= 3;
                    kmerFix.nucPosition = k - 1 - kmerFix.nucPosition;
                }

                outValue.setNucPosition(kmerFix.nucPosition + kmerPosition.getLeftmostPosition());
                outValue.setNewNuc(kmerFix.newNuc);
                assert 0 <= outValue.nucPosition() && outValue.nucPosition() < 2000 :
                        outValue.nucPosition() + " = " + kmerFix.nucPosition + " + " + kmerPosition.getLeftmostPosition();
                output.collect(outKey, outValue);
//                output.collect(new LongWritable(kmer), outValue);
            }
        }
        
        private void findFixes(
            OutputCollector<Int128WritableComparable, PairedDnaFixWritable> output) throws IOException {
//            OutputCollector<LongWritable, PairedDnaFixWritable> output) throws IOException {


            int maxBadFrequency = findMaxBadFrequency();

            for (java.util.Map.Entry<MutableLong, MutableInt> e: kmersStat.entrySet()) {
                if (e.getValue().intValue() > maxBadFrequency) {
                    continue;
                }
                long kmer = e.getKey().longValue();
                int possibleFixes = 0;

                findFix:
                for (int i = prefixLength; i < k; ++i) {
                    long xorShift = 2 * (k - i - 1);
                    for (long xor = 1; xor <= 3; ++xor) {
                        long newKmer = kmer ^ (xor << xorShift);
                        tempLong.setValue(newKmer);
                        MutableInt newKmerCount = (kmersStat.get(tempLong));
                        if (newKmerCount != null && newKmerCount.intValue() > maxBadFrequency) {
                            possibleFixes++;
                            kmerFix.newNuc = (byte)((newKmer >>> xorShift) & 3);
                            kmerFix.nucPosition = i;
                            if (possibleFixes > 1) {
                                break findFix;
                            }
                        }
                    }
                }

                if (possibleFixes == 1) {
                    outFix(kmer, kmerFix, output);
                }
            }
        }
        
        
        @Override
        public void reduce(
                LongWritable key, Iterator<LongAndKmerPosition> values,
                OutputCollector<Int128WritableComparable, PairedDnaFixWritable> output,
//                OutputCollector<LongWritable, IntWritable> output,
                Reporter reporter) throws IOException {

            reporter.setStatus("Calculating kmers stat");
            LongAndKmerPosition value;

            kmersStat.clear();
            kmersPositions.clear();

            while (values.hasNext()) {
                value = values.next();
//                System.err.println(value.first().get() + " " + value.second());
                tempLong.setValue(value.first().get());
                if (!kmersStat.containsKey(tempLong)) {
                    MutableLong kmer = new MutableLong(tempLong.longValue());
                    kmersStat.put(kmer, new MutableInt(0));
                    kmersPositions.put(kmer, new ArrayList<KmerPositionWritable>());
                }
                kmersStat.get(tempLong).increment();
                kmersPositions.get(tempLong).add(new KmerPositionWritable(value.second()));
            }

            /*
            for (Entry<MutableLong, MutableInt> e : kmersStat.entrySet()) {
                output.collect(new LongWritable(e.getKey().longValue()), new IntWritable(e.getValue().intValue()));
            }
            */

            reporter.setStatus("Finding fixes");
            findFixes(output);
        }
    }

    public static void findKmersFixes(Path sourceDir, Path destinationDir, int k, int prefixLength) throws IOException {
        JobConf conf = new JobConf(FixesFinder.class);

        conf.setJobName("Finding fixes");
        conf.set(K, "" + k);
        conf.set(PREFIX_LENGTH, "" + prefixLength);
//        conf.setNumReduceTasks(12);

        conf.setMapOutputKeyClass(LongWritable.class);
        conf.setMapOutputValueClass(LongAndKmerPosition.class);

//        conf.setOutputKeyClass(LongWritable.class);
        conf.setOutputKeyClass(Int128WritableComparable.class);
//        conf.setOutputValueClass(IntWritable.class);
        conf.setOutputValueClass(PairedDnaFixWritable.class);

        conf.setMapperClass(Map.class);
        conf.setReducerClass(Reduce.class);

        conf.setInputFormat(PairedBinqInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);
//        conf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(conf, sourceDir);
        FileOutputFormat.setOutputPath(conf, destinationDir);

        JobClient.runJob(conf);
    }

}
