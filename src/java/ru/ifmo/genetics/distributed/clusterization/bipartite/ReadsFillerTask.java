package ru.ifmo.genetics.distributed.clusterization.bipartite;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.clusterization.types.PairedDnaQWithIdWritable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.dna.IDnaQ;
import ru.ifmo.genetics.statistics.reporter.HadoopReporter;
import ru.ifmo.genetics.structures.debriujn.CompactDeBruijnGraph;
import ru.ifmo.genetics.tools.rf.task.FillingTask;
import ru.ifmo.genetics.tools.rf.task.GlobalContext;
import ru.ifmo.genetics.tools.rf.Orientation;

import java.io.IOException;
import java.util.*;
import java.util.Queue;

/**
 * User: alserg
 * Date: 03.11.11
 * Time: 10:22
 */
public class ReadsFillerTask {

    private static final int DEFAULT_K = 17;

    private static class Reduce extends MapReduceBase
            implements Reducer<ComponentID, PairedDnaQWithIdWritable, Int128WritableComparable, DnaWritable> {
        private final Queue<PairedDnaQWithIdWritable> pool = new ArrayDeque<PairedDnaQWithIdWritable>();

        @Override
        public void reduce(ComponentID key, Iterator<PairedDnaQWithIdWritable> values,
                           OutputCollector<Int128WritableComparable, DnaWritable> output,
                           Reporter reporter) throws IOException {

            System.err.println("map started");
            System.err.println(Runtime.getRuntime().freeMemory());
            ArrayList<PairedDnaQWithIdWritable> pairedReads = new ArrayList<PairedDnaQWithIdWritable>();
            while (values.hasNext()) {
                PairedDnaQWithIdWritable pairedDnaQWithIdWritable;
                if (pool.isEmpty()) {
                    pairedDnaQWithIdWritable = new PairedDnaQWithIdWritable();
                } else {
                    pairedDnaQWithIdWritable = pool.poll();
                }
                pairedDnaQWithIdWritable.setFieldsFrom(values.next());
                pairedReads.add(pairedDnaQWithIdWritable);
            }

            System.err.println("graph creating started");
            System.err.println("graph creating started");
            CompactDeBruijnGraph graph = new CompactDeBruijnGraph(DEFAULT_K, 16 << 20);
            System.err.println("graph created ");
            System.err.println("graph created");

            for (PairedDnaQWithIdWritable pdnaq : pairedReads) {
                graph.addEdges(pdnaq.second.first);
                graph.addEdges(pdnaq.second.second);
            }

            System.err.println("graph built: " + graph.edgesSize());
            System.err.println("graph built, edges calculateSize: " + graph.edgesSize());

            GlobalContext context = new GlobalContext(
                    null,
                    null,
                    DEFAULT_K,
                    10,
                    500,
                    graph,
                    Arrays.asList(Orientation.FR), 0, Long.MAX_VALUE,
                    new HadoopReporter(reporter)
            );

            FillingTask rfTask = new FillingTask(context, null);

            for (PairedDnaQWithIdWritable p : pairedReads) {
                IDnaQ res = rfTask.fillRead(p.second.first, p.second.second).dnaq;
                if (res != null) {
                    output.collect(p.first, new DnaWritable(res));
                }
            }
            pool.addAll(pairedReads);
            rfTask.printStat();
            rfTask.updateCounters();
        }
    }

    /**
     * @param source directory with <ComponentID, PairedDnaQWithIdWritable>
     * @param result directory with <Int128WritableComparable, DnaWritable>
     */
    public static void fillReads(Path source, Path result) throws IOException {
        JobConf conf = new JobConf(ReadsFillerTask.class);

        conf.setJobName("fill reads task");

        conf.setOutputKeyClass(Int128WritableComparable.class);
        conf.setOutputValueClass(DnaWritable.class);

        conf.setMapOutputKeyClass(ComponentID.class);
        conf.setMapOutputValueClass(PairedDnaQWithIdWritable.class);

        conf.setMapperClass(IdentityMapper.class);
        conf.setReducerClass(Reduce.class);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(conf, source);
        FileOutputFormat.setOutputPath(conf, result);

        JobClient.runJob(conf);
    }

}
