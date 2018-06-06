package ru.ifmo.genetics.io.writers;

import ru.ifmo.genetics.io.CommentableSink;

public class NullSink<T> extends CommentableSink<T> {
    @Override
    public void put(String comment, T v) {}

    @Override
    public void put(T v) {}

    @Override
    public void flush() {}

    @Override
    public void close() {}
}
