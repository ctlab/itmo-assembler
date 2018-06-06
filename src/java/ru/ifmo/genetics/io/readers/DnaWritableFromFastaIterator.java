package ru.ifmo.genetics.io.readers;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.RecordReader;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.io.writable.PairWritable;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.io.readers.FastaRecordReader;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DnaWritableFromFastaIterator implements Iterator<PairWritable<Text, DnaWritable>> {

    boolean autoclose;
    boolean haveToRead = true;
    boolean hasNext = false;

    RecordReader<Text, DnaWritable> reader;

    Text id = new Text();
    DnaWritable dnaq = new DnaWritable();

    QualityFormat qf;
    PairWritable<Text, DnaWritable> value = new PairWritable<Text, DnaWritable>(id, dnaq);


    public DnaWritableFromFastaIterator(String fastqFile) throws IOException {
        this(new BufferedInputStream(new FileInputStream(fastqFile)));
    }

    public DnaWritableFromFastaIterator(InputStream in) throws IOException {
        this(in, true);
    }

    public DnaWritableFromFastaIterator(InputStream in, boolean autoclose) throws IOException {
        reader = new FastaRecordReader(in);
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
    public PairWritable<Text, DnaWritable> next() {
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
