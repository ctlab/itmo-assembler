package ru.ifmo.genetics.io.readers;

import org.apache.hadoop.io.Text;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.distributed.io.writable.PairWritable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;
import ru.ifmo.genetics.io.formats.QualityFormat;

import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class PairedFastqIterator implements Iterator<PairWritable<Text, PairedDnaQWritable>> {
    boolean autoclose;
    DnaQWritableFromFastqIterator it1;
    DnaQWritableFromFastqIterator it2;

    PairWritable<Text, PairedDnaQWritable> value = new PairWritable<Text, PairedDnaQWritable>(null, new PairedDnaQWritable(null, null));

    public PairedFastqIterator(String fastqFile1, String fastqFile2, QualityFormat qf) throws IOException {
        this(
            new BufferedInputStream(new FileInputStream(fastqFile1)),
            new BufferedInputStream(new FileInputStream(fastqFile2)),
            qf,
            true
        );

    }
    public PairedFastqIterator(InputStream is1, InputStream is2, QualityFormat qf, boolean autoclose) throws IOException {
        it1 = new DnaQWritableFromFastqIterator(is1, qf, autoclose);
        it2 = new DnaQWritableFromFastqIterator(is2, qf, autoclose);
        this.autoclose = autoclose;
    }

    public void close() throws IOException {
        it1.close();
        it2.close();
    }

    @Override
    public boolean hasNext() {
        boolean res = it1.hasNext() && it2.hasNext();
        if (!res) {
            try {
                it1.close();
                it2.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return res;
    }

    @Override
    public PairWritable<Text, PairedDnaQWritable> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        PairWritable<Text, DnaQWritable> value1 = it1.next();
        PairWritable<Text, DnaQWritable> value2 = it2.next();

        // may be it should not be asserted
        assert value1.first.equals(value2.first);

        value.first = value1.first;
        value.second.first = value1.second;
        value.second.second = value2.second;

        return value;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
