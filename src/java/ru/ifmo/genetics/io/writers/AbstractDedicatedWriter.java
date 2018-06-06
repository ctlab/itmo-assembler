package ru.ifmo.genetics.io.writers;

import org.apache.log4j.Logger;
import ru.ifmo.genetics.io.CommentableSink;
import ru.ifmo.genetics.io.Sink;
import ru.ifmo.genetics.utils.tool.Tool;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Class to write sequences to file. <br></br>
 *
 * You can use many threads to prepare output sequences.
 * But only one dedicated for writing thread actually writes to file.
 */
public abstract class AbstractDedicatedWriter<T> implements CommentableDedicatedWriter<T> {


    private final static int DEFAULT_LIST_QUEUE_CAPACITY = 50;

    protected final Logger log = Logger.getLogger("writer");


    protected File file;

    protected Thread writingThread;
    protected final BlockingQueue<List<T>> dataQueue;


    public AbstractDedicatedWriter(File file) throws FileNotFoundException {
        dataQueue = new ArrayBlockingQueue<List<T>>(DEFAULT_LIST_QUEUE_CAPACITY);
        this.file = file;
        writingThread = new Thread(new WriterTask(
                new FileOutputStream(file)
        ));
    }



    protected abstract void writeData(Iterable<T> data, PrintWriter out);





    @Override
    public void start() {
        if (writingThread.isAlive()) {
            return;     // already running
        }
        writingThread.start();
    }

    @Override
    public void stopAndWaitForFinish() throws InterruptedException {
        List<T> endList = new ArrayList<T>(1);
        endList.add(null);

        dataQueue.add(endList);
        writingThread.join();
    }

    @Override
    public File[] getResultingFiles() {
        return new File[]{file};
    }



    protected class WriterTask implements Runnable {
        private final PrintWriter out;

        public WriterTask(OutputStream outputStream) {
            out = new PrintWriter(outputStream);
        }

        @Override
        public void run() {
            long written = 0;
            while (true) {
                List<T> data = null;
                try {
                    data = dataQueue.take();
                } catch (InterruptedException e) {
                    Tool.error(log, "Writing thread was interrupted", e);
                    break;
                }

                if ((data.size() == 1) && (data.get(0) == null)) {  // stop marker
                    break;
                }

                writeData(data, out);
                written += data.size();
            }
            out.close();
            Tool.debug(log, written + " sequences written");
        }
    }

    @Override
    public Sink<T> getLocalSink() {
        return new MyLocalSink();
    }
    @Override
    public CommentableSink<T> getLocalCommentableSink() {
        return new MyLocalSink();
    }

    public static final int DEFAULT_LIST_CAPACITY = 100;

    private class MyLocalSink extends CommentableSink<T> {
        private List<T> list = null;
        private MyLocalSink() {
            list = new ArrayList<T>(DEFAULT_LIST_CAPACITY);
        }

        @Override
        public void put(T v) {
            list.add(v);
            if (list.size() >= DEFAULT_LIST_CAPACITY) {
                flush();
            }
        }
        @Override
        public void put(String s, T v) {
            put(v); // ignoring comment (this is AbstractDW, not AbstractCommentableDW)
        }

        @Override
        public void flush() {
            try {
                dataQueue.put(list);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            list = new ArrayList<T>(DEFAULT_LIST_CAPACITY);
        }

        @Override
        public void close() {
            flush();
        }
    }

}
