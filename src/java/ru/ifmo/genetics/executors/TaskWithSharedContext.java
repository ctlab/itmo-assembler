package ru.ifmo.genetics.executors;

import org.jetbrains.annotations.Nullable;

public interface TaskWithSharedContext<C> extends Runnable {
    /**
     * Sets the context of task, if context is null, task should create it itself
     * @param context context to set
     */
    public void setContext(@Nullable C context);

    /**
     * Is called after execution, returns modified context
      * @return updated context
     */
    public C getContext();
}
