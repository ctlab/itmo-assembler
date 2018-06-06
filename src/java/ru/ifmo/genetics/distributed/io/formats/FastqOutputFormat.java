package ru.ifmo.genetics.distributed.io.formats;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.Progressable;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.dna.DnaTools;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class FastqOutputFormat extends FileOutputFormat<Int128WritableComparable, DnaQWritable> {
    @Override
    public RecordWriter<Int128WritableComparable, DnaQWritable> getRecordWriter(
            FileSystem ignored, JobConf job, String name, Progressable progress) throws IOException {

        System.err.println("getting record writer");
        Path file = FileOutputFormat.getTaskOutputPath(job, name);
        FileSystem fs = file.getFileSystem(job);
        FSDataOutputStream fileOut = fs.create(file, progress);
        return new FastqRecordWriter(fileOut);
    }

    private static class FastqRecordWriter implements RecordWriter<Int128WritableComparable, DnaQWritable> {
        private PrintWriter out;
        int count = 0;

        public FastqRecordWriter(DataOutputStream out) {
            this.out = new PrintWriter(out);
        }

        @Override
        public void write(Int128WritableComparable key, DnaQWritable value) throws IOException {
            out.printf(">%s:%d\n", key.toString(), count);
            out.println(value);
            out.printf("+%s:%d\n", key.toString(), count++);
            out.println(value.toPhredString());
        }

        @Override
        public void close(Reporter reporter) throws IOException {
            out.close();
        }
    }
}
