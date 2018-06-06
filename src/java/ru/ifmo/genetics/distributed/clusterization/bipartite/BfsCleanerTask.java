package ru.ifmo.genetics.distributed.clusterization.bipartite;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import ru.ifmo.genetics.distributed.clusterization.types.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BfsCleanerTask {
    public static final String MAXIMUM_SIZE = "MAXIMUM_SIZE";
    public static final String DEFAULT_MAXIMUM_SIZE = "1000000";

    /**
     * @param sourceComponents directory with logical type <Vertex|Kmer, ComponentID>
     *                         and physical type <VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable>
     * @param resultComponents directory with directory with logical type <Vertex|Kmer, ComponentID>
     *                         and physical type <VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable>
     * @param maximumSize      maximum size of component to save
     */
    public static void dropLargeComponents(Path sourceComponents, Path resultComponents, int maximumSize) throws IOException {
        final JobConf conf = new JobConf(BfsCleanerTask.class);
        conf.setJobName("Clean BFS data");
        conf.set(MAXIMUM_SIZE, "" + maximumSize);

        FileInputFormat.setInputPaths(conf, sourceComponents);
        FileOutputFormat.setOutputPath(conf, resultComponents);

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setMapOutputKeyClass(ComponentID.class);
        conf.setMapOutputValueClass(VertexOrKmerWritableComparable.class);

        conf.setOutputKeyClass(VertexOrKmerWritableComparable.class);
        conf.setOutputValueClass(VertexOrKmerOrComponentIDWritable.class);

        conf.setMapperClass(CleanerMapper.class);
        conf.setReducerClass(CleanerReducer.class);

        JobClient.runJob(conf);
    }

    private static class CleanerMapper extends MapReduceBase implements Mapper<VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable, ComponentID, VertexOrKmerWritableComparable> {
        ComponentID componentID = new ComponentID();

        @Override
        public void map(VertexOrKmerWritableComparable key, VertexOrKmerOrComponentIDWritable value, OutputCollector<ComponentID, VertexOrKmerWritableComparable> output, Reporter reporter) throws IOException {
            componentID.copyFieldsFrom(value.getThird());
            output.collect(componentID, key);
        }
    }

    private static class CleanerReducer extends MapReduceBase implements Reducer<ComponentID, VertexOrKmerWritableComparable, VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable> {
        Set<Vertex> vertexes = new HashSet<Vertex>();
        Set<Kmer> kmers = new HashSet<Kmer>();
        int maximumSize;

        @Override
        public void configure(JobConf job) {
            maximumSize = Integer.parseInt(job.get(MAXIMUM_SIZE, DEFAULT_MAXIMUM_SIZE));
        }

        VertexOrKmerWritableComparable outKey = new VertexOrKmerWritableComparable();
        VertexOrKmerOrComponentIDWritable outValue = new VertexOrKmerOrComponentIDWritable();

        @Override
        public void reduce(ComponentID key, Iterator<VertexOrKmerWritableComparable> values, OutputCollector<VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable> output, Reporter reporter) throws IOException {
            vertexes.clear();
            kmers.clear();
            while (values.hasNext()) {
                VertexOrKmerWritableComparable vertexOrKmerWritableComparable = values.next();
                if (vertexOrKmerWritableComparable.isFirst()) {
                    vertexes.add(new Vertex(vertexOrKmerWritableComparable.getFirst()));
                    if (vertexes.size() > maximumSize) {
                        System.err.println("drop " + key + "kmers.size() = " + kmers.size());
                        return;
                    }
                } else {
                    kmers.add(new Kmer(vertexOrKmerWritableComparable.getSecond()));
                }
            }
            System.err.println("save vertexes.size() = " + vertexes.size() + ", kmers.size() = " + kmers.size());
            outValue.setThird(key);
            for (Vertex vertex : vertexes) {
                outKey.setFirst(vertex);
                output.collect(outKey, outValue);
            }
            for (Kmer kmer : kmers) {
                outKey.setSecond(kmer);
                output.collect(outKey, outValue);
            }
        }
    }
}
