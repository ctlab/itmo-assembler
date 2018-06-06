package ru.ifmo.genetics.utils.pairs;

public class MutablePair<F, S> implements Pair<F, S>{

    public static final int MAGIC = (int) 1e9 + 9;

    public F first;
    public S second;

    public MutablePair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <F, S> MutablePair<F, S> make(F first, S second) {
        return new MutablePair<F, S>(first, second);
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
        if ((o == null) || !(o instanceof MutablePair)) {
            return false;
        }
        MutablePair<F,S> p = (MutablePair<F,S>) o;
        boolean ef = (first == p.first) || (first != null) && first.equals(p.first);
        boolean es = (second == p.second) || (second != null) && second.equals(p.second);
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
}
