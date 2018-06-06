package ru.ifmo.genetics.executors;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A synchronization aid that allows one or more threads to wait until
 * other threads completes.
 *
 * <p>
 *     A {@code Latch} class has a <em>count</em> variable, that can be increased
 *     by {@link #increase} method and decreased by {@link #decrease} method.
 *     The {@link #await} method blocks until the current count reaches zero.
 * </p>
 *
 * <p>
 *     Be careful using Latch's {@link #increase} method at thread start,
 *     you may call and return from {@link #await} method when not all created
 *     threads start.
 * </p>
 */
public class Latch {
    private final AtomicInteger count;


    public Latch(int count) {
        this.count = new AtomicInteger(count);
    }

    public Latch() {
        this(0);
    }


    /**
     * Be careful using Latch's {@link #increase} method at thread start,
     * you may call and return from {@link #await} method when not all created
     * threads start.
     */
    public void increase() {
        count.incrementAndGet();
    }

    public void decrease() {
        count.decrementAndGet();

        if (count.get() == 0) {
            synchronized (count) {
                count.notifyAll();
            }
        }
    }

    public int get() {
        return count.get();
    }


    /**
     * Wait until <em>count</em> decrease to zero.
     */
    public void await() throws InterruptedException {
        synchronized (count) {
            while (count.get() != 0) {
                count.wait();
            }
        }
    }

}
