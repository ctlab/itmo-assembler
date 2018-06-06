package ru.ifmo.genetics.utils.iterators;

import java.util.Iterator;

public interface ProgressableIterator<T> extends Iterator<T> {
    public double progress();
}
