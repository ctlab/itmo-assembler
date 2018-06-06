package ru.ifmo.genetics.io;

public abstract class CommentableSink<T> implements Sink<T> {

    public abstract void put(String comment, T v);


    long id = 1;

    @Override
    public void put(T v) {
        put(Long.toString(id), v);
        id++;
    }
}
