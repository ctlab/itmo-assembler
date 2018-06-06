package ru.ifmo.genetics.dna.kmers;

import ru.ifmo.genetics.dna.LightDna;

public class MutableBigKmerIteratorFactory implements KmerIteratorFactory {

    @Override
    public Iterable<MutableBigKmer> kmersOf(LightDna d, int k) {
        return MutableBigKmer.kmersOf(d, k);
    }

}
