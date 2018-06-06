package ru.ifmo.genetics.dna.kmers;

import ru.ifmo.genetics.dna.kmers.Kmer;

public interface BigKmer extends Kmer {
    public long fwLongHashCode();
    public long rcLongHashCode();

    /**
     * Hash code compatible with equals()
     * @return
     */
    public long longHashCode();

    /**
     * Hash code indifferent to direction
     * @return
     */
    public long biLongHashCode();
}
