package ru.ifmo.genetics.tools.ec.olcBased;

import it.unimi.dsi.fastutil.longs.LongList;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.io.readers.BinqReader;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Kmer2ReadIndexBuilderDispatcher {

    BinqReader[] readers;
    int ind = -1;
    BinqReader.MyIterator iterator = null;
    final int workRange;
    long totalPos = 0;

    DnaQ nextDnaq;
    long nextPos;

    boolean eof = false;

    public Kmer2ReadIndexBuilderDispatcher(File[] files, int workRange) throws IOException {
        readers = new BinqReader[files.length];
        for (int i = 0; i < readers.length; ++i) {
            readers[i] = new BinqReader(files[i]);
        }
        this.workRange = workRange;
    }

    public synchronized void getWorkRange(List<DnaQ> dl, LongList pl) {
        dl.clear();
        pl.clear();

        for (int i = 0; i < workRange; ++i) {
            next();
            if (nextDnaq == null) {
                break;
            }
            dl.add(nextDnaq);
            pl.add(nextPos);
        }
    }

    public long pos() {
        return totalPos + iterator.position();
    }

    private void next() {
        if (eof) {
            return;
        }
        if ((iterator == null) || (!iterator.hasNext())) {
            ++ind;
            if (ind == readers.length) {
                nextDnaq = null;
                nextPos = -1;
                eof = true;
                return;
            }
            if (iterator != null) {
                totalPos += iterator.position();
            }
            iterator = readers[ind].iterator();
        }
        if (iterator.hasNext()) {
            nextDnaq = iterator.next();
            nextPos = iterator.position() + totalPos;
        } else {
            nextDnaq = null;
            nextPos = -1;
        }
    }

}
