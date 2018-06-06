package ru.ifmo.genetics.utils.pairs;

public class UniPair<D> implements Pair<D, D> {
	public final D first, second;

	public UniPair(D a, D b) {
		this.first = a;
		this.second = b;
	}
	
    @Override
    public D first() {
        return first;
    }

    @Override
    public D second() {
        return second;
    }

    @Override 
    public String toString() {
        return "UniPair<" + first + ", " + second + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UniPair)) return false;

        UniPair uniPair = (UniPair) o;

        if (first != null ? !first.equals(uniPair.first) : uniPair.first != null)
            return false;
        if (second != null ? !second.equals(uniPair.second) : uniPair.second != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }
}
