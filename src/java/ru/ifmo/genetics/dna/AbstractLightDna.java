package ru.ifmo.genetics.dna;

public abstract class AbstractLightDna implements LightDna, Comparable<LightDna>{
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof LightDna)) return false;

        LightDna that = (LightDna) o;

        return DnaTools.equals(this, that);
    }

    @Override
    public int hashCode() {
        return DnaTools.hashCode(this);
    }

    @Override
    public String toString() {
        return DnaTools.toString(this);
    }

    public long longHashCode() {
        return DnaTools.longHashCode(this);
    }

    @Override
    public int compareTo(LightDna o) {
        return DnaTools.compare(this, o);
    }
}
