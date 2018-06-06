package ru.ifmo.genetics.distributed.contigsJoining.tasks;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import org.apache.hadoop.mapred.lib.MultipleInputs;
import ru.ifmo.genetics.distributed.contigsJoining.types.Contig;
import ru.ifmo.genetics.distributed.contigsJoining.types.ContigOrAlignment;
import ru.ifmo.genetics.distributed.contigsJoining.types.Filler;
import ru.ifmo.genetics.distributed.contigsJoining.types.Hole;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.util.ArrayListWritable;
import ru.ifmo.genetics.dna.*;
import ru.ifmo.genetics.io.writers.NullDedicatedWriter;
import ru.ifmo.genetics.structures.debriujn.WeightedDeBruijnGraph;
import ru.ifmo.genetics.tools.irf.FillingReport;
import ru.ifmo.genetics.tools.irf.FillingResult;
import ru.ifmo.genetics.tools.rf.Orientation;
import ru.ifmo.genetics.tools.microassembly.types.PairedMaybeAlignedDnaQWritable;
import ru.ifmo.genetics.tools.irf.FillingTask;
import ru.ifmo.genetics.tools.irf.GlobalContext;
import ru.ifmo.genetics.tools.olc.layouter.Consensus;
import ru.ifmo.genetics.utils.pairs.ImmutablePair;
import ru.ifmo.genetics.utils.pairs.Pair;

import java.io.IOException;
import java.util.*;

public class FillHoles {
    public static String MIN_LENGTH = "minLength";
    public static String MAX_LENGTH = "maxLength";

    private static Log log = LogFactory.getLog(FillHoles.class);

