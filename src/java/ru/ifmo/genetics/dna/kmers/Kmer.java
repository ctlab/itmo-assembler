package ru.ifmo.genetics.dna.kmers;

import ru.ifmo.genetics.dna.LightDna;

public interface Kmer extends LightDna {
    /**
     * Converts kmer to long regardless of direction
     */
    public long toLong();
}
