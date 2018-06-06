package ru.ifmo.genetics.io.readers;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.utils.iterators.DnaQIteratorFromDnaIterator;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;

public class DnaQReaderFromDnaSource implements NamedSource<DnaQ> {
    private NamedSource<String> dnaSource;
    private int phred;

    public DnaQReaderFromDnaSource(NamedSource<String> dnaSource) {
        this(dnaSource, 0);
    }

    public DnaQReaderFromDnaSource(NamedSource<String> dnaSource, int phred) {
        this.dnaSource = dnaSource;
        this.phred = phred;
    }

    @Override
    public ProgressableIterator<DnaQ> iterator() {
        return new DnaQIteratorFromDnaIterator(dnaSource.iterator(), phred);
    }

    @Override
    public String name() {
        return dnaSource.name();
    }
}
