package ru.ifmo.genetics.distributed.io.writable;

public interface Copyable<T> {
    public void copyFieldsFrom(T source);
}
