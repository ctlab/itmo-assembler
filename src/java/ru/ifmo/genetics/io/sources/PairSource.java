package ru.ifmo.genetics.io.sources;

import ru.ifmo.genetics.dna.LightDnaQ;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;
import ru.ifmo.genetics.utils.pairs.UniPair;

import java.util.NoSuchElementException;

public class PairSource<D> implements Source<UniPair<D>> {
	Source<? extends D> s1, s2;

	public PairSource(Source<? extends D> s1, Source<? extends D> s2) {
		this.s1 = s1;
		this.s2 = s2;
	}

	@Override
	public ProgressableIterator<UniPair<D>> iterator() {
		return new MyIteraror(s1.iterator(), s2.iterator());
	}

    public static <T> PairSource<T> create(NamedSource<? extends T> source1, NamedSource<? extends T> source2) {
        return new PairSource<T>(source1, source2);
    }

    class MyIteraror implements ProgressableIterator<UniPair<D>> {
		ProgressableIterator<? extends D> i1, i2;

		public MyIteraror(ProgressableIterator<? extends D> i1, ProgressableIterator<? extends D> i2) {
			this.i1 = i1;
			this.i2 = i2;
		}

		@Override
		public boolean hasNext() {
			return i1.hasNext() && i2.hasNext();
		}

		@Override
		public UniPair<D> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			return new UniPair<D>(i1.next(), i2.next());
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

        @Override
        public double progress() {
            return (i1.progress() + i2.progress()) / 2;
        }
    }
}
