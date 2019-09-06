package ru.ifmo.genetics.io.readers;

import org.apache.log4j.Logger;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.io.sources.Source;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Created by -- on 30.10.2018.
 */
/**
 * Reading dnas from dnaQs. Splits all dnaQs by 'N' nucleotide.
 */
public class FastaReaderFromXQSourceTrunc implements NamedSource<Dna> {
    protected final Logger logger = Logger.getLogger("reader");
    private final NamedSource<DnaQ> source;
    private final String fileName;

    public FastaReaderFromXQSourceTrunc(NamedSource<DnaQ> source) {
        this(source, null);
    }
    public FastaReaderFromXQSourceTrunc(NamedSource<DnaQ> source, File file) {
        this.source = source;
        fileName = (file != null) ? file.getName() : null;
    }


    @Override
    public ProgressableIterator<Dna> iterator() {
        try {
            return new FastaReaderFromXQSourceTrunc.MyIterator(source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return source.name();
    }


    class MyIterator implements ProgressableIterator<Dna> {
        ProgressableIterator<DnaQ> iterator;

        public MyIterator(Source<DnaQ> source) throws IOException {
            iterator = source.iterator();
        }

        private Dna next = null;
        private DnaQ nextPart = null;

        @Override
        public boolean hasNext() {
            while (nextPart == null && next == null && iterator.hasNext()) {
                DnaQ dnaQ = iterator.next();
                DnaQ goodDnaQ = dnaQ.truncateByQuality(1);
                next = new Dna(goodDnaQ);
                if (dnaQ.length() > goodDnaQ.length() + 1) {
                    nextPart = new DnaQ(Arrays.copyOfRange(dnaQ.value, goodDnaQ.length + 1, dnaQ.length));
                } else {
                    nextPart = null;
                }
            }

            return next != null;
        }

        @Override
        public Dna next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (nextPart != null) {
                DnaQ resQ = nextPart.truncateByQuality(1);
                Dna res = new Dna(resQ);
                if (nextPart.length() > resQ.length() + 1) {
                    nextPart = new DnaQ(Arrays.copyOfRange(nextPart.value, resQ.length + 1, nextPart.length));
                } else {
                    nextPart = null;
                }
                return res;
            }
            Dna res = next;
            next = null;
            return res;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double progress() {
            return iterator.progress();
        }
    }
}

