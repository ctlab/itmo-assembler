package ru.ifmo.genetics.executors;

import org.apache.commons.configuration.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PatientExecutorService implements Executor {
    private static final int THREAD_POOL_TIMEOUT = 365;    // in days!

    private ExecutorService executor;
    private Latch jobs;
    private int threads;

    private boolean failed = false;


    public PatientExecutorService(int availableProcessors) {
        executor = Executors.newFixedThreadPool(availableProcessors);
        jobs = new Latch();
        threads = availableProcessors;
    }

    public PatientExecutorService(Configuration c) {
        this(c.getInt("available_processors"));
    }


    public void execute(final Runnable task) {
        jobs.increase();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    failed = true;
                } finally {
                    jobs.decrease();
                }
            }
        });
    }

    public void waitForShutdown() throws InterruptedException {
//        System.err.println("waiting for shutdown");

        jobs.await();
        executor.shutdown();

//        System.err.println("all tasks finished");

        /*
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(INFO_UPDATE_INTERVAL);
            } catch (InterruptedException e) {
            }
            System.err.print(jobsWait
                    + " jobs wait, "
                    + t + " elapsed, "
                    + String.format("%.2f", ((t.getTime() / (jobsCount - jobsWait.intValue() + 0.0))
                            * jobsWait.intValue() / 1000.0)) + " seconds left");
            System.err.print('\r');
        }
        System.err.print('\n');
        */
        // System.err.print(jobsWait + " jobs wait, ");

        executor.awaitTermination(THREAD_POOL_TIMEOUT, TimeUnit.DAYS);

//        long lt = (long) ((t.getTime() / (jobsCount - jobsWait.intValue() + 0.0)) * jobsWait.intValue());
//        System.err.println(t + " elapsed, " + Timer.timeToString(lt) + " left");

        if (failed) {
            throw new ExecutionFailedException("Execution of at least one of the tasks failed");
        }
    }

    public boolean notMuchTasks() {
        return jobs.get() < 2 * threads;
    }

}
