package ru.ifmo.genetics.io.writers;

import ru.ifmo.genetics.io.CommentableSink;
import ru.ifmo.genetics.io.Sink;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractCommentableDedicatedWriter<T> extends AbstractDedicatedWriter<T> {


    protected final ConcurrentLinkedQueue<List<String>> commentQueue;

    public AbstractCommentableDedicatedWriter(File file) throws IOException {
        super(file);
        commentQueue = new ConcurrentLinkedQueue<List<String>>();
    }


    protected abstract void writeData(Iterable<String> comments, Iterable<T> data, PrintWriter out);

    @Override
    protected void writeData(Iterable<T> data, PrintWriter out) {
        List<String> comments = commentQueue.poll();
        writeData(comments, data, out);
    }


    @Override
    public Sink<T> getLocalSink() {
        return new MyLocalSink();
    }

    public CommentableSink<T> getLocalCommentableSink() {
        return new MyLocalSink();
    }


    private class MyLocalSink extends CommentableSink<T> {
        private List<T> dataList = null;
        private List<String> commentList = null;
        private MyLocalSink() {
            dataList = new ArrayList<T>(DEFAULT_LIST_CAPACITY);
            commentList = new ArrayList<String>(DEFAULT_LIST_CAPACITY);
        }

        @Override
        public void put(String comment, T v) {
            dataList.add(v);
            commentList.add(comment);
            if (dataList.size() >= DEFAULT_LIST_CAPACITY) {
                flush();
            }
        }

        @Override
        public void flush() {
            try {
                synchronized (AbstractCommentableDedicatedWriter.this) {
                    commentQueue.add(commentList);
                    dataQueue.put(dataList);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            dataList = new ArrayList<T>(DEFAULT_LIST_CAPACITY);
            commentList = new ArrayList<String>(DEFAULT_LIST_CAPACITY);
        }

        @Override
        public void close() {
            flush();
        }
    }
}
