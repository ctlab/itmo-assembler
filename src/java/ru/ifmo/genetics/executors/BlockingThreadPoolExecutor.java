package ru.ifmo.genetics.executors;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockingThreadPoolExecutor extends ThreadPoolExecutor {

    private boolean failed = false;
    public BlockingThreadPoolExecutor(int nThreads) {
        this(nThreads, nThreads * 2);
    }

    public BlockingThreadPoolExecutor(int nThreads, int queueCapacity) {
        super(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        blockingQueue = new ArrayBlockingQueue<Runnable>(queueCapacity);
        freeThreads = new AtomicInteger(nThreads);
    }

    private final BlockingQueue<Runnable> blockingQueue;
    private final AtomicInteger freeThreads;

    public void blockingExecute(Runnable command) throws InterruptedException {
        blockingQueue.put(command);

        synchronized (freeThreads) {
            if (freeThreads.get() > 0) {
                if (tryExecuteCommand()) {
                    freeThreads.decrementAndGet();
                }
            }
        }
    }

    private int blockingQueueSize() {
        return blockingQueue.size();
    }

    private boolean tryExecuteCommand() {
        synchronized (blockingQueue) {
            Runnable command = blockingQueue.poll();
            if (command == null) {
                return false;
            }

            execute(command);
        }
        return true;
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (t == null) {    // if last task completed, not canceled
            synchronized (freeThreads) {
                if (!tryExecuteCommand()) {
                    freeThreads.incrementAndGet();
                }
            }
        }
        if (t != null) {
            t.printStackTrace();
            failed = true;
        }
    }

    public void shutdownAndAwaitTermination() throws InterruptedException {
        Thread.sleep(50);   // Wait a little before first check (small tasks will finish for this time)
        while (true) {
            synchronized (blockingQueue) {
                if (blockingQueue.size() == 0) {
                    shutdown();     // Disable new tasks from being submitted
                    break;
                }
            }
            Thread.sleep(1000);
        }

        try {
            // Wait a while for existing tasks to terminate
            if (!awaitTermination(42, TimeUnit.DAYS)) {
                shutdownNow();   // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }

            if (failed) {
                throw new ExecutionFailedException("Execution of at least one of the tasks failed");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
