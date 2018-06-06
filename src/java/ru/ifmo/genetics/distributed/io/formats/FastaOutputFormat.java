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
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class FastaOutputFormat extends FileOutputFormat<Text, DnaWritable> {
    @Override
    public RecordWriter<Text, DnaWritable> getRecordWriter(FileSystem ignored, JobConf job, String name, Progressable progress) throws IOException {

        System.err.println("getting record writer");
        Path file = FileOutputFormat.getTaskOutputPath(job, name);
        FileSystem fs = file.getFileSystem(job);
        FSDataOutputStream fileOut = fs.create(file, progress);
        return new FastaRecordWriter(fileOut);
    }

    private static class FastaRecordWriter implements RecordWriter<Text, DnaWritable> {
        private PrintWriter out;
        int count = 0;

        public FastaRecordWriter(DataOutputStream out) {
            this.out = new PrintWriter(out);
        }

        @Override
        public void write(Text key, DnaWritable value) throws IOException {
            out.printf(">%s:%d\n", key.toString(), count++);
            out.println(value);
        }

        @Override
        public void close(Reporter reporter) throws IOException {
            out.close();
        }
    }
}
