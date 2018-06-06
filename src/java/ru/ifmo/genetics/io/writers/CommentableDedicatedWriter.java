package ru.ifmo.genetics.io.writers;

import ru.ifmo.genetics.io.CommentableSink;
import ru.ifmo.genetics.io.Sink;

import java.io.File;

public interface CommentableDedicatedWriter<T> extends DedicatedWriter<T> {

    public CommentableSink<T> getLocalCommentableSink();

}
