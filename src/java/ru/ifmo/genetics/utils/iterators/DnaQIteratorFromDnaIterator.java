package ru.ifmo.genetics.utils.iterators;

import ru.ifmo.genetics.dna.DnaQ;

public class DnaQIteratorFromDnaIterator implements ProgressableIterator<DnaQ> {
    private ProgressableIterator<String> dnaIterator;
    private int phred;

    public DnaQIteratorFromDnaIterator(ProgressableIterator<String> dnaIterator, int phred) {
        this.dnaIterator = dnaIterator;
        this.phred = phred;
    }

    @Override
    public boolean hasNext() {
        return dnaIterator.hasNext();
    }

    @Override
    public DnaQ next() {
        return new DnaQ(dnaIterator.next(), phred);
    }

    @Override
    public void remove() {
    }

    @Override
    public double progress() {
        return dnaIterator.progress();
    }
}
