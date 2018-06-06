package ru.ifmo.genetics.executors;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadExecutorWithSharedContext<C> extends BlockingThreadPoolExecutor {
    ConcurrentLinkedQueue<C> contexts = new ConcurrentLinkedQueue<C>();
    public ThreadExecutorWithSharedContext(int nThreads) {
        super(nThreads);
    }

    public ThreadExecutorWithSharedContext(int nThreads, int queueCapacity) {
        super(nThreads, queueCapacity);
    }

    public void blockingExecute(TaskWithSharedContext<C> command) throws InterruptedException {
        @Nullable C context = contexts.poll();
        command.setContext(context);
        super.blockingExecute(command);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (r instanceof TaskWithSharedContext) {
            C context = ((TaskWithSharedContext<C>) r).getContext();
            if (context != null) {
                contexts.add(context);
            }
        }
    }

    /**
     * Returns all the resulting contexts. Should be called after full shutdown.
     * @return list of resulting contexts
     */
    public Collection<C> getContexts() {
        return new ArrayList<C>(contexts);
    }
}
