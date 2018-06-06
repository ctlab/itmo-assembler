package ru.ifmo.genetics.distributed.clusterization.bipartite;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import ru.ifmo.genetics.distributed.clusterization.types.Kmer;
import ru.ifmo.genetics.distributed.clusterization.types.Vertex;
import ru.ifmo.genetics.distributed.clusterization.types.VertexOrKmerOrComponentIDWritable;
import ru.ifmo.genetics.distributed.clusterization.types.VertexOrKmerWritableComparable;
import ru.ifmo.genetics.distributed.io.KmerIterable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.utils.KmerUtils;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;

/**
 * Author: Sergey Melnikov
 */

public class IndexesTask {
    private IndexesTask() {
    }

    private static final String KMER_LENGTH = "kmerLength";
    private static final String DEFAULT_KMER_LENGTH = "17";
    private static final String MAX_KMER_NEIGHBOURS = "MAX_KMER_NEIGHBOURS";
    private static final String DEFAULT_MAX_KMER_NEIGHBOURS = "100";
    private static final String MIN_KMER_NEIGHBOURS = "MIN_KMER_NEIGHBOURS";
    private static final String DEFAULT_MIN_KMER_NEIGHBOURS = "0";

    /**
     * @param reads      directory with type <Int128WritableComparable, A>
     * @param index      directory with logical type <Vertex, Kmer>
     *                   and physical type <VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable>
     * @param kmerLength kmer length
     */
    public static void makeIndex(Path reads, Path index, int kmerLength) throws IOException {
        JobConf conf = new JobConf(IndexesTask.class);
        conf.set(KMER_LENGTH, "" + kmerLength);
        conf.setJobName("make direct index");

        conf.setOutputKeyClass(VertexOrKmerWritableComparable.class);
        conf.setOutputValueClass(VertexOrKmerOrComponentIDWritable.class);

        conf.setMapperClass(MakeIndexMap.class);
        conf.setReducerClass(IdentityReducer.class);


        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(conf, reads);
        FileOutputFormat.setOutputPath(conf, index);

        JobClient.runJob(conf);
    }

    private static class MakeIndexMap<A extends KmerIterable> extends MapReduceBase implements
            Mapper<Int128WritableComparable, A, VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable> {

        private int kmerLength;
        private VertexOrKmerWritableComparable outKey = new VertexOrKmerWritableComparable();
        private VertexOrKmerOrComponentIDWritable outValue = new VertexOrKmerOrComponentIDWritable();
        private Vertex vertex = new Vertex();
        private Kmer outKmer = new Kmer();

        @Override
        public void configure(JobConf job) {
            this.kmerLength = Integer.parseInt(job.get(KMER_LENGTH, DEFAULT_KMER_LENGTH));
        }

        @Override
        public void map(Int128WritableComparable key, A value,
                        OutputCollector<VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable> output,
                        Reporter reporter) throws IOException {
            Iterator<Kmer> kmerIterator = value.kmerIterator(kmerLength);
            outKey.setFirst(vertex);
            vertex.copyFieldsFrom(key);
            outValue.setSecond(outKmer);
            while (kmerIterator.hasNext()) {
                Kmer kmer = kmerIterator.next();
                outKmer.set(KmerUtils.getKmerKey(kmer.get(), kmerLength));
                output.collect(outKey, outValue);
            }
        }
    }


