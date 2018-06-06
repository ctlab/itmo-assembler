package ru.ifmo.genetics.io.readers;

import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class FastaWithNsGZReader extends FastaWithNsReader {

	public FastaWithNsGZReader(File f) {
        super(f);
        libraryName = FileUtils.removeExtension(f.getName(),  ".fasta.gz", ".fa.gz", ".fn.gz", ".fna.gz");
    }

	@Override
	public ProgressableIterator<String> iterator() {
		try {
            FileInputStream fis = new FileInputStream(f);
            InputStream is = new GZIPInputStream(fis);
			return new MyIterator(is, fis.getChannel());
		} catch (IOException e) {
			throw new RuntimeException(e);
        }
    }
}
