package ru.ifmo.genetics.tools.olc.overlaps;

public class FullEdge implements Comparable<FullEdge> {
    public int from;
    public int to;
    public int centerShift;

    public FullEdge(int from, int to, int centerShift) {
        this.from = from;
        this.to = to;
        this.centerShift = centerShift;
    }

    @Override
    public int hashCode() {
        return from + to * 239 + centerShift * 1000000007;
    }

    @Override
    public boolean equals(Object o) {
        FullEdge e = (FullEdge)o;
        return (from == e.from) && (to == e.to) && (centerShift == e.centerShift);
    }

    @Override
    public int compareTo(FullEdge e) {
        if (centerShift != e.centerShift)
            return centerShift - e.centerShift;
        if (to != e.to)
            return to - e.to;
        return from - e.from;
    }

    @Override
    public String toString() {
        return "FullEdge [from=" + from + ", to=" + to + ", centerShift=" + centerShift + "]";
    }
}
