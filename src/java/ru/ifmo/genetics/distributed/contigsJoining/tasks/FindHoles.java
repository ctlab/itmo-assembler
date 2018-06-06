package ru.ifmo.genetics.distributed.contigsJoining.tasks;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.MultipleInputs;
import ru.ifmo.genetics.distributed.contigsJoining.types.Contig;
import ru.ifmo.genetics.distributed.contigsJoining.types.ContigEnd;
import ru.ifmo.genetics.distributed.contigsJoining.types.ContigOrAlignment;
import ru.ifmo.genetics.distributed.contigsJoining.types.Hole;
import ru.ifmo.genetics.distributed.io.formats.ExtendedFastaInputFormat;
import ru.ifmo.genetics.distributed.io.formats.FastaInputFormat;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.util.ArrayListWritable;
import ru.ifmo.genetics.tools.microassembly.types.PairedMaybeAlignedDnaQWritable;
import ru.ifmo.genetics.utils.TextUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FindHoles {
    private static Log log = LogFactory.getLog(FindHoles.class);
    // <Text id, JoinedAlign align> -> <Int contigId, Hole>
    public static class AlignsMap extends MapReduceBase
            implements Mapper<
            Text, PairedMaybeAlignedDnaQWritable,
            ContigEnd, ContigOrAlignment> {

        ContigEnd outKey = new ContigEnd();
        ContigOrAlignment outValue = new ContigOrAlignment(null, null, (byte)0);
        
        @Override
        public void map(Text key, PairedMaybeAlignedDnaQWritable value, OutputCollector<ContigEnd, ContigOrAlignment> output, Reporter reporter) throws IOException {
            reporter.getCounter("", "aligns").increment(1);
            if (!value.first.isAligned && !value.second.isAligned) {
                return;
            }
            if (value.first.alignment.contigId != value.second.alignment.contigId) {
                reporter.getCounter("", "joiningAligns").increment(1);
            }

            outValue.setSecond(value);

            if (value.first.isAligned) {
                outKey.set(value.first.alignment.contigId, value.first.alignment.onForwardStrand);
                output.collect(outKey, outValue);
            }

            if (value.second.isAligned) {
                outKey.set(value.second.alignment.contigId, value.second.alignment.onForwardStrand);
                output.collect(outKey, outValue);
            }
        }
    }

    // <Text contigId, Dna> -> <[ContigEnd], Dna>
    public static class ContigsMap extends MapReduceBase
            implements Mapper<
                Text, DnaQWritable,
                ContigEnd, ContigOrAlignment> {

        public ContigEnd outKey = new ContigEnd();
        public Contig outContig = new Contig();
        public ContigOrAlignment outValue = new ContigOrAlignment(outContig);

        @Override
        public void map(Text key, DnaQWritable value, OutputCollector<ContigEnd, ContigOrAlignment> output, Reporter reporter) throws IOException {
            reporter.getCounter("", "contigs").increment(1);
            int contigId = TextUtils.parseInt(key, 0, TextUtils.getWordEnd(key, 0));
            outContig.id = contigId;
            outContig.sequence = value;

            outKey.set(contigId, true);
            output.collect(outKey, outValue);
            outKey.set(contigId, false);
            output.collect(outKey, outValue);
        }
    }


    // <Int contigId, Dna + [JoinedAlign] > -> <Hole, Dna + [JoinedAlign]>
    public static class Reduce extends MapReduceBase
            implements Reducer<
            ContigEnd, ContigOrAlignment,
            Hole, ContigOrAlignment> {
        private Log log = LogFactory.getLog(Reduce.class);
        private Hole outKey = new Hole();
        private Hole tempHole = new Hole();

        private Contig outContig;
        private ContigOrAlignment outValue = new ContigOrAlignment();
        private ArrayListWritable<PairedMaybeAlignedDnaQWritable> outAligns = new ArrayListWritable<PairedMaybeAlignedDnaQWritable>(100);
        
        private HashMap<Hole, MutableInt> holesStat = new HashMap<Hole, MutableInt>();

        @Override
        public void reduce(ContigEnd key, Iterator<ContigOrAlignment> values, OutputCollector<Hole, ContigOrAlignment> output, Reporter reporter) throws IOException {
            holesStat.clear();
            outAligns.clear();
            System.err.println("Reducing " + key);
            int bridgesFound = 0;
            int maxCount = 0;
            outContig = null;
            while (values.hasNext()) {
                ContigOrAlignment value = values.next();
                if (value.isFirst()) {
                    outContig = value.getFirst();
                    // System.err.println("found contig: " + outContig.id);
                } else {
                    PairedMaybeAlignedDnaQWritable align = value.getSecond();
                    outAligns.add(align);
                    // System.err.println("found align: " + align);
                    if (align.first.isAligned && align.second.isAligned && align.first.alignment.contigId != align.second.alignment.contigId) {
                        bridgesFound++;
                        tempHole.set(align);
                        if (!holesStat.containsKey(tempHole)) {
                            Hole newKey = new Hole();
                            newKey.copyFieldsFrom(tempHole);
                            holesStat.put(newKey, new MutableInt(0));
                        }

                        MutableInt statValue =  holesStat.get(tempHole);
                        statValue.increment();
                        maxCount = Math.max(maxCount, statValue.intValue());
                    }
                }

            }
            if (outContig == null) {
                log.warn("Contig not found, skipping key");
                return;
            }
            

            System.err.println("stat:");
            for (Map.Entry<Hole, MutableInt> entry: holesStat.entrySet()) {
                System.err.println(entry.getKey() + ": " + entry.getValue());
            }

            System.err.println("end of stat");
            int holesSelected = 0;
            for (Map.Entry<Hole, MutableInt> entry: holesStat.entrySet()) {
                // :ToDo: move threshold to config
                if (entry.getValue().intValue() > 10) {
                    outKey.copyFieldsFrom(entry.getKey());
                    holesSelected++;
                    outValue.setFirst(outContig);
                    output.collect(outKey, outValue);

                    for (int i = 0; i < outAligns.size(); ++i) {
                        outValue.setSecond(outAligns.get(i));
                        // System.err.println("collecting " + outValue.getSecond());
                        // :ToDo: filter out bad overlapping bridges
                        output.collect(outKey, outValue);
                    }
                }

            }

            // And just extending contig
            outKey.setOpen(key.contigId, !key.rightEnd);
            for (int i = 0; i < outAligns.size(); ++i) {
                outValue.setSecond(outAligns.get(i));
                output.collect(outKey, outValue);
            }
            outValue.setFirst(outContig);
            output.collect(outKey, outValue);


            System.err.println(holesSelected +  " holes  selected");
        }
    }
    
    public static void find(Path joinedAlignsDir, Path contigsDir, Path destination) throws IOException {
        log.info("Starting finding holes");
        JobConf conf = new JobConf(FindHoles.class);

        conf.setJobName("Finding holes to fill");

        conf.setMapOutputKeyClass(ContigEnd.class);
        conf.setMapOutputValueClass(ContigOrAlignment.class);
        conf.setOutputKeyClass(Hole.class);
        conf.setOutputValueClass(ContigOrAlignment.class);
        
        System.err.println("joinedAlignsDir: " + joinedAlignsDir);
        MultipleInputs.addInputPath(conf, joinedAlignsDir, SequenceFileInputFormat.class, AlignsMap.class);
        MultipleInputs.addInputPath(conf, contigsDir, ExtendedFastaInputFormat.class, ContigsMap.class);
        conf.setReducerClass(Reduce.class);

        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileOutputFormat.setOutputPath(conf, destination);

        JobClient.runJob(conf);

        log.info("Finding holes finished");
    }
}
