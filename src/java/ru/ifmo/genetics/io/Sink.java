package ru.ifmo.genetics.io;

public interface Sink<T> {
    public void put(T v);
    public void flush();
    public void close();
}
