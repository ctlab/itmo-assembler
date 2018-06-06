package ru.ifmo.genetics.io.readers;

import org.apache.hadoop.io.compress.BZip2Codec;
import org.apache.hadoop.io.compress.bzip2.BZip2Constants;
import org.apache.hadoop.io.compress.bzip2.BZip2DummyCompressor;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class FastaBZ2Reader extends FastaReader {

	public FastaBZ2Reader(File f) {
        super(f);
        libraryName = FileUtils.removeExtension(f.getName(),  ".fasta.bz2", ".fa.bz2", ".fn.bz2", ".fna.bz2");
    }

	@Override
	public ProgressableIterator<Dna> iterator() {
		try {
            FileInputStream fis = new FileInputStream(f);
            InputStream is = new BZip2Codec().createInputStream(fis);
			return new MyIterator(is, fis.getChannel());
		} catch (IOException e) {
			throw new RuntimeException(e);
        }
    }
}
