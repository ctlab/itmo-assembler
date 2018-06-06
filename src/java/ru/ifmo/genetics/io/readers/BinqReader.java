package ru.ifmo.genetics.io.readers;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;
import ru.ifmo.genetics.utils.iterators.ReadersIterator;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.NoSuchElementException;

public class BinqReader implements NamedSource<DnaQ> {
	private File f;
    private String libraryName;

	public BinqReader(File f) {
		this.f = f;
        libraryName = FileUtils.removeExtension(f.getName(), ".binq");
	}

    public BinqReader(String s) throws IOException {
        this(new File(s));
    }

	@Override
	public MyIterator iterator() {
		try {
            FileInputStream is = new FileInputStream(f);
            return new MyIterator(is, is.getChannel());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

    @Override
    public String name() {
        return libraryName;
    }

    public class MyIterator extends ReadersIterator<DnaQ> {
		private InputStream in;
        private long position;  // exact position!

		public MyIterator(InputStream is, FileChannel fc) throws IOException {
            super(fc);
            in = new BufferedInputStream(is);
            position = 0;
		}


        @Override
        protected DnaQ readNext() throws IOException {
            if (in == null) {
                return null;
            }

            int ch1 = in.read();
            while (ch1 == 255) {
                ch1 = in.read();
                position++;
            }
            if (ch1 == -1) {
                in.close();
                in = null;
                fc = null;
                return null;
            }
            int ch2 = in.read();
            int ch3 = in.read();
            int ch4 = in.read();
            if (ch2 == -1 || ch3 == -1 || ch4 == -1) {
                throw new RuntimeException("Unexpected end of file " + f.getName());
            }
            int len = (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0);

            DnaQ res = DnaQ.getPrototype(len);

            int x = 0;
            do {
                int t = in.read(res.value, x, len - x);
                if (t == -1)
                    throw new RuntimeException("Unexpected end of file " + f.getName());
                x += t;
            } while (x < len);
            position += len + 4;
            return res;
        }

        /**
         * Not standard method, it is only in this class!
         */
        @Deprecated
        public long position() {
            return position;
        }
    }
}

