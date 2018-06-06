package ru.ifmo.genetics.io.sources;

import ru.ifmo.genetics.utils.iterators.ProgressableIterator;

import java.util.NoSuchElementException;

public class ConsecutiveSource<D> implements Source<D> {
	Source<D>[] sources;
	
	public ConsecutiveSource(Source<D>... sources) {
		this.sources = sources;
	}

	public ProgressableIterator<D> iterator() {
		return new MyIterator();
	}
	
	class MyIterator implements ProgressableIterator<D> {
		private ProgressableIterator<D> iterator;
		private int current;

		@Override
		public boolean hasNext() {
			while (iterator == null || !iterator.hasNext()) {
				if (current == sources.length) {
					return false;
				}
				iterator = sources[current].iterator();
				current++;
			}
			return true;
		}

		@Override
		public D next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			return iterator.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

        @Override
        public double progress() {
            if (iterator == null) {
                return 0;
            }
            return (current + iterator.progress()) / sources.length;
        }
    }
}
