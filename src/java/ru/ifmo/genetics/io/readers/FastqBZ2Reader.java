package ru.ifmo.genetics.io.readers;

import org.apache.hadoop.io.compress.BZip2Codec;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class FastqBZ2Reader extends FastqReader {

    public FastqBZ2Reader(File f, QualityFormat qf) {
        super(f, qf);
        libraryName = FileUtils.removeExtension(f.getName(), ".fastq.bz2", ".fq.bz2");
	}

	@Override
	public ProgressableIterator<DnaQ> iterator() {
		try {
            FileInputStream fis = new FileInputStream(f);
            InputStream is = new BZip2Codec().createInputStream(fis);
            return new MyIterator(is, fis.getChannel());
		} catch (IOException e) {
			throw new RuntimeException(e);
        }
    }
}
