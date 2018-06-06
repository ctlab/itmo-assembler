package ru.ifmo.genetics.distributed.io.formats;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.LineReader;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: alserg
 * Date: 02.11.11
 * Time: 16:41
 */
public class WholePairedFastqInputFormat extends FileInputFormat<Text, ArrayWritable> {
    @Override
    protected boolean isSplitable(FileSystem fs, Path filename) {
        return false;
    }

    @Override
    public RecordReader<Text, ArrayWritable> getRecordReader(InputSplit split, JobConf job, Reporter reporter) throws IOException {
        reporter.setStatus(split.toString());
        return new WholePairedFastqRecordReader(job, (FileSplit) split);
    }

    /**
     * Created by IntelliJ IDEA.
     * User: alserg
     * Date: 02.11.11
     * Time: 16:54
     */
    public static class WholePairedFastqRecordReader implements RecordReader<Text, ArrayWritable> {
        int size;
        PairedFastqInputFormat.PairedFastqRecordReader pairedFastqRecordReader = null;
        boolean read = false;
        String filename;
        public WholePairedFastqRecordReader(JobConf job, FileSplit split) throws IOException {
            final Path file = split.getPath();
            filename = file.toString();
            final FileSystem fs = file.getFileSystem(job);
            FSDataInputStream fileIn = fs.open(file);
            LineReader lineReader = new LineReader(fileIn, job);
            size = 0;
            long start = split.getStart();
            long end = start + split.getLength();
            Text line = new Text();

            do {
                start += lineReader.readLine(line);
                if (line.getLength() > 0 && line.charAt(0) == '@' && line.charAt(line.getLength() - 1) != '1') {
                    size++;
                }

            } while (start < end);
            lineReader.close();
            System.err.println("free memory after counting: " + Runtime.getRuntime().freeMemory());
            pairedFastqRecordReader = new PairedFastqInputFormat.PairedFastqRecordReader(job, split);
        }

        @Override
        public boolean next(Text key, ArrayWritable value) throws IOException {
            if (read) {
                return false;
            }
            System.err.println("free memory before next: " + Runtime.getRuntime().freeMemory());

            key.set(filename);
            PairedDnaQWritable[] res = new PairedDnaQWritable[size];
            Int128WritableComparable tempKey = new Int128WritableComparable();
            for (int i = 0; i < size; ++i) {
                res[i] = new PairedDnaQWritable();
                if (!pairedFastqRecordReader.next(tempKey, res[i])) {

                    System.err.println("file " + key);
                    System.err.println("expected " + size);
                    System.err.println("read " + i);
                    throw new RuntimeException("not enough reads in file");
                }
            }

            value.set(res);

            System.err.println("free memory after next: " + Runtime.getRuntime().freeMemory());
            read = true;
            return true;
        }

        @Override
        public Text createKey() {
            return new Text();
        }

        @Override
        public ArrayWritable createValue() {
            return new ArrayWritable(PairedDnaQWritable.class);
        }

        @Override
        public long getPos() throws IOException {
            return pairedFastqRecordReader.getPos();
        }

        @Override
        public void close() throws IOException {
            pairedFastqRecordReader.close();
        }

        @Override
        public float getProgress() throws IOException {
            return pairedFastqRecordReader.getProgress();
        }
    }
}
