package ru.ifmo.genetics.statistics.reporter;

import ru.ifmo.genetics.utils.NumUtils;

public class Counter {
    public final String name;
    private long n;

    public Counter(String name) {
        this.name = name;
    }

    public void increment() {
        n++;
    }

    public void incrementBy(long value) {
        n += value;
    }

    public long value() {
        return n;
    }

    public String toString() {
        return name + " = " + NumUtils.makeHumanReadable(n);
    }

    public Fraction fraction(Counter total) {
        return new Fraction(this, total);
    }

    public void mergeFrom(Counter b) {
        n += b.n;
    }

    public void reset() {
        n = 0;
    }
}
