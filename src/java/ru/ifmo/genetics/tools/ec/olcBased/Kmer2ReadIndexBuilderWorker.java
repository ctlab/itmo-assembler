package ru.ifmo.genetics.tools.ec.olcBased;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.ints.*;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.kmers.ShortKmer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Kmer2ReadIndexBuilderWorker implements Runnable {

    private boolean interrupted = false;

    private final Long2IntMap kmers;
    private final List<LongList> chains;
    private final Long2IntMap kmer2chain;
    private final Long2IntMap kmer2ind;

    final int len;
    final Kmer2ReadIndexBuilderDispatcher dispatcher;
    final ByteList[] index;

    final CountDownLatch latch;

    final List<DnaQ> dl = new ArrayList<DnaQ>();
    final LongList pl = new LongArrayList();

    public Kmer2ReadIndexBuilderWorker(Long2IntMap kmers, List<LongList> chains, Long2IntMap kmer2chain, Long2IntMap kmer2ind, 
                                       int len, int totalChains,
                                       Kmer2ReadIndexBuilderDispatcher dispatcher, CountDownLatch latch) {
        this.kmers = kmers;
        this.chains = chains;
        this.kmer2chain = kmer2chain;
        this.kmer2ind = kmer2ind;
        this.len = len;
        this.dispatcher = dispatcher;
        index = new ByteList[totalChains];
        this.latch = latch;
    }

    public void interrupt() {
        interrupted = true;
    }

    @Override
    public void run() {
        while (!interrupted) {
            dispatcher.getWorkRange(dl, pl);
            if (dl.isEmpty()) {
                break;
            }
            process(dl, pl);
        }
        latch.countDown();
    }

    private void process(List<DnaQ> dl, LongList pl) {
        for (int i = 0; i < dl.size(); ++i) {
            DnaQ dnaq = dl.get(i);
            long pos = pl.get(i) - dnaq.length - 4;
            int j = -1;
            IntSet added = new IntOpenHashSet();
            for (ShortKmer kmer : ShortKmer.kmersOf(dnaq, len)) {
                long l = kmer.toLong();
                ++j;
                if (!kmers.containsKey(l)) {
                    continue;
                }
                int chainFW = kmer2chain.get(kmer.fwKmer());
                int chainRC = kmer2chain.get(kmer.rcKmer());

                if (kmer.fwKmer() == kmer.toLong()) {
                    if (!added.contains(chainFW)) {
                        simpleAdd(chainFW, pos, j + kmer2ind.get(kmer.fwKmer()), false);
                        added.add(chainFW);
                    }
                } else {
                    if (!added.contains(chainRC)) {
                        simpleAdd(chainRC, pos, dnaq.length() - len - j - kmer2ind.get(kmer.rcKmer()), true);
                        added.add(chainRC);
                    }
                }
                // System.err.println("added " + pos + " to " + chainID);
                /*
                for (long cur = kmer.fwKmer(); ; cur = kmer2kmer.get(cur)) {
                    if (!added.contains(curPos)) {
                        ShortKmer sk = new ShortKmer(cur, len);
                        if (cur == sk.toLong()) {
                            simpleAdd(sk.toLong(), pos, curPos, false);
                        } else {
                            simpleAdd(sk.toLong(), pos, dnaq.length() - len - curPos, true);
                        }
                    }
                    added.add(curPos);
                    curPos += kmer2pos.get(cur);
                    if (kmer2kmer.get(cur) == cur) {
                        break;
                    }
                }
                */
            }
        }
    }

    private void simpleAdd(int key, long lvalue, int ivalue, boolean bvalue) {
        int ind = key;
        if (index[ind] == null) {
            index[ind] = new ByteArrayList(16);
        }
        for (int i = 0; i < 8; ++i) {
            index[ind].add((byte)((lvalue >>> (8 * (7 - i))) & 255));
        }
        for (int i = 0; i < 4; ++i) {
            index[ind].add((byte)((ivalue >>> (8 * (3 - i))) & 255));
        }
        index[ind].add((byte)(bvalue ? 1 : 0));
    }

}
