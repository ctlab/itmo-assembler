package ru.ifmo.genetics.distributed.quasicontigsAssembly.task;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.PairedDnaQWithIdWritable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.IDnaQ;
import ru.ifmo.genetics.statistics.reporter.HadoopReporter;
import ru.ifmo.genetics.structures.debriujn.CompactDeBruijnGraph;
import ru.ifmo.genetics.tools.rf.task.FillingTask;
import ru.ifmo.genetics.tools.rf.task.GlobalContext;
import ru.ifmo.genetics.tools.rf.Orientation;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: alserg
 * Date: 03.11.11
 * Time: 10:22
 */
public class ReadsFillerTask {

    private static final int DEFAULT_K = 17;
    private static final String PERCENT_OF_READS_TO_SAVE = "PERCENT_OF_READS_TO_SAVE";
    private static final String DEFAULT_PERCENT_OF_READS_TO_SAVE = "1.0";


    private static class Reduce extends MapReduceBase
            implements Reducer<ComponentID, PairedDnaQWithIdWritable, Int128WritableComparable, DnaWritable> {

        private final Queue<PairedDnaQWithIdWritable> pool = new ArrayDeque<PairedDnaQWithIdWritable>();

        Random random = new Random(1);
        Int128WritableComparable outKey = new Int128WritableComparable();
        CompactDeBruijnGraph smallGraph = new CompactDeBruijnGraph(DEFAULT_K, 1 << 20);

        @Override
        public void reduce(ComponentID key, Iterator<PairedDnaQWithIdWritable> values, OutputCollector<Int128WritableComparable, DnaWritable> output, Reporter reporter) throws IOException {
            System.err.println("map started: key = " + key);
            System.err.println(Runtime.getRuntime().freeMemory());

            ArrayList<PairedDnaQWithIdWritable> pairedReads = new ArrayList<PairedDnaQWithIdWritable>();
            int totalLength = 0;

            while (values.hasNext()) {
                PairedDnaQWithIdWritable pairedDnaQWithIdWritable;
                if (pool.isEmpty()) {
                    pairedDnaQWithIdWritable = new PairedDnaQWithIdWritable();
                } else {
                    pairedDnaQWithIdWritable = pool.poll();
                }
                pairedDnaQWithIdWritable.setFieldsFrom(values.next());
                pairedReads.add(pairedDnaQWithIdWritable);
                totalLength += pairedDnaQWithIdWritable.second().first.length() + pairedDnaQWithIdWritable.second().second.length();
                /*                
                if (pairedReads.calculateSize() > 2000) {
                    //TODO REMOVE
                    pool.addAll(pairedReads);
                    return;
                }
                */
            }
            System.err.println("graph creating started");

            CompactDeBruijnGraph graph;
            if (pairedReads.size() < 1000) {
                graph = smallGraph;
                graph.reset();
            } else {
                graph = new CompactDeBruijnGraph(DEFAULT_K, totalLength * 8);
            }
            System.err.println("graph created");

            for (PairedDnaQWithIdWritable pdnaq : pairedReads) {
                graph.addEdges(pdnaq.second().first);
                graph.addEdges(pdnaq.second().second);
            }

            System.err.println("graph built, edges size: " + graph.edgesSize());

            GlobalContext context = new GlobalContext(
                    null,
                    null,
                    DEFAULT_K,
                    150,
                    450,
                    graph,
                    Arrays.asList(Orientation.FR), 0, Long.MAX_VALUE,
                    new HadoopReporter(reporter)
            );

            FillingTask rfTask = new FillingTask(context, null);


            ArrayList<Dna> quasiContigs = new ArrayList<Dna>();
            ArrayList<Int128WritableComparable> keys = new ArrayList<Int128WritableComparable>();
            for (PairedDnaQWithIdWritable p : pairedReads) {
                outKey.copyFieldsFrom(p.first());
                FillingTask.FillingResult fillingResult = rfTask.fillRead(p.second().first, p.second().second);
                if (fillingResult != null) {
                    IDnaQ res = fillingResult.dnaq;
                    if (res != null) {
                        keys.add(p.first());
                        quasiContigs.add(new Dna(res));
                    }
                }
            }
            System.err.println("filling finished");
            try {
                URL configURL = ClassLoader.getSystemResource("config.properties");
                Configuration config = new PropertiesConfiguration(configURL);
//                ContigsAssembler ca = new ContigsAssembler(config);
//                ca.assemble(quasiContigs, output, 1);
            } catch (ConfigurationException e) {
                throw new RuntimeException(e);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
            }

//            assert quasiContigs.size() == keys.size();
//            for (int i = 0; i < quasiContigs.size(); i++) {
//                output.collect(keys.get(i), new DnaWritable(quasiContigs.get(i)));
//            }

            rfTask.printStat();
            rfTask.updateCounters();
            pool.addAll(pairedReads);
        }


    }


    public static void main(String[] args) throws Exception {
        fillReads(new Path(args[0]), new Path(args[1]));
    }

    public static void fillReads(Path source, Path target) throws IOException {
        JobConf conf = new JobConf(ReadsFillerTask.class);

        conf.setJobName("FillingReads");

        conf.setOutputKeyClass(Int128WritableComparable.class);
        conf.setOutputValueClass(DnaWritable.class);
        conf.setMapOutputKeyClass(ComponentID.class);
        conf.setMapOutputValueClass(PairedDnaQWithIdWritable.class);


        conf.setMapperClass(IdentityMapper.class);
        conf.setReducerClass(Reduce.class);

        conf.setInputFormat(SequenceFileInputFormat.class);
//        conf.setOutputFormat(FastaOutputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(conf, source);
        FileOutputFormat.setOutputPath(conf, target);

        JobClient.runJob(conf);
    }


    private static class UniqueReduce extends MapReduceBase
            implements Reducer<Int128WritableComparable, DnaWritable, Int128WritableComparable, DnaWritable> {
        double percentOfReadsToSave;
        Random random = new Random();

        @Override
        public void configure(JobConf job) {
            percentOfReadsToSave = Double.parseDouble(job.get(PERCENT_OF_READS_TO_SAVE, DEFAULT_PERCENT_OF_READS_TO_SAVE));
        }

        @Override
        public void reduce(Int128WritableComparable key, Iterator<DnaWritable> values, OutputCollector<Int128WritableComparable, DnaWritable> output, Reporter reporter) throws IOException {
            if (random.nextDouble() < percentOfReadsToSave) {
                output.collect(key, values.next());
            }
            int count = 1;
            if (values.hasNext()) {
                values.next();
                count++;
            }
            if (count > 1) {
                reporter.getCounter("statistics", "number of nonunique quasi-contigs").increment(1);
            }
        }
    }

    public static void makeFilledReadsUniqueAndDropPart(Path nonUniqueResult, Path result, double percentOfReadsToSave) throws IOException {
        JobConf conf = new JobConf(ReadsFillerTask.class);

        conf.setJobName("making filled reads unique");
        conf.set(PERCENT_OF_READS_TO_SAVE, "" + percentOfReadsToSave);

        conf.setOutputKeyClass(Int128WritableComparable.class);
        conf.setOutputValueClass(DnaWritable.class);


        conf.setMapperClass(IdentityMapper.class);
        conf.setReducerClass(UniqueReduce.class);

        conf.setInputFormat(SequenceFileInputFormat.class);
//        conf.setOutputFormat(FastaOutputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(conf, nonUniqueResult);
        FileOutputFormat.setOutputPath(conf, result);

        JobClient.runJob(conf);

    }

}
