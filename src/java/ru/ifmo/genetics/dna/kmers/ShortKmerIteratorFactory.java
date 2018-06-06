package ru.ifmo.genetics.dna.kmers;

import ru.ifmo.genetics.dna.LightDna;

public class ShortKmerIteratorFactory implements KmerIteratorFactory {

    @Override
    public Iterable<ShortKmer> kmersOf(LightDna d, int k) {
        return ShortKmer.kmersOf(d, k);
    }

}
