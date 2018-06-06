package ru.ifmo.genetics.io.readers;

import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

public class FastaGZReader extends FastaReader {

	public FastaGZReader(File f) {
        super(f);
        libraryName = FileUtils.removeExtension(f.getName(),  ".fasta.gz", ".fa.gz", ".fn.gz", ".fna.gz");
    }

	@Override
	public ProgressableIterator<Dna> iterator() {
		try {
            FileInputStream fis = new FileInputStream(f);
            InputStream is = new GZIPInputStream(fis);
			return new MyIterator(is, fis.getChannel());
		} catch (IOException e) {
			throw new RuntimeException(e);
        }
    }
}