    public static class Reduce extends MapReduceBase
        implements Reducer<
            Hole, ContigOrAlignment,
            Hole, Filler> {
        
        final int K = 19;
        int minLength;
        int maxLength;

        private ArrayListWritable<PairedMaybeAlignedDnaQWritable> preads = new ArrayListWritable<PairedMaybeAlignedDnaQWritable>(100);

        private Contig[] contigs = new Contig[]{ new Contig(), new Contig()};
        
        private DnaWritable outSequence = new DnaWritable();
        private Filler outValue = new Filler(0, 0, outSequence);
        
        private HashMap<MutableInt, Consensus> consensuses = new HashMap<MutableInt, Consensus>();
        
        private MutableInt tempInt = new MutableInt();

        private final DnaQWritable emptyDnaq = new DnaQWritable();
        private MutableDnaView tempView = new MutableDnaView();

        @Override
        public void configure(JobConf job) {
            super.configure(job);
            minLength = job.getInt(MIN_LENGTH, -1);
            maxLength = job.getInt(MAX_LENGTH, -1);
            if (minLength == -1 || maxLength == -1) {
                throw new RuntimeException("length bounds aren't set");
            }
        }
        
        public static boolean preadIsWellOriented(Hole hole, PairedMaybeAlignedDnaQWritable pread) {
            /*
             * |---leftContig---->   |---rightContig--->
             *     |-firstRest->       <-secondRead-|
              */
            assert pread.first.isAligned || pread.second.isAligned;
            return
                    !pread.first.isAligned &&
                    pread.second.isAligned &&
                    pread.second.alignment.contigId == hole.rightContigId &&
                    pread.second.alignment.onForwardStrand == hole.rightComplemented
                    ||
                    pread.first.isAligned &&
                    !pread.second.isAligned &&
                    pread.first.alignment.contigId == hole.leftContigId &&
                    pread.first.alignment.onForwardStrand != hole.leftComplemented
                    ||
                    pread.first.isAligned &&
                    pread.second.isAligned &&
                    (
                            pread.first.alignment.contigId == hole.leftContigId &&
                            pread.first.alignment.onForwardStrand != hole.leftComplemented
                            ||
                            pread.second.alignment.contigId == hole.rightContigId &&
                            pread.second.alignment.onForwardStrand == hole.rightComplemented
                    );
        }

        /**
         * Checks if aligned reads in pread are align on according contigs in right direction
         * @param hole
         * @param pread
         * @return
         */
        public static boolean preadIsNormalized(Hole hole, PairedMaybeAlignedDnaQWritable pread) {
            if (pread.first.isAligned) {
                if (!((pread.first.alignment.contigId == hole.leftContigId) &&
                    (pread.first.alignment.onForwardStrand != hole.leftComplemented))) {
                    return false;
                }
            }

            if (pread.second.isAligned) {
                if (!((pread.second.alignment.contigId == hole.rightContigId) &&
                    (pread.second.alignment.onForwardStrand == hole.rightComplemented))) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Sets read unaligned, if it's aligned on wrong contig or in wrong direction
         * @param hole
         * @param pread
         */
        public static void normalizePread(Hole hole, PairedMaybeAlignedDnaQWritable pread) {
            if (pread.first.isAligned) {
                if (!((pread.first.alignment.contigId == hole.leftContigId) &&
                        (pread.first.alignment.onForwardStrand != hole.leftComplemented))) {
                    pread.first.isAligned = false;
                }
            }

            if (pread.second.isAligned) {
                if (!((pread.second.alignment.contigId == hole.rightContigId) &&
                        (pread.second.alignment.onForwardStrand == hole.rightComplemented))) {
                    pread.second.isAligned = false;
                }
            }
        }

        /**
         * Reverse-complements reads in pread if to look like contigs in hole
         * are not reverse-complemented.
         *
         * @param hole
         * @param pread
         */
        public void orientRight(Hole hole, PairedMaybeAlignedDnaQWritable pread) {
            if (hole.leftComplemented && pread.first.isAligned) {
                pread.first.reverseComplement(contigs[0].sequence.length());
            }

            if (hole.rightComplemented && pread.second.isAligned) {
                pread.second.reverseComplement(contigs[1].sequence.length());
            }
        }

        /**
         * Fills the hole.
         *
         *
         * @param k length of k-mer to use
         * @param phredTrim value of phred such that reads are trimmed from the
         *                  right end to ensure that all qualities are at least
         *                  phreadTrim
         * @return pair of result (may be null) and filling statistis
         */
        private ImmutablePair<Filler, ru.ifmo.genetics.tools.irf.FillingReport> fillHole(int k, int phredTrim) {

            consensuses.clear();
            System.err.println("Filling using k = " + k + ", and phredTrim = " + phredTrim);
            WeightedDeBruijnGraph graph = new WeightedDeBruijnGraph(k, 1 <<  24, 2);
            // graph.addEdges(contigs[0].sequence);
            // graph.addEdges(contigs[1].sequence);

            int nreads = preads.size();
            // :ToDo: ignore positions far from the end
            for (int i = 0; i < nreads; ++i) {
                PairedMaybeAlignedDnaQWritable pread = preads.get(i);

                if (pread.first.isAligned) {
                    if (pread.first.alignment.offset >= minOffset0 - maxLength &&
                            pread.first.alignment.offset <= maxOffset0 + maxLength) {
                        graph.addEdges(pread.second.dnaq, phredTrim);
                    }
                    if (pread.first.alignment.offset >= minOffset0 &&
                            pread.first.alignment.offset <= maxOffset0 + maxLength) {
                        graph.addEdges(pread.first.dnaq, phredTrim);
                    }
                }

                if (pread.second.isAligned) {
                    if (pread.second.alignment.offset >= minOffset1 - maxLength &&
                            pread.second.alignment.offset <= maxOffset1 + maxLength) {
                        graph.addEdges(pread.first.dnaq, phredTrim);
                    }
                    if (pread.second.alignment.offset >= minOffset1 + maxLength &&
                            pread.second.alignment.offset <= maxOffset1) {
                        graph.addEdges(pread.second.dnaq, phredTrim);
                    }
                }
            }

//            graph.addEdges(new DnaView(contigs[0].sequence, Math.max(minOffset0 - maxLength, 0), Math.min(contigs[0].sequence.length(), maxOffset0 + maxLength)));
//            graph.addEdges(new DnaView(contigs[1].sequence, Math.max(minOffset1 - maxLength, 0), Math.min(contigs[1].sequence.length(), maxOffset1 + maxLength)));

            System.err.println(Arrays.toString(graph.countStat10()));
            GlobalContext context = new GlobalContext(
                    k,
                    graph,
                    minLength,
                    maxLength,
                    100,
                    null,
                    Collections.singletonList(Orientation.FR),
                    new NullDedicatedWriter< LightDna>(),
                    k / 4
            );

            FillingTask rfTask = new FillingTask(context, null);

            long sum = 0;
            long sumSq = 0;
            long count = 0;
            int totalRepaired = 0;


            if (key.isOpen()) {
                int tries = 0;
                Consensus extendingConsensus = new Consensus(null, 0.7, 2);
                for (int i = 0; i < nreads; ++i) {
                    PairedMaybeAlignedDnaQWritable pread = preads.get(i);
                    assert pread.first.isAligned;
                    assert !pread.second.isAligned;
                    if (pread.first.alignment.offset < minOffset0) {
                        continue;
                    }


                    LightDnaQ firstTail = new DnaQViewFromDna(
                            new DnaView(contigs[0].sequence,
                                    pread.first.alignment.offset + pread.first.dnaq.length(),
                                    contigs[0].sequence.length()),
                            (byte)18);

                    LightDnaQ leftHint = new ConcatenatingDnaQView(pread.first.dnaq, firstTail);
                    LightDnaQ rightHint = pread.second.dnaq;
                    FillingResult res = rfTask.fill(leftHint, rightHint, Orientation.FR);
                    tries++;
                    /*
                    if (key.isOpen() && key.leftContigId == 0) {
                        System.err.print(TextUtils.multiply(" ", pread.first.alignment.offset - minOffset0));
                        System.err.print(pread.first.dnaq.toString());
                        System.err.print(TextUtils.multiply(" ", (maxLength - minLength) / 2));
                        System.err.println(DnaView.rcView(pread.second.dnaq).toString());
                    }
                    */

                    if (res.dna != null) {
                        int leftOffset = contigs[0].sequence.length() - pread.first.alignment.offset - res.leftSkip;
                        int extendingLength = res.dna.length() - leftOffset;
                        if (extendingLength > 0) {
                            tempView.set(res.dna, leftOffset, extendingLength);
                        } else {
                            tempView.set(emptyDnaq, 0, 0);
                        }

                        // System.err.println("view:");
                        // System.err.println(DnaTools.toString(tempView));
                        extendingConsensus.addDna(tempView, 0);
                        /*
                        if (key.isOpen() && key.leftContigId == 0) {
                            System.err.print(TextUtils.multiply(" ", pread.first.alignment.offset + res.leftSkip - minOffset0));
                            System.err.println(res.dnaq.toString());
                        }
                        */
                    }

                    if (tries >= 100)
                        break;
                }
                System.err.println(rfTask.report.toString());

                // :ToDo: move to config
                int allowedErrors = 3;
                int outputLength = 0;
                for (int i = extendingConsensus.startIndex(); i < extendingConsensus.endIndex(); ++i, ++outputLength) {
                    byte nuc = extendingConsensus.get(i);
                    if (nuc < 0) {
                        allowedErrors--;
                        if (allowedErrors < 0) {
                            System.err.println("stopped because of errors on length " + outputLength + " of " + extendingConsensus.totalSize());
                            break;
                        }
                    }
                }


                if (outputLength > 20) {
                    outValue.distance = outputLength;
                    outValue.weight = (int) (nreads * extendingConsensus.getLayersNumber() / tries/ rfTask.report.processed.value());
                    outSequence.set(new DnaWritable(extendingConsensus.getDna(), 0, outputLength));
                    return ImmutablePair.make(outValue, rfTask.report);
                }


                return ImmutablePair.make(null, rfTask.report);
            }

            int tries = 0;
            int totalBridges = 0;
            for (int i = 0; i < nreads; ++i) {
                PairedMaybeAlignedDnaQWritable pread = preads.get(i);
                if (isPreadForFilling(pread, false))  {
                    totalBridges++;
                }
            }

            for (int i = 0; i < nreads; ++i) {
                PairedMaybeAlignedDnaQWritable pread = preads.get(i);
                if (!isPreadForFilling(pread, key.isOpen()))  {
                    continue;
                }

                LightDnaQ firstTail = new DnaQView(
                        contigs[0].sequence,
                        pread.first.alignment.offset + pread.first.dnaq.length(),
                        contigs[0].sequence.length());

                LightDnaQ secondTail = new DnaQView(
                        contigs[1].sequence,
                        0, Math.max(pread.second.alignment.offset, 0),
                        true, true);

                LightDnaQ leftHint = new ConcatenatingDnaQView(pread.first.dnaq, firstTail);
                LightDnaQ rightHint = new ConcatenatingDnaQView(pread.second.dnaq, secondTail);

//                System.err.println("Filling:");
//                System.err.println(leftHint);
//                System.err.println(DnaView.rcView(rightHint));

                FillingResult res = rfTask.fill(leftHint, rightHint, Orientation.FR);

                tries++;

                if (res.dna != null) {
                    /** offsets to hole
                     * |------------------>       |--------------->
                     *         |----->               <----|
                     * |>-----<|                  |><|          alignment offsets
                     *         |><|                    |><|     left and right skips
                     *            |>-----<|       |>--<|        left and right offsets to hole
                     */
                    int leftOffset = contigs[0].sequence.length() - pread.first.alignment.offset - res.leftSkip;
                    int rightOffset = pread.second.alignment.offset + pread.second.dnaq.length() - res.rightSkip;
                    int distance = res.dna.length() - leftOffset - rightOffset;

//                    System.err.println("distance: " + distance);


//                        System.err.println("found:");
//                        System.err.println(distance);
//                        System.err.println(pread);
//                        System.err.println(res.dnaq);

                    tempInt.setValue(distance);
                    if (!consensuses.containsKey(tempInt)) {
                        consensuses.put(new MutableInt(tempInt), new Consensus(null, 0.8));
                    }

                    // System.err.println("res:");
                    // System.err.println(DnaTools.toString(res.dnaq));

                    if (leftOffset >= 0 && rightOffset >= 0) {
                        if (distance > 0) {
                            tempView.set(res.dna, leftOffset, distance);
                        } else {
                            tempView.set(emptyDnaq, 0, 0);
                        }

                        // System.err.println("view:");
                        // System.err.println(DnaTools.toString(tempView));
                        consensuses.get(tempInt).addDna(tempView, 0);
                        totalRepaired++;
                    }

                    count++;
                    sum += distance;
                    sumSq += distance * distance;
                } else {
//                    System.err.println("not found");
                }

                if (tries >= 100)
                    break;
            }

            System.err.println(rfTask.report.toString());
            double averageDistance = sum / (double) count;
            System.err.println((sumSq * count - sum * sum) / (double) count / (count - 1));
            double stdDeviationDistance = Math.sqrt((sumSq * count - sum * sum) / (double) count / (count - 1));
            System.err.println("distance: " + averageDistance + "+-" + stdDeviationDistance);
            System.err.println("stat:");
            for (Map.Entry<MutableInt, Consensus> entry: consensuses.entrySet()) {
                System.err.println(entry.getKey() + " " + entry.getValue().getLayersNumber());
            }
            System.err.println("end of stat:");

            if (rfTask.report.ok.value() < 0.8 * (rfTask.report.processed.value() - rfTask.report.notFound.value())) {
                System.err.println("too little ok");
                return ImmutablePair.make(null, rfTask.report);
            }
            if (rfTask.report.ambiguous.value() > 10) {
                System.err.println("too ambiguous");
                return ImmutablePair.make(null, rfTask.report);
            }

            for (Map.Entry<MutableInt, Consensus> entry: consensuses.entrySet()) {
                Consensus consensus = entry.getValue();
                if (consensus.getLayersNumber() > totalRepaired * 0.8) {
                    outValue.distance = entry.getKey().intValue();
                    outValue.weight = (int) (consensus.getLayersNumber() * totalBridges / rfTask.report.processed.value());
                    System.err.println("distance: " + outValue.distance);
                    boolean isBadOverlap = false;
                    if (outValue.distance < 0) {
                        int errors = 0;
                        int shift = contigs[0].sequence.length() + outValue.distance;
                        int overlapStart = Math.max(0, shift);
                        int overlapEnd = Math.min(contigs[0].sequence.length(),
                                shift + contigs[1].sequence.length());
                        for (int i = overlapStart; i < overlapEnd; ++i) {
                            if (contigs[0].sequence.nucAt(i) != contigs[1].sequence.nucAt(i - shift) &&
                                    contigs[0].sequence.phredAt(i) > 2 &&
                                    contigs[1].sequence.phredAt(i - shift) > 2) {
                                errors++;
                            }
                        }
                        System.err.println("errors: " + errors);
                        System.err.println("ends: " + overlapStart + " " + overlapEnd);
                        if (errors / Math.max(100., overlapEnd - overlapStart) > 0.05) {
                            isBadOverlap = true;
                        }
                    }
                    if (isBadOverlap) {
                        continue;
                    }
                    outSequence.set(consensus.getDna());
                    return ImmutablePair.make(outValue, rfTask.report);
                }
            }
            System.err.println("can't select a distance");
            return ImmutablePair.make(null, rfTask.report);
        }

        private int minOffset0;
        private int maxOffset0;
        private int minOffset1;
        private int maxOffset1;

        private Hole key;
        private OutputCollector<Hole, Filler> output;
        private Reporter reporter;


        @Override
        public void reduce(Hole key, Iterator<ContigOrAlignment> values, OutputCollector<Hole, Filler> output, Reporter reporter) throws IOException {
            preads.clear();
            this.key = key;
            this.output = output;
            this.reporter = reporter;


            System.err.println("Filling: " + key);


            contigs[0].sequence.copyFieldsFrom(emptyDnaq);
            contigs[1].sequence.copyFieldsFrom(emptyDnaq);

            int contigsFound = 0;

            while (values.hasNext()) {
                ContigOrAlignment value = values.next();
                if (value.isFirst()) {
                    Contig contig = value.getFirst();
                    ++contigsFound;
                    if (contig.id == key.leftContigId && contigs[0].sequence.length() == 0) {
                        contigs[0].copyFieldsFrom(contig);
                        if (key.leftComplemented) {
                            contigs[0].sequence.reverse().complement();
                        }
                    } else {
                        assert contig.id == key.rightContigId;
                        contigs[1].copyFieldsFrom(contig);
                        if (key.rightComplemented) {
                            contigs[1].sequence.reverse().complement();
                        }
                    }
                    assert contigsFound <= 2;
                } else {
                    PairedMaybeAlignedDnaQWritable pread = value.getSecond();
                    // System.err.println("got: " + pread);
                    assert pread.first.isAligned || pread.second.isAligned : pread;
                    if (!preadIsWellOriented(key, pread)) {
                        pread.reverseComplement();
                    }

                    if (preadIsWellOriented(key, pread)) {
                        // Excluding strange microassembly (see bad example in test for preadIsWellOriented)
                        normalizePread(key, pread);
//                        System.err.println("add: " + pread);
                        preads.add(pread);
                    }
                }
            }
            System.err.println("added all");

            if (!key.isOpen() && contigsFound != 2) {
                System.err.println("no consensus");
                return;
            }

            if (key.isOpen() && contigsFound != 1) {
                System.err.println("found " + contigsFound + " contigs instead of 1");
                return;
            }

            if (key.isOpen()) {
                contigs[1].id = Hole.NONEXISTENT_CONTIG_ID;
            }
            
            int nreads = preads.size();


            PairedMaybeAlignedDnaQWritable tempPread = new PairedMaybeAlignedDnaQWritable();
            for (int i = 0; i < nreads; ++i) {
                PairedMaybeAlignedDnaQWritable pread = preads.get(i);
                tempPread.copyFieldsFrom(pread);
                orientRight(key, pread);
//                System.err.println("oriented: " + pread);
                if (isPreadForFilling(pread, false)) {
                    assert 
                        -pread.first.dnaq.length() < pread.first.alignment.offset &&
                        pread.first.alignment.offset < contigs[0].sequence.length() &&
                        -pread.second.dnaq.length() < pread.second.alignment.offset &&
                        pread.second.alignment.offset < contigs[1].sequence.length() 
                        : "\n" + tempPread + "\n" + pread + "\n" + contigs[0].sequence.length() + " " + contigs[1].sequence.length();
                }
            }
//            System.err.println("oriented all");

            minOffset0 = Integer.MAX_VALUE;
            maxOffset0 = Integer.MIN_VALUE;
            minOffset1 = Integer.MAX_VALUE;
            maxOffset1 = Integer.MIN_VALUE;


            for (int i = 0; i < nreads; ++i) {
                PairedMaybeAlignedDnaQWritable pread = preads.get(i);
                if (!isPreadForFilling(pread, key.isOpen())) {
                    continue;
                }

                if (pread.first.isAligned) {
                    minOffset0 = Math.min(minOffset0, pread.first.alignment.offset);
                    maxOffset0 = Math.max(maxOffset0, pread.first.alignment.offset);
                }

                if (pread.second.isAligned) {
                    minOffset1 = Math.min(minOffset1, pread.second.alignment.offset);
                    maxOffset1 = Math.max(maxOffset1, pread.second.alignment.offset);
                }

            }
            
            System.err.println("offsets");
            System.err.println(minOffset0 + " " + maxOffset0);
            System.err.println(minOffset1 + " " + maxOffset1);

            if (key.isOpen()) {
                maxOffset0 = contigs[0].sequence.length();
                minOffset0 = maxOffset0 - maxLength;
            }

            int minK = 11;
            int maxK = K;
            int minPhredTrim = 0;
            int maxPhredTrim = 20;
            int curK = maxK;
            int curPhredTrim = minPhredTrim;
            for (int i = 0; i < 1; ++i) {
                ImmutablePair<Filler, FillingReport> fillingResult = fillHole(curK, curPhredTrim);
                if (fillingResult.first() != null) {
                    output.collect(key, fillingResult.first());
                    break;
                }
                /*
                FillingReport stat = fillingResult.second();
                if (stat.tooPolymorphic.value() > stat.notFound.value()) {
                    curPhredTrim = maxPhredTrim;
                } else {
                    curK = minK;
                }
                */
                curK = minK;
            }

        }

        private boolean isPreadForFilling(PairedMaybeAlignedDnaQWritable pread, boolean keyIsOpen) {
            return pread.first.isAligned && (keyIsOpen || pread.second.isAligned);
        }
    }
    
    public static void fill(Path holesPath, int minLength, int maxLength, Path destination) throws IOException {
        log.info("Starting filling holes");
        JobConf conf = new JobConf(FindHoles.class);

        conf.setJobName("Filling holes");

        conf.set(MIN_LENGTH, String.valueOf(minLength));
        conf.set(MAX_LENGTH, String.valueOf(maxLength));

        conf.setMapOutputKeyClass(Hole.class);
        conf.setMapOutputValueClass(ContigOrAlignment.class);
        conf.setOutputKeyClass(Hole.class);
        conf.setOutputValueClass(DnaWritable.class);


        MultipleInputs.addInputPath(conf, holesPath, SequenceFileInputFormat.class, IdentityMapper.class);
        conf.setReducerClass(Reduce.class);

        conf.setOutputFormat(TextOutputFormat.class);

        FileOutputFormat.setOutputPath(conf, destination);

        JobClient.runJob(conf);
        log.info("Filling holes finished");
    }
}
