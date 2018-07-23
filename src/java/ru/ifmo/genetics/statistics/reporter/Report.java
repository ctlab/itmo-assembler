package ru.ifmo.genetics.statistics.reporter;

import java.util.ArrayList;
import java.util.List;

public abstract class Report {
    private final String name;

    protected Report(String name) {
        this.name = name;
    }

    public String name() { return name; };

    protected List<Counter> counters = new ArrayList<Counter>();

    protected Counter addCounter(String name) {
        Counter res = new Counter(name);
        counters.add(res);
        return res;
    }

    protected Object[] layout;


    protected void setLayout(Object... layout) {
        this.layout = layout;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean needComma = false;
        for (Object o: layout) {
            if (needComma) {
                sb.append(", ");
            }
            sb.append(o);
            needComma = true;
        }
        return sb.toString();
    }

    synchronized
    public void mergeFrom(Report other) {
        assert counters.size() == other.counters.size();
        for (int i = 0; i < counters.size(); ++i) {
            Counter a = counters.get(i);
            Counter b = other.counters.get(i);
            assert a.name.equals(b.name) : "merging counters " + a + " and " + b;
            a.mergeFrom(b);
        }
    }

    public void reset() {
        for (Counter c: counters) {
            c.reset();
        }
    }
    public long getCounterCurrentValue(String name) {
        for (Counter counter : counters) {
            if (counter.name.equals(name)) {
                return counter.value();
            }
        }
        return -1;
    }
}
