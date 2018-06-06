package ru.ifmo.genetics.utils.iterators;

import ru.ifmo.genetics.utils.pairs.UniPair;

import java.util.Iterator;

public class UniZippingIterator<T> implements Iterator<UniPair<T>> {
    private final Iterator<? extends T> iterator1;
    private final Iterator<? extends T> iterator2;

    public UniZippingIterator(Iterator<? extends T> iterator1, Iterator<? extends T> iterator2) {
        this.iterator1 = iterator1;
        this.iterator2 = iterator2;
    }

    @Override
    public boolean hasNext() {
        return iterator1.hasNext() && iterator2.hasNext();
    }

    @Override
    public UniPair<T> next() {
        T e1 = iterator1.next();
        T e2 = iterator2.next();
        return new UniPair<T>(e1, e2);
    }

    @Override
    public void remove() {
        iterator1.remove();
        iterator2.remove();
    }
}
