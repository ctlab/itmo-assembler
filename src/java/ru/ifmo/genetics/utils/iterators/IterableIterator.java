package ru.ifmo.genetics.utils.iterators;

import java.util.Iterator;

public class IterableIterator<T> implements Iterable<T>{
    private Iterator<T> iterator;

    public IterableIterator(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    public static <T> IterableIterator<T> makeIterable(Iterator<T> iterator) {
        return new IterableIterator<T>(iterator);
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<T> res = iterator;
        iterator = null;
        return res;
    }
}
