package ru.ifmo.genetics.executors;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class NonBlockingQueueExecutor {
    final static int THREAD_SLEEP_TIME = 80;    // in millis

    final int nThreads;
    public final Queue<Runnable> tasks;

    private final Thread[] workers;
    private final CountDownLatch latch;     // how many workers haven't finished yet
    private final AtomicInteger running;    // how many tasks are running




    public NonBlockingQueueExecutor(int nThreads) {
        this(nThreads, null);
    }

    public NonBlockingQueueExecutor(int nThreads, Comparator<Runnable> comparator) {
        this.nThreads = nThreads;
        if (comparator == null) {
            tasks = new LinkedTransferQueue<Runnable>();
        } else {
            tasks = new PriorityQueue<Runnable>(20, comparator);
        }

        workers = new Thread[nThreads];
        latch = new CountDownLatch(nThreads);
        running = new AtomicInteger(0);
    }

    public void startWorkers() {
        for (int i = 0; i < nThreads; i++) {
            workers[i] = new Thread(new Worker());
            workers[i].start();
        }
    }



    public void addTask(Runnable task) {
        synchronized (tasks) {
            tasks.add(task);
        }
    }

    /**
     * Returns only when no new tasks are in the task queue and
     * all previous tasks have already finished.
     */
    public void waitForTasksToFinish() {
        while (true) {
            synchronized (tasks) {
                if (tasks.isEmpty() && (running.get() == 0)) {
                    return;
                }
            }
            try {
                Thread.sleep((int) (THREAD_SLEEP_TIME * 2.4)); // max delay 0.2 sec
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting to finish", e);
            }
        }
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are
     * executed, but no new tasks will be accepted.
     */
    public void shutdownAndAwaitTermination() {
        synchronized (tasks) {
            for (int i = 0; i < nThreads; i++) {
                tasks.add(new EndTask());
            }
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.err.println("Main thread was interrupted");
            for (Thread worker : workers) {
                worker.interrupt();
            }
            throw new RuntimeException("Main thread was interrupted while waiting to finish", e);
        }
    }



    private class EndTask implements Runnable {
        @Override
        public void run() {
        }
    }

    public class Worker implements Runnable {
        @Override
        public void run() {
            while (true) {
                Runnable task = null;

                synchronized (tasks) {
                    task = tasks.poll();
                    if (task != null) {
                        running.incrementAndGet();
                    }
                }

                if (task == null) {
                    try {
                        Thread.sleep(THREAD_SLEEP_TIME);
                    } catch (InterruptedException e) {
                        break;
                    }
                } else {
                    if (task.getClass() == EndTask.class) {
                        running.decrementAndGet();
                        break;
                    }
                    task.run();
                    running.decrementAndGet();
                }
            }
            latch.countDown();
        }
    }
}
