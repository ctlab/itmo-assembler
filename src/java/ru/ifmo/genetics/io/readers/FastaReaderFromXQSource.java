package ru.ifmo.genetics.io.readers;

import org.apache.log4j.Logger;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.io.sources.Source;
import ru.ifmo.genetics.utils.NumUtils;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;
import ru.ifmo.genetics.utils.tool.Tool;

import java.io.File;
import java.io.IOException;
import java.lang.String;
import java.util.NoSuchElementException;

/**
 * Reading dnas from dnaQs. Skipping all dnaQs with at least one 'N' nucleotide.
 */
public class FastaReaderFromXQSource implements NamedSource<Dna> {
    protected final Logger logger = Logger.getLogger("reader");
    private final NamedSource<DnaQ> source;
    private final String fileName;

    public FastaReaderFromXQSource(NamedSource<DnaQ> source) {
        this(source, null);
    }
    public FastaReaderFromXQSource(NamedSource<DnaQ> source, File file) {
        this.source = source;
        fileName = (file != null) ? file.getName() : null;
    }


    @Override
    public ProgressableIterator<Dna> iterator() {
        try {
            return new MyIterator(source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return source.name();
    }


    class MyIterator implements ProgressableIterator<Dna> {
        private long allReads = 0, skipped = 0;

        ProgressableIterator<DnaQ> iterator;

        public MyIterator(Source<DnaQ> source) throws IOException {
            iterator = source.iterator();
        }

        private Dna next = null;
        private boolean printedInfo = false;

        @Override
        public boolean hasNext() {
            while (next == null && iterator.hasNext()) {
                DnaQ dnaQ = iterator.next();
                boolean good = true;
                for (int i = 0; i < dnaQ.length() && good; i++) {
                    if (dnaQ.phredAt(i) == 0) {
                        good = false;
                    }
                }
                allReads++;
                if (good) {
                    next = new Dna(dnaQ);
                } else {
                    skipped++;
                }
            }
            if (next == null && !printedInfo && (skipped != 0)) { // no next element, i.e. at the end of file
                Tool.debug(logger, "Skipped " + NumUtils.groupDigits(skipped) + " (" + String.format("%.1f", skipped * 100.0 / allReads) + "%) " +
                        "out of " + NumUtils.groupDigits(allReads) + " reads (because of N nucleotide), " +
                        (fileName != null ? "file " + fileName : "source " + name()));
                printedInfo = true;
            }
            return next != null;
        }

        @Override
        public Dna next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
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
