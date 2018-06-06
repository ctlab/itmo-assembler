package ru.ifmo.genetics.utils.iterators;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.NoSuchElementException;

public abstract class ReadersIterator<T> implements ProgressableIterator<T> {
    protected FileChannel fc;
    private final long sizeBytes;
    protected T next;

    public ReadersIterator(FileChannel fc) throws IOException {
        this.fc = fc;
        sizeBytes = fc.size();
        next = null;
    }



    protected abstract T readNext() throws IOException;

    @Override
    public boolean hasNext() {
        if (next == null) {
            try {
                next = readNext();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return next != null;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        T res = next;
        next = null;
        return res;
    }



    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double progress() {
        if (fc == null) {
            return 1.0;
        }
        try {
            long pos = fc.position();
            return (double) pos / sizeBytes;
        } catch (IOException e) {
            // and what?!
        }
        return 1.0; // read till the end of file?
    }

}