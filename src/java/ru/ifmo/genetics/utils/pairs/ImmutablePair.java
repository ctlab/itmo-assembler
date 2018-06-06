package ru.ifmo.genetics.utils.pairs;

public class ImmutablePair<F, S> implements Pair<F, S> {
    public static final int MAGIC = (int) 1e9 + 9;

    public final F first;
    public final S second;

    public ImmutablePair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <F, S> ImmutablePair<F, S> make(F first, S second) {
        return new ImmutablePair<F, S>(first, second);
    }

    @Override
    public int hashCode() {
        return first.hashCode() + MAGIC * second.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || !(o instanceof Pair)) {
            return false;
        }
        Pair<F,S> p = (Pair<F,S>) o;
        boolean ef = (first == p.first()) || (first != null) && first.equals(p.first());
        boolean es = (second == p.second()) || (second != null) && second.equals(p.second());
        return ef && es;
    }

    @Override
    public F first() {
        return first;
    }

    @Override
    public S second() {
        return second;
    }

    @Override
    public String toString() {
        return "ImmutablePair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
