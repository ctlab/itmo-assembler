package ru.ifmo.genetics.distributed.io.formats;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import ru.ifmo.genetics.distributed.io.SequencedWritableRecordReader;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.PairWritable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;

import java.io.EOFException;
import java.io.IOException;

public class PairedBinqInputFormat
        extends FileInputFormat<Int128WritableComparable, PairedDnaQWritable> {
    @Override
    public RecordReader<Int128WritableComparable, PairedDnaQWritable> getRecordReader(InputSplit split, JobConf job, Reporter reporter) throws IOException {
        reporter.setStatus(split.toString());
        return new MyRecordReader(job, (FileSplit) split);
    }

    @Override
    protected boolean isSplitable(FileSystem fs, Path filename) {
        return true;
    }

    // TODO: there is a problem here: seems like some k-mers are processed twice
    private static class MyRecordReader
            implements RecordReader<Int128WritableComparable, PairedDnaQWritable> {
        private SequencedWritableRecordReader<PairWritable<Int128WritableComparable, PairedDnaQWritable>> internalReader;
        private long end;
        private long start;
        private PairWritable<Int128WritableComparable, PairedDnaQWritable> kv = new PairWritable<Int128WritableComparable, PairedDnaQWritable>(null, null);

        MyRecordReader(JobConf job, FileSplit split) throws IOException {
            start = split.getStart();
            end = start + split.getLength();
            final Path file = split.getPath();
            internalReader = new SequencedWritableRecordReader<PairWritable<Int128WritableComparable, PairedDnaQWritable>>(job, file, start);

        }

        @Override
        public boolean next(Int128WritableComparable key, PairedDnaQWritable value) throws IOException {
            // :TODO: there is a problem in the end of file
            if (internalReader.canFinishReading() && internalReader.position() >= end) {
                return false;
            }

            try {
                kv.first = key;
                kv.second = value;
                internalReader.readRecord(kv);
                return true;
            } catch (EOFException e) {
                return false;
            }
        }

        @Override
        public Int128WritableComparable createKey() {
            return new Int128WritableComparable();
        }

        @Override
        public PairedDnaQWritable createValue() {
            return new PairedDnaQWritable();
        }

        @Override
        public long getPos() throws IOException {
            return internalReader.position();
        }

        @Override
        public void close() throws IOException {
            internalReader.close();
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
