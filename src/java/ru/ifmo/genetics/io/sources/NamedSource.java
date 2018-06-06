package ru.ifmo.genetics.io.sources;

public interface NamedSource<D> extends Source<D> {
    public String name();
}
