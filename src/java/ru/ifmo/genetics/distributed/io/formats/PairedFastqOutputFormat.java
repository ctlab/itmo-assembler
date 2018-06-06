package ru.ifmo.genetics.distributed.io.formats;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.lib.MultipleOutputFormat;
import org.apache.hadoop.util.Progressable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.dna.DnaTools;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class PairedFastqOutputFormat extends MultipleOutputFormat<Int128WritableComparable, PairedDnaQWritable> {
    @Override
    public RecordWriter<Int128WritableComparable, PairedDnaQWritable> getRecordWriter(
            FileSystem ignored, JobConf job, String name, Progressable progress) throws IOException {

        System.err.println("getting record writer");
        Path file1 = FileOutputFormat.getTaskOutputPath(job, name + "_1");
        Path file2 = FileOutputFormat.getTaskOutputPath(job, name + "_2");
        FileSystem fs1 = file1.getFileSystem(job);
        FileSystem fs2 = file2.getFileSystem(job);
        FSDataOutputStream fileOut1 = fs1.create(file1, progress);
        FSDataOutputStream fileOut2 = fs2.create(file2, progress);
        return new PairedFastqRecordWriter(fileOut1, fileOut2);
    }

    /*
     * never used
     */
    @Override
    protected RecordWriter<Int128WritableComparable, PairedDnaQWritable> getBaseRecordWriter(
            FileSystem fileSystem, JobConf entries, String s, Progressable progressable) throws IOException {
        return null;
    }

    private static class PairedFastqRecordWriter implements RecordWriter<Int128WritableComparable, PairedDnaQWritable> {
        private PrintWriter out1, out2;
        int count = 0;

        public PairedFastqRecordWriter(DataOutputStream out1, DataOutputStream out2) {
            this.out1 = new PrintWriter(out1);
            this.out2 = new PrintWriter(out2);
        }

        @Override
        public void write(Int128WritableComparable key, PairedDnaQWritable value) throws IOException {
            out1.printf("@%s:%d\n", key.toString(), count);
            out1.println(value.first);
            out1.printf("+%s:%d\n", key.toString(), count++);
            out1.println(value.first.toPhredString());

            out2.printf("@%s:%d\n", key.toString(), count);
            out2.println(value.second);
            out2.printf("+%s:%d\n", key.toString(), count++);
            out2.println(value.second.toPhredString());
        }

        @Override
        public void close(Reporter reporter) throws IOException {
            out1.close();
            out2.close();
        }
    }
}