    /**
     * @param reads             directory with <Int128WritableComparable, A>
     * @param reverseIndex      directory with logical type <Kmer, Vertex>
     *                          and physical type <VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable>
     * @param kmerLength        kmer length
     * @param minKmerNeighbours minimal number of kmer neighbours, for output they
     * @param maxKmerNeighbours maximal number of kmer neighbours, for output they
     */
    public static void makeReverseIndex(Path reads, Path reverseIndex, int kmerLength,
                                        int minKmerNeighbours, int maxKmerNeighbours) throws IOException {
        JobConf conf = new JobConf(IndexesTask.class);
        conf.set(KMER_LENGTH, "" + kmerLength);
        conf.set(MAX_KMER_NEIGHBOURS, "" + maxKmerNeighbours);
        conf.set(MIN_KMER_NEIGHBOURS, "" + minKmerNeighbours);

        conf.setJobName("make reverse index");

        conf.setOutputKeyClass(VertexOrKmerWritableComparable.class);
        conf.setOutputValueClass(VertexOrKmerOrComponentIDWritable.class);

        conf.setMapOutputKeyClass(Kmer.class);
        conf.setMapOutputValueClass(Vertex.class);

        conf.setMapperClass(MakeReverseIndexMap.class);
        conf.setReducerClass(MakeReverseIndexReduce.class);


        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(conf, reads);
        FileOutputFormat.setOutputPath(conf, reverseIndex);

        JobClient.runJob(conf);
    }

    private static class MakeReverseIndexMap<A extends KmerIterable> extends MapReduceBase implements
            Mapper<Int128WritableComparable, A, Kmer, Vertex> {

        private int kmerLength;
        private Vertex vertex = new Vertex();
        private Kmer outKmer = new Kmer();

        @Override
        public void configure(JobConf job) {
            this.kmerLength = Integer.parseInt(job.get(KMER_LENGTH, DEFAULT_KMER_LENGTH));
        }

        @Override
        public void map(Int128WritableComparable key, A value,
                        OutputCollector<Kmer, Vertex> output,
                        Reporter reporter) throws IOException {
            Iterator<Kmer> kmerIterator = value.kmerIterator(kmerLength);
            vertex.copyFieldsFrom(key);
            while (kmerIterator.hasNext()) {
                Kmer kmer = kmerIterator.next();
                outKmer.set(KmerUtils.getKmerKey(kmer.get(), kmerLength));
                output.collect(outKmer, vertex);
            }
        }
    }

    private static class MakeReverseIndexReduce extends MapReduceBase implements
            Reducer<Kmer, Vertex, VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable> {
        private VertexOrKmerWritableComparable outKey = new VertexOrKmerWritableComparable();
        private VertexOrKmerOrComponentIDWritable outValue = new VertexOrKmerOrComponentIDWritable();
        private Queue<Vertex> pool = new ArrayDeque<Vertex>();
        private int maxKmerNeighbours;
        private int minKmerNeighbours;

        private ArrayList<Vertex> vertexes = new ArrayList<Vertex>();
        
        @Override
        public void configure(JobConf job) {
            maxKmerNeighbours = Integer.parseInt(job.get(MAX_KMER_NEIGHBOURS, DEFAULT_MAX_KMER_NEIGHBOURS));
            minKmerNeighbours = Integer.parseInt(job.get(MIN_KMER_NEIGHBOURS, DEFAULT_MIN_KMER_NEIGHBOURS));
        }

        @Override
        public void reduce(Kmer key, Iterator<Vertex> values,
                           OutputCollector<VertexOrKmerWritableComparable, VertexOrKmerOrComponentIDWritable> output,
                           Reporter reporter) throws IOException {
            pool.addAll(vertexes);
            vertexes.clear();
            while (values.hasNext()) {
                Vertex newVertex;
                if (!pool.isEmpty()) {
                    newVertex = pool.poll();
                } else {
                    newVertex = new Vertex();
                }
                newVertex.copyFieldsFrom(values.next());
                vertexes.add(newVertex);
                
                if (vertexes.size() > maxKmerNeighbours) {
                    return;
                }
            }
            if (vertexes.size() < minKmerNeighbours) {
                return;
            }
            assert vertexes.size() <= maxKmerNeighbours;
            outKey.setSecond(key);
            for (Vertex vertex : vertexes) {
                outValue.setFirst(vertex);
                output.collect(outKey, outValue);
            }
        }
    }
}
