package ru.ifmo.genetics.dna;

public interface InplaceDnaQFactory {
    public int get(byte[] buf, int offset, DnaQ dnaq);
}
