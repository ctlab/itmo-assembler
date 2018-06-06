package ru.ifmo.genetics.io.readers;

import org.apache.log4j.Logger;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.NumUtils;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;
import ru.ifmo.genetics.utils.iterators.ReadersIterator;

import java.io.*;
import java.nio.channels.FileChannel;

public class FastaWithNsReader implements NamedSource<String> {
    private final Logger logger = Logger.getLogger("reader");
    protected final File f;
    protected String libraryName;

    public FastaWithNsReader(File f) {
        this.f = f;
        libraryName = FileUtils.removeExtension(f.getName(), ".fasta", ".fa", ".fn", ".fna");
    }

	@Override
	public ProgressableIterator<String> iterator() {
		try {
            FileInputStream fis = new FileInputStream(f);
			return new MyIterator(fis, fis.getChannel());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

    @Override
    public String name() {
        return libraryName;
    }

    class MyIterator extends ReadersIterator<String> {
        private long allReads = 0, skipped = 0;
        private BufferedReader br;
        private StringBuilder sb;

        public MyIterator(InputStream is, FileChannel fc) throws IOException {
            super(fc);
            this.br = new BufferedReader(new InputStreamReader(is));
            sb = new StringBuilder();
        }


        @Override
        protected String readNext() throws IOException {
            String s = readNextDataLine();
            if (s != null) {
                allReads++;
            }
            return s;
        }

        protected String readNextDataLine() throws IOException {
            if (br == null) {
                return null;
            }

            sb.setLength(0);
            while (true) {
                String s = br.readLine();
                if (s == null) {
                    br.close();
                    br = null;
                    fc = null;
                    break;
                }
                if (isComment(s)) {
                    if (sb.length() > 0) {
                        break;
                    }
                } else {
                    sb.append(s);
                }
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
            return null;
        }

        private boolean isComment(String s) {
            return s.startsWith(">") || s.startsWith(";");
        }
    }
}
