package ru.ifmo.genetics.io.readers;

import org.apache.hadoop.io.compress.BZip2Codec;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FastaWithNsBZ2Reader extends FastaWithNsReader {

	public FastaWithNsBZ2Reader(File f) {
        super(f);
        libraryName = FileUtils.removeExtension(f.getName(),  ".fasta.bz2", ".fa.bz2", ".fn.bz2", ".fna.bz2");
    }

	@Override
	public ProgressableIterator<String> iterator() {
		try {
            FileInputStream fis = new FileInputStream(f);
            InputStream is = new BZip2Codec().createInputStream(fis);
			return new MyIterator(is, fis.getChannel());
		} catch (IOException e) {
			throw new RuntimeException(e);
        }
    }
}
