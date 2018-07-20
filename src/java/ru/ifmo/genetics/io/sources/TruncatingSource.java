package ru.ifmo.genetics.io.sources;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;

import java.util.Iterator;

public class TruncatingSource implements NamedSource<DnaQ> {
	NamedSource internalSource;
    int phredThreshold;

    public long getSumLen() {
        return sumLen;
    }

    public long getSumTrustLen() {
        return sumTrustLen;
    }

    @Override
    public String name() {
        return internalSource.name() + " (truncated)";
    }

    private long sumLen;
    private long sumTrustLen;

	public TruncatingSource(NamedSource internalSource, int phredThreshold) {
        this.internalSource = internalSource;
        this.phredThreshold = phredThreshold;
	}


	@Override
	public ProgressableIterator<DnaQ> iterator() {
        return new MyIterator(internalSource.iterator());
	}

	class MyIterator implements Iterator<DnaQ>, ProgressableIterator<DnaQ> {
        ProgressableIterator<DnaQ> internaIterator;

		public MyIterator(ProgressableIterator<DnaQ> internaIterator) {
            this.internaIterator = internaIterator;
		}

		@Override
		public boolean hasNext() {
            return internaIterator.hasNext();
        }

		@Override
		public DnaQ next() {
		    DnaQ dnaq = internaIterator.next();
            sumLen += dnaq.length();

            DnaQ dnaqT = dnaq.inplaceTruncateByQuality(phredThreshold);
		    sumTrustLen += dnaqT.length();
		    
            return dnaqT;
		}

		@Override
		public void remove() {
            internaIterator.remove();
		}

        @Override
        public double progress() {
            return internaIterator.progress();
        }
    }
}

