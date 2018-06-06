package ru.ifmo.genetics.io.sources;

import ru.ifmo.genetics.utils.iterators.ProgressableIterator;

public interface Source<D> extends Iterable<D> {
    @Override
    public ProgressableIterator<D> iterator();
}
