package ru.ifmo.genetics.io.writers;

import ru.ifmo.genetics.io.CommentableSink;
import ru.ifmo.genetics.io.Sink;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

public class NullDedicatedWriter<T> implements CommentableDedicatedWriter<T> {

    @Override
    public void start() {}

    @Override
    public void stopAndWaitForFinish() throws InterruptedException {}

    @Override
    public Sink<T> getLocalSink() {
        return new NullSink<T>();
    }
    @Override
    public CommentableSink<T> getLocalCommentableSink() {
        return new NullSink<T>();
    }

    @Override
    public File[] getResultingFiles() {
        return new File[0];
    }

}
