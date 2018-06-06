package ru.ifmo.genetics.io.writers;

import java.util.Iterator;

public class DataCounter implements Iterable<String> {

    @Override
    public Iterator<String> iterator() {
        return new MyIterator();
    }


    private class MyIterator implements Iterator<String> {
        long count = 0;

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public String next() {
            count++;
            return Long.toString(count);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
