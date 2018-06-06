package ru.ifmo.genetics.tools.ec;

import org.apache.log4j.Logger;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.kmers.*;
import ru.ifmo.genetics.dna.kmers.KmerIteratorFactory;
import ru.ifmo.genetics.structures.map.ArrayLong2IntHashMap;
import ru.ifmo.genetics.utils.tool.Tool;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class KmerLoadWorker implements Runnable {

    private DnaQReadDispatcher dispatcher;
    CountDownLatch latch;
    int LEN;
    long maxSize;
    long step;
    ArrayLong2IntHashMap hm;
    KmerIteratorFactory<? extends Kmer> factory;

    long prefix;
    long prefixMask;
    int prefixLength;

    boolean interrupted = false;

    Random random;

    Logger logger;

    public KmerLoadWorker(DnaQReadDispatcher dispatcher, CountDownLatch latch, Random random,
                          int LEN, long maxSize, ArrayLong2IntHashMap hm,
                          long prefix, long prefixMask, int prefixLength,
                          KmerIteratorFactory<? extends Kmer> factory) {

        this.dispatcher = dispatcher;
        this.latch = latch;
        this.LEN = LEN;
        this.maxSize = maxSize;
        this.hm = hm;
        this.random = random;

        this.prefix = prefix;
        this.prefixMask = prefixMask;
        this.prefixLength = prefixLength;

        step = this.maxSize >> 1;

        this.factory = factory;
    }

    public void add(Kmer kmer) {
        long fwPrefix = 0;
        long rcPrefix = 0;
        for (int i = 0; i < prefixLength; ++i) {
            fwPrefix = (fwPrefix << 2) | kmer.nucAt(i);
            rcPrefix = (rcPrefix << 2) | (3 - kmer.nucAt(kmer.length() - 1 - i));
        }
        fwPrefix <<= 2 * (kmer.length() - prefixLength);
        rcPrefix <<= 2 * (kmer.length() - prefixLength);
        if ((fwPrefix != prefix) && (rcPrefix != prefix)) {
            return;
        }

        long key = kmer.toLong();
        hm.add(key, 1);
        if (hm.size() > maxSize) {
            long toAdd = random.nextInt(4);
            Tool.debug(logger, "trim (" + toAdd + ")");
            prefix |= toAdd << (2 * (LEN - prefixLength - 1));
            prefixMask |= 3L << (2 * (LEN - prefixLength - 1));
            ++prefixLength;
            maxSize += step;
            step >>= 1;
        }
    }

    void add(DnaQ dnaq) {
        for (Kmer kmer : factory.kmersOf(dnaq, LEN)) {
            add(kmer);
        }
    }

    void add(Iterable<DnaQ> dnaqs) {
        for (DnaQ dnaq : dnaqs) {
            add(dnaq);
        }
    }

    public void interrupt() {
        interrupted = true;
    }

    public void run() {
        logger = Logger.getLogger("worker-" + Thread.currentThread().getId());
        while (!interrupted) {
            List<DnaQ> list = dispatcher.getWorkRange();
            if (list == null) {
                break;
            }

            add(list);
        }
        latch.countDown();
    }

    public long getPrefixMask(){
        return prefixMask;
    }

    public int getPrefixLength(){
        return prefixLength;
    }

}
