package ru.ifmo.genetics.dna.kmers;

import ru.ifmo.genetics.dna.LightDna;

public interface KmerIteratorFactory<T extends Kmer> {

    public Iterable<T> kmersOf(LightDna l, int k);

}