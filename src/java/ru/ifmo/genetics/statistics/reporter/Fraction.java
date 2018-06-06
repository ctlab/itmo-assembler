package ru.ifmo.genetics.statistics.reporter;

import java.util.Formatter;

public class Fraction {
    private final Counter n;
    private final Counter total;

    public Fraction(Counter n, Counter total) {
        this.n = n;
        this.total = total;
    }

    public double value() {
        if (total.value() == 0) {
            return 0;
        }
        return (double)n.value() / total.value();
    }

    public String toString() {
        Formatter f = new Formatter(new StringBuilder(5));
        f.format("%s = %.1f%%", n.name, value() * 100);
        return f.toString();
    }
}
