package ru.ifmo.genetics.utils.pairs;

public class Tuple3<T1, T2, T3> {
    public final T1 first;
    public final T2 second;
    public final T3 third;

    public static <T1, T2, T3> Tuple3<T1, T2, T3> create(T1 first, T2 second, T3 third) {
        return new Tuple3<T1, T2, T3>(first, second, third);
    }

    public Tuple3(T1 first, T2 second, T3 third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tuple3)) return false;

        Tuple3 tuple3 = (Tuple3) o;

        if (first != null ? !first.equals(tuple3.first) : tuple3.first != null)
            return false;
        if (second != null ? !second.equals(tuple3.second) : tuple3.second != null)
            return false;
        if (third != null ? !third.equals(tuple3.third) : tuple3.third != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        result = 31 * result + (third != null ? third.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "<" +
                first +
                ", " + second +
                ", " + third +
                '>';
    }
}
