package ru.ifmo.genetics.distributed.io.formats;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.LineReader;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;
import ru.ifmo.genetics.io.formats.Illumina;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.io.formats.Sanger;

import java.io.IOException;

public class PairedFastqInputFormat extends FileInputFormat<Int128WritableComparable, PairedDnaQWritable> {
    private static final QualityFormat illuminaQF = new Sanger();

    @Override
    public RecordReader<Int128WritableComparable, PairedDnaQWritable> getRecordReader(InputSplit split, JobConf job, Reporter reporter) throws IOException {
        reporter.setStatus(split.toString());
        return new PairedFastqRecordReader(job, (FileSplit) split);
    }

    public static class PairedFastqRecordReader implements RecordReader<Int128WritableComparable, PairedDnaQWritable> {
        private static final Log LOG = LogFactory.getLog(LineRecordReader.class.getName());
        public static final int FASTQ_BLOCK_SIZE = 8;

        private LineReader lineReader;
        private LongWritable lineKey;
        private Text lineValue;
        private long pos;
        private long end;
        private long start;
        private int maxLineLength;

        public PairedFastqRecordReader(JobConf job, FileSplit split) throws IOException {
    //        maxLineLength = job.getInt(org.apache.hadoop.mapreduce.lib.input.
    //                LineRecordReader.MAX_LINE_LENGTH, Integer.MAX_VALUE);
            maxLineLength = 1000;
            start = split.getStart();
            end = start + split.getLength();
            LOG.info(start + " " + end);
            System.err.println(start + " " + end);
            final Path file = split.getPath();

            // open the file and seek to the start of the split
            final FileSystem fs = file.getFileSystem(job);
            FSDataInputStream fileIn = fs.open(file);

            fileIn.seek(start);
            lineReader = new LineReader(fileIn, job);
            lineValue = new Text();
            lineKey = new LongWritable();


            if (start != 0) {
                // won't we skip a line if start equals to start of a block?
                start += lineReader.readLine(lineValue);
            }
            // next() method.
            do {
                lineKey.set(start);
                start += lineReader.readLine(lineValue);
            } while (lineValue.getLength() == 0 || (lineValue.getLength() > 0 && lineValue.charAt(0) != '@') || (lineValue.charAt(0) ==
                    '@' && lineValue.charAt(lineValue.getLength() - 1) != '1'));
            this.pos = start;

            positions = new LongWritable[FASTQ_BLOCK_SIZE];
            contents = new Text[FASTQ_BLOCK_SIZE];
            for (int i = 0; i < FASTQ_BLOCK_SIZE; i++) {
                positions[i] = new LongWritable();
                contents[i] = new Text();
            }

        }

        LongWritable[] positions;
        Text[] contents;

        @Override
        public boolean next(Int128WritableComparable key, PairedDnaQWritable value) throws IOException {
            boolean ok = true;
            for (int i = 0; i < FASTQ_BLOCK_SIZE; i++) {
                ok &= readLine(positions[i], contents[i]);
            }
            if (!ok) {
                return false;
            }
            String caption = contents[0].toString();
            int p2 = caption.lastIndexOf('#');
            int p = caption.lastIndexOf('#', p2 - 1);
            if (p == -1) {
                LOG.warn("Wrong 2fastq, unexpected line: " + caption);
                return false;
            }
            String pp = caption.substring(p + 1, p2);
            key.loLong = Long.parseLong(pp);
            key.hiLong = 0;
            value.set(contents[1], contents[3], contents[5], contents[7], illuminaQF);
            return true;
        }

        private boolean readLine(LongWritable key, Text value) throws IOException {
            if (lineValue != null) {
                key.set(lineKey.get());
                value.set(lineValue.getBytes());
                lineKey = null;
                lineValue = null;
                return true;
            }
            while (getPos() <= end) {
                key.set(pos);
                int newSize = lineReader.readLine(value, maxLineLength,
                        Math.max(maxBytesToConsume(pos), maxLineLength));
                if (newSize == 0) {
                    return false;
                }
                pos += newSize;
                if (newSize < maxLineLength) {
                    return true;
                }
                LOG.info("Skipped line of size " + newSize + " at pos " + (pos - newSize));
            }
            return false;
        }

        private int maxBytesToConsume(long pos) {
            return (int) Math.min(Integer.MAX_VALUE, end - pos);
        }

        @Override
        public Int128WritableComparable createKey() {
            return new Int128WritableComparable(0L);
        }

        @Override
        public PairedDnaQWritable createValue() {
            return new PairedDnaQWritable();
        }

        @Override
        public long getPos() throws IOException {
            return pos;
        }

        @Override
        public void close() throws IOException {
            lineReader.close();
        }

        @Override
        public float getProgress() throws IOException {
            if (start == end) {
                return 0.0f;
            } else {
                return Math.min(1.0f, (getPos() - start) / (float) (end - start));
            }
        }


    }
}


