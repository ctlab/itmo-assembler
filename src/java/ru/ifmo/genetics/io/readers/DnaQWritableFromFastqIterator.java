package ru.ifmo.genetics.io.readers;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.RecordReader;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.PairWritable;
import ru.ifmo.genetics.io.formats.QualityFormat;

import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DnaQWritableFromFastqIterator implements Iterator<PairWritable<Text, DnaQWritable>> {

    boolean autoclose;
    boolean haveToRead = true;
    boolean hasNext = false;

    RecordReader<Text, DnaQWritable> reader;

    Text id = new Text();
    DnaQWritable dnaq = new DnaQWritable();

    QualityFormat qf;
    PairWritable<Text, DnaQWritable> value = new PairWritable<Text, DnaQWritable>(id, dnaq);


    public DnaQWritableFromFastqIterator(String fastqFile, QualityFormat qf) throws IOException {
        this(new BufferedInputStream(new FileInputStream(fastqFile)), qf);
    }

    public DnaQWritableFromFastqIterator(InputStream in, QualityFormat qf) throws IOException {
        this(in, qf, true);
    }

    public DnaQWritableFromFastqIterator(InputStream in, QualityFormat qf, boolean autoclose) throws IOException {
        reader = new FastqRecordReader(in, qf);
        this.qf = qf;
        this.autoclose = autoclose;
    }

    public void close() throws IOException {
        reader.close();
    }


    @Override
    public boolean hasNext() {
        if (!haveToRead) {
            return hasNext;
        }
        try {
            hasNext = reader.next(id, dnaq);
            haveToRead = false;
            if (!hasNext && autoclose) {
                reader.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return hasNext;
    }

    @Override
    public PairWritable<Text, DnaQWritable> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        haveToRead = true;

        return value;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
