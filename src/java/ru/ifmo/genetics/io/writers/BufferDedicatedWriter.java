package ru.ifmo.genetics.io.writers;

import ru.ifmo.genetics.executors.Latch;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class BufferDedicatedWriter implements Runnable {
    private final static int BUFFERS_SIZE = 1 << 21; // 2 MB
    private final static int BUFFERS_NUMBER = 50;  // total 100 MB

    private BlockingQueue<ByteBuffer> freeBuffers = new ArrayBlockingQueue<ByteBuffer>(BUFFERS_NUMBER);
    private BlockingQueue<ByteBuffer> buffersToWrite = new ArrayBlockingQueue<ByteBuffer>(BUFFERS_NUMBER);

    private OutputStream out;

    private Latch writingThreads;
    private volatile boolean finished;


    public BufferDedicatedWriter(String fileName) throws FileNotFoundException {
        this(new FileOutputStream(fileName));
    }

    public BufferDedicatedWriter(OutputStream out) {
        this.out = out;
        writingThreads = new Latch();
        finished = false;
        for (int i = 0; i < BUFFERS_NUMBER; ++i) {
            freeBuffers.add(ByteBuffer.allocate(BUFFERS_SIZE));
        }
    }


    public ByteBuffer getBuffer() throws InterruptedException {
        ByteBuffer res = freeBuffers.take();
        res.clear();
        return res;
    }

    public void returnBuffer(ByteBuffer buffer) {
        buffer.flip();
        buffersToWrite.add(buffer);
    }


    /**
     * Call this method when no thread will add a list to write.
     * This call blocks until all list will be written.
     */
    public void close() {
        finished = true;

        try {
            writingThreads.await();
        } catch (InterruptedException e) {
            System.err.println("Method await was interrupted!");
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        writingThreads.increase();
        try {
            while ((freeBuffers.size() != BUFFERS_NUMBER) || !finished) {
                ByteBuffer buffer = buffersToWrite.poll(100, TimeUnit.MILLISECONDS);
                if (buffer == null) {
                    continue;
                }

                out.write(buffer.array(), buffer.position(), buffer.limit());
                freeBuffers.add(buffer);
            }
        } catch (InterruptedException e) {
            System.err.println("Writing thread was interrupted!");
        } catch (IOException e) {
            System.err.println("IOException!");
            e.printStackTrace();
        } finally {
            writingThreads.decrease();
        }
    }
}
