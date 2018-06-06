package ru.ifmo.genetics.io.readers;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class reads file by chunks of lines separated by newline symbol.
 *
 * It works well on relatively small lines. And doesn't work on lines greater than
 * half of a BUFFERS_SIZE.
 */
public class DedicatedLineReader implements Runnable {
    private final static int BUFFERS_SIZE = 1 << 19;

    private BlockingQueue<ByteBuffer> freeBuffers;
    private BlockingQueue<ByteBuffer> filledBuffers;
    private final ByteBuffer FINISH = ByteBuffer.allocate(0);

    private InputStream[] in;

    private final Thread readingThread;
    private volatile boolean finished;

    /**
     * @param readingFromBuffersThreadsNumber - number of threads, that will read data from buffers, parse and process it.
     */
    public DedicatedLineReader(File file, int readingFromBuffersThreadsNumber) throws FileNotFoundException {
        this(new InputStream[]{new FileInputStream(file)}, readingFromBuffersThreadsNumber);
    }

    /**
     * @param readingFromBuffersThreadsNumber - number of threads, that will read data from buffers, parse and process it.
     */
    public DedicatedLineReader(File[] files, int readingFromBuffersThreadsNumber) throws FileNotFoundException {
        this(readingFromBuffersThreadsNumber);
        in = new InputStream[files.length];
        for (int i = 0; i < files.length; i++) {
            in[i] = new FileInputStream(files[i]);
        }
    }

    /**
     * @param readingFromBuffersThreadsNumber - number of threads, that will read data from buffers, parse and process it.
     */
    public DedicatedLineReader(InputStream[] in, int readingFromBuffersThreadsNumber) {
        this(readingFromBuffersThreadsNumber);
        this.in = in;
    }

    private DedicatedLineReader(int readingThreadsNumber) {
        int buffersNumber = readingThreadsNumber * 2 + 1;
        readingThread = new Thread(this);
        finished = false;
        freeBuffers = new ArrayBlockingQueue<ByteBuffer>(buffersNumber);
        filledBuffers = new ArrayBlockingQueue<ByteBuffer>(buffersNumber);
        for (int i = 0; i < buffersNumber; ++i) {
            freeBuffers.add(ByteBuffer.allocate(BUFFERS_SIZE));
        }
    }



    public void start() {
        readingThread.start();
    }

    /**
     * Returns readable ByteBuffer.
     * Returns null, if reading process is finished and there are no filled buffers.
     */
    public ByteBuffer getBuffer() throws InterruptedException {
        ByteBuffer res = filledBuffers.take();
        if (res == FINISH) {
            filledBuffers.add(res);
            return null;
        }
        return res;
    }

    public void returnBuffer(ByteBuffer buffer) {
        buffer.clear();
        freeBuffers.add(buffer);
    }

    public boolean isFinished() {
        return finished;
    }

    @Override
    public void run() {
        ByteBuffer buffer = freeBuffers.poll();
        int inIndex = 0;
        try {
            while (inIndex < in.length) {
                byte[] bufferArray = buffer.array();
                while (inIndex < in.length && buffer.hasRemaining()) {
                    int bytesRead = in[inIndex].read(bufferArray, buffer.position(), buffer.remaining());
                    if (bytesRead == -1) {
                        // end of file i has been reached
                        try {
                            in[inIndex].close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        inIndex++;
                    } else {
                        assert bytesRead != 0;
                        buffer.position(buffer.position() + bytesRead);
                    }
                }

                ByteBuffer nextBuffer = null;
                if (inIndex < in.length) {  // i.e. if we will read some new data
                    nextBuffer = freeBuffers.take();
                    boolean endOfLineFound = false;
                    for (int i = buffer.position() - 1; i >= 0; --i) {
                        if (bufferArray[i] == '\n') {
                            endOfLineFound = true;
                            nextBuffer.put(bufferArray, i + 1, buffer.limit() - i - 1);
                            buffer.position(i + 1);
                            break;
                        }
                    }
                    if (!endOfLineFound) {
                        throw new RuntimeException("End of line not found");
                    }
                }

                buffer.flip();
                filledBuffers.add(buffer);
                buffer = nextBuffer;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        finished = true;
        filledBuffers.add(FINISH);
    }

    public static int readInteger(ByteBuffer buffer) throws IOException {
        byte[] bufferArray = buffer.array();
        int pos = buffer.position();
        int limit = buffer.limit();

        while (pos < limit && isWS(bufferArray[pos])) {
            pos++;
        }
        if (pos == limit) {
            return -1;
        }
        int begin = pos;

        while (pos < limit && !isWS(bufferArray[pos])) {
            pos++;
        }
        int end = pos;
        buffer.position(pos);

        return Integer.parseInt(new String(bufferArray, begin, end - begin));
    }

    private static boolean isWS(byte c) {
        return c == '\r' || c == '\n' || c == ' ';
    }
}
