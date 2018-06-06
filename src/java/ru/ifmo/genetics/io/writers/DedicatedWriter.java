package ru.ifmo.genetics.io.writers;

import ru.ifmo.genetics.io.Sink;

import java.io.File;

public interface DedicatedWriter<T> {

    public void start();
    public void stopAndWaitForFinish() throws InterruptedException;

    public Sink<T> getLocalSink();

    public File[] getResultingFiles();
}
