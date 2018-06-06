package ru.ifmo.genetics.tools.ec.simple;

import it.unimi.dsi.fastutil.longs.LongList;
import ru.ifmo.genetics.tools.io.LazyLongReader;

import java.io.EOFException;
import java.io.IOException;

public class ReadFixesDispatcher {

    LazyLongReader reader;
    int workRangeSize;

    public ReadFixesDispatcher(LazyLongReader reader, int workRangeSize) {
        this.reader = reader;
        this.workRangeSize = workRangeSize;
    }

    public void getWorkRange(LongList kmers, LongList fixes) {
        kmers.clear();
        fixes.clear();
        try {
            byte[] ar;
            synchronized (reader) {
                ar = reader.read();
            }
            for (int i = 0; i < ar.length; i += 16) {
                long kmer = 0;
                for (int j = 0; j < 8; ++j) {
                    int c = ar[i + j];
                    if (c < 0) {
                        c += 256;
                    }
                    kmer = (kmer << 8) + c;
                }
                long fix = 0;
                for (int j = 8; j < 16; ++j) {
                    int c = ar[i + j];
                    if (c < 0) {
                        c += 256;
                    }
                    fix = (fix << 8) + c;
                }
                kmers.add(kmer);
                fixes.add(fix);
            }
        } catch (EOFException e) {
            return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
