package ru.ifmo.genetics.distributed.contigsAssembly.tasks;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import ru.ifmo.genetics.distributed.clusterization.bipartite.MakeTextOutputTask;
import ru.ifmo.genetics.distributed.clusterization.types.ComponentID;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.util.JobUtils;
import ru.ifmo.genetics.dna.Dna;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class for launching contigs assembly as a Hadoop task.
 */
public class ContigsAssemblyTask {
    public static class Reduce extends MapReduceBase
            implements Reducer<ComponentID, DnaWritable, Int128WritableComparable, DnaWritable> {
        @Override
        public void reduce(ComponentID key, Iterator<DnaWritable> values,
                           OutputCollector<Int128WritableComparable, DnaWritable> output, Reporter reporter) throws IOException {
            reporter.setStatus("Loading");
            ArrayList<Dna> reads = new ArrayList<Dna>();
            while (values.hasNext()) {
                reads.add(new Dna(values.next()));
            }

            try {
                URL configURL = ClassLoader.getSystemResource("config.properties");
                Configuration config = new PropertiesConfiguration(configURL);

                reporter.setStatus("Assembly");
//                ContigsAssembler ca = new ContigsAssembler(config);
//                ca.assemble(reads, output);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            reporter.setStatus("Done");
        }
    }

    public static void main(String[] args) throws Exception {
        Path source = new Path(args[0]);
        Path workDir = new Path(args[1]);
        makeContigsAssembly(source, workDir);
    }

    private static Path makeContigsAssembly(Path source, Path workDir) throws IOException {
        Path result = new Path(workDir, "result");
        Path textResult = new Path(workDir, "result_text");
        if (!JobUtils.jobSucceededOrRemove(result)) {
            assembleContigs(source, result);
        }
        if (!JobUtils.jobSucceededOrRemove(textResult)) {
            MakeTextOutputTask.makeTextOutput(result, textResult, Int128WritableComparable.class, DnaWritable.class);
        }
        return result;
    }

    public static void assembleContigs(Path source, Path result) throws IOException {
        JobConf conf = new JobConf(ContigsAssemblyTask.class);
        conf.setJobName("Contigs Assembly");


        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setMapOutputKeyClass(ComponentID.class);
        conf.setMapOutputValueClass(DnaWritable.class);

        conf.setOutputKeyClass(Int128WritableComparable.class);
        conf.setOutputValueClass(DnaWritable.class);

        conf.setMapperClass(IdentityMapper.class);
        conf.setReducerClass(Reduce.class);


        FileInputFormat.setInputPaths(conf, source);
        FileOutputFormat.setOutputPath(conf, result);

        JobClient.runJob(conf);
    }
}
