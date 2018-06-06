package ru.ifmo.genetics.io.writers;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class writes file by chunks of lines separated by newline symbol.
 *
 * It works well on relatively small lines. And doesn't work on lines greater than
 * half of a BUFFERS_SIZE.
 */
public class DedicatedLineWriter implements Runnable {
    private final static int BUFFERS_SIZE = 1 << 19;

    private BlockingQueue<ByteBuffer> freeBuffers;
    private BlockingQueue<ByteBuffer> filledBuffers;
    private final ByteBuffer FINISH = ByteBuffer.allocate(0);

    private OutputStream out;

    private final Thread writingThread;
    private volatile boolean finished;

    /**
     * @param writingToBuffersThreadsNumber - number of threads, that will write data to buffers and give them to me.
     */
    public DedicatedLineWriter(File file, int writingToBuffersThreadsNumber) throws FileNotFoundException {
        this(new FileOutputStream(file), writingToBuffersThreadsNumber);
    }

    public DedicatedLineWriter(OutputStream out, int writingToBuffersThreadsNumber) {
        int buffersNumber = writingToBuffersThreadsNumber * 2 + 1;
        this.out = out;
        finished = false;
        freeBuffers = new ArrayBlockingQueue<ByteBuffer>(buffersNumber);
        filledBuffers = new ArrayBlockingQueue<ByteBuffer>(buffersNumber);
        for (int i = 0; i < buffersNumber; ++i) {
            freeBuffers.add(ByteBuffer.allocate(BUFFERS_SIZE));
        }
        writingThread = new Thread(this);
        writingThread.start();
    }

    /**
     * Returns ByteBuffer to write data to, waiting for free one if necessary.
     */
    public ByteBuffer getBuffer() throws InterruptedException {
        return freeBuffers.take();
    }

    public void writeBuffer(ByteBuffer buffer) throws InterruptedException {
        buffer.flip();
        filledBuffers.put(buffer);
    }

    public boolean isFinished() {
        return finished;
    }

    public void close() {
        finished = true;
        filledBuffers.add(FINISH);

        try {
            writingThread.join();    // waiting for thread to finish
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for writingThread to finish");
        }
    }


    @Override
    public void run() {
        try {
            while (true) {
                ByteBuffer buffer = filledBuffers.take();
                if (buffer == FINISH) {
                    break;
                }
                
                byte[] bufferArray = buffer.array();
                out.write(bufferArray, 0, buffer.limit());

                buffer.clear();
                freeBuffers.add(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String NL = System.getProperty("line.separator");

    /**
     * @return resulting output buffer
     */
    public ByteBuffer writeLine(ByteBuffer output, String str) throws InterruptedException {
        str += NL;
        byte[] bytes = str.getBytes();

        if (bytes.length > output.remaining()) {
            writeBuffer(output);
            output = getBuffer();
        }
        output.put(bytes);

        return output;
    }

}
