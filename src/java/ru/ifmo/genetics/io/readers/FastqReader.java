package ru.ifmo.genetics.io.readers;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.DnaQBuilder;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;
import ru.ifmo.genetics.utils.iterators.ReadersIterator;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;

public class FastqReader implements NamedSource<DnaQ> {
	protected final File f;
	private final QualityFormat qf;
    protected String libraryName;

    public FastqReader(File f, QualityFormat qf) {
		this.f = f;
		this.qf = qf;
        libraryName = FileUtils.removeExtension(f.getName(), ".fastq", ".fq");
	}

	@Override
	public ProgressableIterator<DnaQ> iterator() {
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

    class MyIterator extends ReadersIterator<DnaQ> {
		private BufferedReader br;

        public MyIterator(InputStream is, FileChannel fc) throws IOException {
            super(fc);
            this.br = new BufferedReader(new InputStreamReader(is), 1 << 18);   // 0.5 Mb buffer
        }


        @Override
        protected DnaQ readNext() throws IOException {
            // reading
            String sData = readNextDataLine();
            if (sData == null) {
                return null;
            }
            char[] data = sData.toCharArray();

            String sQual = readNextDataLine();
            if (sQual == null) {
                throw new InputMismatchException("Unexpected end of file. File is corrupted/Format mismatch.");
            }
            char[] qual = sQual.toCharArray();

            if (data.length != qual.length) {
                throw new InputMismatchException("Bad dnaq record: length of chars and quality is not the same. " +
                        "File is corrupted/Format mismatch.");
            }

            // parsing
            DnaQBuilder builder = new DnaQBuilder(data.length);
            for (int i = 0; i < data.length; i++) {
                if (data[i] == 'N' || data[i] == 'n' || data[i] == '.') {
                    builder.unsafeAppendUnknown();
                } else {
                    builder.unsafeAppend(DnaTools.fromChar(data[i]), qf.getPhred(qual[i]));
                }
            }
            return builder.build();
        }

        private String readNextDataLine() throws IOException {
            if (br == null) {
                return null;
            }

            String s = br.readLine();
            while (s != null && s.length() == 0) {      // skipping empty lines
                s = br.readLine();
            }
            if (s == null) {
                br.close();
                br = null;
                fc = null;
                return null;
            }
            if (!isComment(s)) {
                throw new RuntimeException("Unknown structure of fastq file! Waiting \"@ID\" or \"+ID\" string, found \"" +
                        (s.length() > 20 ? s.substring(0,20) + "..." : s) + "\".\n" +
                        "Possibly file is corrupted/format mismatch.");
            }

            s = br.readLine();  // data
            if (s == null) {
                throw new InputMismatchException("Unexpected end of file. File is corrupted/Format mismatch.");
            }
            return s;
        }

        private boolean isComment(String s) {
            return s.startsWith("@") || s.startsWith("+");
        }

    }
}
