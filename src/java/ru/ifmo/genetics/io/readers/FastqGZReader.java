package ru.ifmo.genetics.io.readers;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.DnaQBuilder;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

public class FastqGZReader extends FastqReader {

    public FastqGZReader(File f, QualityFormat qf) {
        super(f, qf);
        libraryName = FileUtils.removeExtension(f.getName(), ".fastq.gz", ".fq.gz");
	}

	@Override
	public ProgressableIterator<DnaQ> iterator() {
		try {
            FileInputStream fis = new FileInputStream(f);
            InputStream is = new GZIPInputStream(fis, 1 << 20);     // 1 Mb buffer
            return new MyIterator(is, fis.getChannel());
		} catch (IOException e) {
			throw new RuntimeException(e);
        }
    }
}
