package ru.ifmo.genetics.tools.ec.olcBased;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.bytes.*;
import it.unimi.dsi.fastutil.booleans.*;

import ru.ifmo.genetics.io.*;
import ru.ifmo.genetics.dna.*;
import ru.ifmo.genetics.dna.kmers.*;
import ru.ifmo.genetics.utils.KmerUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class NewCleanWorker implements Runnable {

    static int readCorrectingTimes = Integer.MAX_VALUE;

    final List<LongList> chains;
    final Long2IntMap kmer2chain;
    final Long2IntMap kmer2ind;

    final CountDownLatch latch;
    final NewCleanDispatcher dispatcher;
    final MultiFile2MemoryMap mf;
    final int len;
    final Long2IntMap times;

    LongSet set;
    boolean interrupted = false;

    final int err;

    public NewCleanWorker(CountDownLatch latch, NewCleanDispatcher dispatcher,
                          List<LongList> chains, Long2IntMap kmer2chain, Long2IntMap kmer2ind,
                          MultiFile2MemoryMap mf, int len, Long2IntMap times, int err) throws IOException {
        this.latch = latch;
        this.dispatcher = dispatcher;
        this.chains = chains;
        this.kmer2chain = kmer2chain;
        this.kmer2ind = kmer2ind;
        this.mf = mf;
        this.len = len;
        this.times = times;
        this.err = err;
    }

    public void interrupt() {
        interrupted = true;
    }

    @Override
    public void run() {
        while (!interrupted) {
            List<byte[]> list = dispatcher.getWorkRange();
            if (list == null) {
                break;
            }
            for (byte[] ar : list) {
                try {
                    processKmer(len, ar, mf, times);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        latch.countDown();
    }

    public void processKmer(int len, byte[] array, MultiFile2MemoryMap mf, Long2IntMap times) throws IOException {
        ++Kmer2ReadIndexBuilder.kmersProcessed;


        List<byte[]> dnaqs = new ArrayList<byte[]>();
        IntList starts = new IntArrayList();
        LongList poses = new LongArrayList();
        IntList lengths = new IntArrayList();
        BooleanList fws = new BooleanArrayList();
        int chain = 0;
        for (int i = 0; i < 4; ++i) {
            int x = array[i];
            if (x < 0) x += 256;
            chain = (chain << 8) + x;
        }
        /*
        ShortKmer KMER = new ShortKmer(kmer, len);
        if (KMER.toLong() != kmer) {
            System.err.println("HERE");
            while (true);
        }
        */
        boolean verbose = false;
        /*
        for (int i = 4; i < array.length; i += 13) {
            long pos = 0;
            for (int j = 0; j < 8; ++j) {
                int x = array[i + j];
                if (x < 0) x += 256;
                pos = (pos << 8) + x;
            }
            verbose |= pos == 9142;
            byte[] dnaqBytes = readDnaQ(mf, pos);
            verbose |= (new DnaQ(dnaqBytes).nucEquals(new Dna("GCATTGGTTGTTACTCTTTACCTTTGGTCGAAAAAAAAAGCCCGCACTGTAGGGTCGGCTTTTTTTCTGTGTTTCCTGTACGCGTCAGACCGCACCGTT")));
            if (verbose) break;
        }
        */
        if (verbose) {
            System.err.println("chain = " + chain);
        }
        /*
        verbose |= kmer == 257764947132L;
        if (kmer == 257764947132L) {
            System.err.println("read kmer: " + KmerUtils.kmer2String(kmer, len) + " " + kmer + "; size = " + array.length);
        }
        */
        int minStart = Integer.MAX_VALUE;
        for (int i = 4; i < array.length; i += 13) {
            long pos = 0;
            for (int j = 0; j < 8; ++j) {
                int x = array[i + j];
                if (x < 0) x += 256;
                pos = (pos << 8) + x;
            }
            byte[] dnaqBytes = readDnaQ(mf, pos);
            int dnaqLen = dnaqBytes.length;
            DnaQ d = new DnaQ(dnaqBytes);

            //verbose |= d.nucEquals(new Dna("TCCGCCCTCTACCGGAATACCCAGCGGATGGAAGAAATCGACAGCCTCTTTGGTTAAACGAATATTTTCGTT"));

            int inReadPos = 0;
            for (int j = 0; j < 4; ++j) {
                int x = array[i + j + 8];
                if (x < 0) x += 256;
                inReadPos = (inReadPos << 8) + x;
            }
            boolean fw = array[i + 12] == 0;

            synchronized (times) {
                int old = times.containsKey(pos) ? times.get(pos) : 0;
                if (old > readCorrectingTimes) {
                    ++Kmer2ReadIndexBuilder.readsSkipped;
                    continue;
                }
                times.put(pos, old + 1);
            }
            ++Kmer2ReadIndexBuilder.readsProcessed;
            //
            if (!fw) {
                reverseComplement(dnaqBytes);
            }
            int old = inReadPos;
            inReadPos = check(dnaqBytes, len, -inReadPos, chain);
            if (inReadPos == Integer.MAX_VALUE) {
                ++Kmer2ReadIndexBuilder.readsChanged;
                continue;
            }
            minStart = Math.min(minStart, -inReadPos);
            dnaqs.add(dnaqBytes);
            starts.add(inReadPos);
            poses.add(pos);
            fws.add(fw);
            lengths.add(dnaqLen);
            if (dnaqs.size() > 500) {
                return;
            }
            if (verbose) {
                System.err.println("pos = " + pos + "; " + (fw ? "FW" : "RC"));
            }
        }
        if (dnaqs.isEmpty()) {
            return;
        }
        int max = 0;
        for (int i = 0; i < starts.size(); ++i) {
            starts.set(i, -starts.get(i));
            max = Math.max(max, starts.get(i));
        }
        if (max > 0) {
            if (verbose) {
                System.err.println("max = " + max);
            }
            for (int i = 0; i < starts.size(); ++i) {
                starts.set(i, starts.get(i) - max);
            }
        }
        /*
        verbose = dnaqs.size() > 50;
        if (verbose) {
            System.err.println("size = " + dnaqs.size());
            System.err.println("kmer = " + KmerUtils.kmer2String(kmer, len));
            for (int i = 0; i < dnaqs.size(); ++i) {
                System.err.println(new DnaQ(dnaqs.get(i)));
            }
            while (true);
        }
        */
        processKmer(chain, dnaqs, starts, err, len, verbose);
        for (int j = 0; j < dnaqs.size(); ++j) {
            ++Kmer2ReadIndexBuilder.corrected;
            if (!fws.get(j)) {
                byte[] t = dnaqs.get(j);
                reverseComplement(t);
            }
            //
            long pos = poses.get(j);
            mf.writeDnaQ(pos, new DnaQ(dnaqs.get(j)));
        }

        //System.err.println("---------------------");
        //System.err.println("calc: " + (System.currentTimeMillis() - ct));

    }

    public int check(byte[] b, int len, int start, int chain) {
        DnaQ d = new DnaQ(b);
        int i = 0;
        for (ShortKmer sk : ShortKmer.kmersOf(d, len)) {
            long cur = sk.fwKmer();
            long curRC = sk.rcKmer();
            if (kmer2chain.containsKey(cur) && (kmer2chain.get(cur) == chain)) {
                return i + kmer2ind.get(cur);
            }
            if (kmer2chain.containsKey(curRC) && (kmer2chain.get(curRC) == chain)) {
                return b.length - len - i - kmer2ind.get(cur);
            }
            ++i;
        }
        return Integer.MAX_VALUE;
    }


    // starts are negative

    public void processKmer(int chain, List<byte[]> dnaqs, IntList starts, int md, int len, boolean verbose) {
        int size = dnaqs.size();

        /*
        int minS = Integer.MAX_VALUE;
        int maxS = Integer.MIN_VALUE;
        for (int i = 0; i < size; ++i) {
            minS = Math.min(minS, starts.get(i));
            maxS = Math.max(maxS, starts.get(i));
        }
        System.err.println(minS + " .. " + maxS);
        */

        int maxShift = 0;
        for (int i = 0; i < size; ++i) {
            maxShift = Math.max(maxShift, -starts.get(i));
        }
        int minShift = Integer.MAX_VALUE;
        for (int i = 0; i < size; ++i) {
            minShift = Math.min(minShift, maxShift + starts.get(i) + dnaqs.get(i).length);
        }
        if (maxShift >= minShift) {
//            System.err.println("WHOOOPS!");
            /*
            for (int i = 0; i < size; ++i) {
                int s = maxShift + starts.get(i);
                for (int j = 0; j < s; ++j) {
                    System.err.print(" " );
                }
                System.err.println(new DnaQ(dnaqs.get(i)));
            }
            throw new RuntimeException("maxShift >= minShift: " + maxShift + " " + minShift);
            */
            return;
        }
        int mid = (maxShift + minShift) / 2;

        //
        if (verbose) {
            System.err.print("kmers: ");
            for (long l : chains.get(chain)) {
                System.err.print(KmerUtils.kmer2String(l, len) + " ");
            }
            System.err.println();
            System.err.println("<before>");
            for (int i = 0; i < size; ++i) {
                int s = maxShift + starts.get(i);
                for (int j = 0; j < s; ++j) {
                    System.err.print(" ");
                }
                System.err.println(new DnaQ(dnaqs.get(i)) + " " + starts.get(i));
            }
            System.err.println("</before>");
        }
        //

        List<ByteList> al = new ArrayList<ByteList>();
        List<ByteList> ar = new ArrayList<ByteList>();
        for (int i = 0; i < size; ++i) {
            byte[] b = dnaqs.get(i);
            int s = maxShift + starts.get(i);
            int f = b.length + s;

            ByteList left = new ByteArrayList();
            for (int j = mid; j >= s; --j) {
                left.add(b[j - s]);
            }
            al.add(left);

            if (mid + 1 - s < 0) {
                System.err.println("PANIC PANIC PANIC");
                System.err.println("maxShift = " + maxShift);
                System.err.println("minShift = " + minShift);
                System.err.println("mid = " + mid);
                System.err.println("s = " + s);
                throw new RuntimeException("Something wrong!");
            }

            ByteList right = new ByteArrayList();
            for (int j = mid + 1; j < f; ++j) {
                right.add(b[j - s]);
            }
            ar.add(right);

        }

//        long consTime = 0;
//        long ct = System.currentTimeMillis();
        List<ByteList> left = preConsensus(al, md, verbose);
        //
        if (verbose) {
            // The reads were written in preConsensus
            System.err.println("l---------------------------------------------------------");
        }
        //
//        consTime += System.currentTimeMillis() - ct;
        //
        if (verbose) {
            for (ByteList bl : left) {
                System.err.println(new DnaQ(bl.toByteArray()));
            }
            System.err.println("l+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        }
        //
        fill(al, left, md);
        //
        if (verbose) {
            for (ByteList bl : al) {
                System.err.println(new DnaQ(bl.toByteArray()));
            }
            System.err.println("l=========================================================");
        }
        //

//        ct = System.currentTimeMillis();
        List<ByteList> right = preConsensus(ar, md, false);
        /*
        if (verbose) {
            // The reads were written in preConsensus
            System.err.println("r---------------------------------------------------------");
        }
        */
//        consTime += System.currentTimeMillis() - ct;
        /*
        if (verbose) {
            for (ByteList bl : right) {
                System.err.println(new DnaQ(bl.toByteArray()));
            }
            System.err.println("r+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        }
        */
        fill(ar, right, md);
        /*
        if (verbose) {
            for (ByteList bl : ar) {
                System.err.println(new DnaQ(bl.toByteArray()));
            }
            System.err.println("r=========================================================");
        }
        */

        //System.err.println(al.size() + " " + ar.size() + ": " + left.size() + " " + right.size() + " (" + consTime + ")");


        for (int i = 0; i < size; ++i) {
            if ((al.get(i) == null) || (ar.get(i) == null)) {
                continue;
            }

            int oldLeftSize = mid - maxShift - starts.get(i);
            int newLeftSize = al.get(i).size();
            int delta = newLeftSize - oldLeftSize;

            byte[] newLeft = al.get(i).toByteArray();
            byte[] newRight = ar.get(i).toByteArray();
            byte[] b = dnaqs.get(i);

            starts.set(i, starts.get(i) - delta);
            if (b.length > newLeft.length + newRight.length) {
                b = new byte[newLeft.length + newRight.length];
                dnaqs.set(i, b);
            }

            for (int j = 0; j < b.length; ++j) {
                b[j] = (j < newLeftSize) ? newLeft[newLeftSize - 1 - j] : newRight[j - newLeftSize];
            }
        }
        //
        if (verbose) {
            System.err.println("<after>");
            for (int i = 0; i < size; ++i) {
                int s = 10 + maxShift + starts.get(i);
                for (int j = 0; j < s; ++j) {
                    System.err.print(" ");
                }
                System.err.println(new DnaQ(dnaqs.get(i)));
            }
            System.err.println("</after>");
        }
        //
    }

    int[][] forDist = new int[220][220];

    public void fill(List<ByteList> ar, List<ByteList> ans, int md) {
        int ml1 = 0, ml2 = 0;
        for (ByteList bl : ar) {
            ml1 = Math.max(ml1, bl.size());
        }
        for (ByteList bl : ans) {
            ml2 = Math.max(ml2, bl.size());
        }
        if ((ml1 >= forDist.length) || (ml2 >= forDist[0].length)) {
            ml1 = Math.max(ml1, forDist.length);
            ml2 = Math.max(ml2, forDist[0].length);
            //System.err.println("here " + ml1 + " " + ml2);
            forDist = new int[ml1 + 1][ml2 + 1];
        }

        for (ByteList from : ar) {
            int mind = Integer.MAX_VALUE;
            ByteList fin = null;
            int len = -1;
            for (ByteList to : ans) {
                int l1 = from.size();
                int l2 = to.size();
                int maxL = Math.min(l1 + md, l2);
                if (dist(from.toByteArray(), to.subList(0, maxL).toByteArray(), forDist, mind) > mind) {
                    continue;
                }
                for (int i = l1 >= md ? l1 - md : 0; i <= maxL; ++i) {
                    //int cd = dist(from.toByteArray(), to.subList(0, i).toByteArray(), forDist, mind);
                    int cd = forDist[l1][i];
                    if ((cd < mind) /*|| (cd == mind) && (i > len)*/) {
                        mind = cd;
                        fin = to;
                        len = i;
                    }
                }
            }
            if ((fin == null) || (mind > md)) {
                continue;
            }
            if (from.size() != len) {
                from.size(len);
            }
            for (int i = 0; i < len; ++i) {
                from.set(i, fin.get(i));
            }
        }
    }

    int msize = 500;
    int mlen = 400;
    int[][][] dists = new int[msize][mlen][mlen];

    IntSet emptySet = new IntOpenHashSet();

    int prefixLength = 12;
    int minLength = 8;

    public List<ByteList> preConsensus(List<ByteList> ar, int md, boolean verbose) {
        /*
        System.err.println("preConsensus:");
        for (ByteList bl : ar) {
            System.err.println(new DnaQ(bl.toByteArray()));
        }
        */
        int size = ar.size();
        int[] comp = new int[size];
        int total = 0;
        for (int i = 0; i < size; ++i) {
            ByteList bl = ar.get(i);
            if (bl.size() < minLength) {
                comp[i] = -1;
                continue;
            }
            int c = -1;
            int mind = Integer.MAX_VALUE;
            for (int j = 0; j < i; ++j) {
                ByteList jbl = ar.get(j);
                if (comp[j] == -1) {
                    continue;
                }
                int ml = Math.min(Math.min(bl.size(), jbl.size()), prefixLength);
                int cmind = Math.min(ml / 2, mind);
                int cd = dist(bl.subList(0, ml).toByteArray(), jbl.subList(0, ml).toByteArray(), forDist, cmind);
                if (cd < cmind) {
                    mind = cd;
                    c = comp[j];
                }
            }
            if (mind > md) {
                comp[i] = total++;
            } else {
                comp[i] = c;
            }

            //
            if (verbose) {
                System.err.println(new DnaQ(bl.toByteArray()) + " " + comp[i]);
            }
            //
        }
        List<ByteList> ans = new ArrayList<ByteList>();
        for (int j = 0; j < total; ++j) {
            List<ByteList> nar = new ArrayList<ByteList>();
            for (int i = 0; i < size; ++i) {
                if (comp[i] == j) {
                    nar.add(ar.get(i));
                }
            }
            ans.addAll(consensus(nar, md, verbose));
            if (verbose) System.err.println();
        }
        return ans;
    }

    public List<ByteList> consensus(List<ByteList> ar, int md, boolean verbose) {

        int size = ar.size();

        if (size <= 2) {
            return ar;
        }

        boolean[] masked = new boolean[size];
        int maxLen = 0;
        for (int i = 0; i < size; ++i) {
            ByteList bl = ar.get(i);
            maxLen = maxLen < bl.size() ? bl.size() : maxLen;
            masked[i] = bl.size() < 4;
        }
        if (size > dists.length) {
            msize = size + 50;
        }
        if (maxLen + md >= dists[0].length) {
            mlen = maxLen + md + 50;
        }
        if ((dists.length < msize) || (dists[0][0].length < mlen)) {
            dists = new int[msize][mlen][mlen];
        }

        for (int i = 0; i < size; ++i) {
            for (int j = 0; j <= maxLen; ++j) {
                dists[i][j][0] = j;
            }
            for (int j = 0; j <= maxLen + md; ++j) {
                dists[i][0][j] = j;
            }
        }
        ByteList ans = new ByteArrayList();
        List<ByteList> rem = new ArrayList<ByteList>();

        /*
        IntList lastLengths = new IntArrayList();
        for (int i = 0; i < size; ++i) {
            lastLengths.add(0);
        }
        */
        List<IntList> lastLengths = new ArrayList<IntList>();
        for (int i = 0; i < size; ++i) {
            lastLengths.add(new IntArrayList());
            lastLengths.get(i).add(0);
        }

        IntSet maxIncreasedByOne = null;
        IntSet increasedByOne = new IntOpenHashSet();
        for (int i = 1; i <= maxLen; ++i) {
            /*
            for (int j = 0; j < size; ++j) {
                System.err.print("(" + j + ":");
                for (int x : lastLengths.get(j)) {
                    System.err.print(" " + x);
                }
                System.err.print(") ");
            }
            System.err.println();
            */
            maxIncreasedByOne = emptySet;
            ans.add((byte)0);
            byte mb = -1;
            int remNumber = 0;
            int[] toAdd = new int[4];
            for (byte s = 0; s < 4; ++s) {
                increasedByOne.clear();
                ans.set(i - 1, s);
                remNumber = 0;
                for (int j = 0; j < size; ++j) {
                    ByteList cbl = ar.get(j);
                    int cl = cbl.size();
                    if ((cl < i) || masked[j]) {
                        continue;
                    }
                    ++remNumber;
                    for (int k = 1; k <= cl; ++k) {
                        dists[j][k][i] = dists[j][k - 1][i - 1] + byteDist[ar.get(j).get(k - 1)][s];
                        if (dists[j][k][i] > dists[j][k - 1][i] + 1) {
                            dists[j][k][i] = dists[j][k - 1][i] + 1;
                        }
                        if (dists[j][k][i] > dists[j][k][i - 1] + 1) {
                            dists[j][k][i] = dists[j][k][i - 1] + 1;
                        }
                    }
                    int min = Integer.MAX_VALUE;
                    IntList minLengths = new IntArrayList();
                    for (int k = 0 < i - md + 1 ? i - md + 1 : 0; (k <= cl) && (k <= i + md - 1); ++k) {
                        int cd = dists[j][k][i];
                        if (min >= cd) {
                            if (min > cd) {
                                minLengths.clear();
                            }
                            min = cd;
                            minLengths.add(k);
                        }
                    }
                    for (int x : minLengths) {
                        boolean br = false;
                        for (int y : lastLengths.get(j)) {
                            if ((x - y == 1) && (dists[j][x][i] == dists[j][y][i - 1])) {
                                increasedByOne.add(j);
                                br = true;
                                break;
                            }
                        }
                        if (br) {
                            break;
                        }
                    }
                    /*
                    if (minLength - lastLengths.get(j) == 1) {
                        increasedByOne.add(j);
                    }
                    */
                }
                if (increasedByOne.size() > maxIncreasedByOne.size()) {
                    mb = s;
                    maxIncreasedByOne = new IntOpenHashSet(increasedByOne);
                }
                int threshold = Math.max(4, remNumber / 5);
                if (increasedByOne.size() >= threshold) {
                    toAdd[s] = 1;
                }
                /*
                if (verbose) {
                    System.err.print("s = " + s + "; inc = " + increasedByOne.size() + ": ");
                    for (int x : increasedByOne) {
                        System.err.print(x + " ");
                    }
                    System.err.println();
                    for (int j = 0; j < size; ++j) {
                        if (!masked[j]) {
                            System.err.print(j + ": ");
                            for (int x : lastLengths.get(j)) {
                                System.err.print("(" + x + ", " + dists[j][x][i - 1] + ") ");
                            }
                            System.err.println();
                        }
                    }
                }
                */
            }
            int q = 0;
            for (int x : toAdd) {
                q += x;
            }
            /*
            if ((q > 1) && verbose) {
                System.err.print(q + "; ");
                for (int w = 0; w < 4; ++w) {
                    if (toAdd[w] > 0) {
                        System.err.print(w + " ");
                    }
                }
                System.err.println("; ans = " + new DnaQ(ans.toByteArray()));
            }
            */
            //
            if ((remNumber == 2) || (maxIncreasedByOne.size() < remNumber * 2. / 3) || (mb == -1)) {
                ans.removeByte(ans.size() - 1);
                List<ByteList> t = new ArrayList<ByteList>();
                for (int j = 0; j < size; ++j) {
                    ByteList bl = ar.get(j);
                    int cl = bl.size();
                    if ((cl < i) || masked[j]) {
                        continue;
                    }
                    int ll = lastLengths.get(j).get(lastLengths.get(j).size() - 1);
                    if (ll == cl) {
                        continue;
                    }
                    ByteList cbl = new ByteArrayList(ans);
                    cbl.addAll(bl.subList(ll, cl));
                    t.add(cbl);
                }
                return t;
            }
            /*
            if (((maxIncreasedByOne.size() < remNumber * 2 / 3) /*&& (remNumber >= 6)) || (mb == -1)) {
                List<ByteList> t = new ArrayList<ByteList>();

                List<ByteList> l1 = new ArrayList<ByteList>();
                List<ByteList> l2 = new ArrayList<ByteList>();
                if (!maxIncreasedByOne.isEmpty()) {
                    for (int j = 0; j < size; ++j) {
                        if (maxIncreasedByOne.contains(j) || (!masked[j] && (ar.get(j).size() < i))) {
                            l1.add(ar.get(j));
                        } else {
                            l2.add(ar.get(j));
                        }
                    }
                    l1 = consensus(l1, md, verbose);
                    l2 = consensus(l2, md, verbose);
                }
                //t.addAll(consensus(rem, md, verbose));
                t.addAll(l1);
                t.addAll(l2);

                ans.remove(i - 1);
                t.add(ans);

                return t;
            }
            */
            ans.set(i - 1, mb);
            if (verbose) {
                System.err.print(mb == 0 ? 'A' : mb == 1 ? 'G' : mb == 2 ? 'C' : 'T');
            }

            int s = mb;
            for (int j = 0; j < size; ++j) {
                ByteList cbl = ar.get(j);
                int cl = cbl.size();
                if ((cl < i) || masked[j]) {
                    continue;
                }
                for (int k = 1; k <= cl; ++k) {
                    dists[j][k][i] = dists[j][k - 1][i - 1] + byteDist[ar.get(j).get(k - 1)][s];
                    if (dists[j][k][i] > dists[j][k - 1][i] + 1) {
                        dists[j][k][i] = dists[j][k - 1][i] + 1;
                    }
                    if (dists[j][k][i] > dists[j][k][i - 1] + 1) {
                        dists[j][k][i] = dists[j][k][i - 1] + 1;
                    }
                }
                int min = Integer.MAX_VALUE;
                for (int k = 0 < i - md + 1 ? i - md + 1 : 0; (k <= cl) && (k <= i + md - 1); ++k) {
                    if ((min >= dists[j][k][i]) && ((dists[j][k][i] < k / 3) || (k < 6))) {
                        if (min > dists[j][k][i]) {
                            lastLengths.get(j).clear();
                        }
                        lastLengths.get(j).add(k);
                        min = dists[j][k][i];
                    }
                }
                if (min == Integer.MAX_VALUE) {
                    masked[j] = true;
                }
            }
        }
        List<ByteList> result = new ArrayList<ByteList>();
        result.add(ans);
        return result;
    }

    static int[][] byteDist = new int[][]{
            {0, 1, 1, 1},
            {1, 0, 1, 1},
            {1, 1, 0, 1},
            {1, 1, 1, 0}
    };

    
    public int dist(long a, long b, int len) {
        return dist(a, b, len, len);
    }

    int[][] kmerDist = new int[50][50];

    public int dist(long a, long b, int len, int md) {
        byte[] ar = new byte[len];
        byte[] br = new byte[len];
        for (int i = 0; i < len; ++i) {
            ar[i] = (byte)((a >> (2 * (len - 1 - i))) & 3);
            br[i] = (byte)((b >> (2 * (len - 1 - i))) & 3);
        }

        return dist(ar, br, kmerDist, md);
    }

    public int dist(byte[] a, int posA, byte[] b, int posB) {
        //System.err.println(posA + " " + a.length);
        //System.err.println(posB + " " + b.length);
        int beg = posA > posB ? posA : posB;
        int end = (posA + a.length) < (posB + b.length) ? (posA + a.length) : (posB + b.length);
        byte[] na = Arrays.copyOfRange(a, beg - posA, end - posA);
        byte[] nb = Arrays.copyOfRange(b, beg - posB, end - posB);
        return dist(na, nb, null);
    }

    public int dist(byte[] a, byte[] b) {
        return dist(a, b, null);
    }

    public int dist(byte[] a, byte[] b, int[][] dist) {
        return dist(a, b, dist, a.length + b.length + 1);
    }

    public int dist(byte[] a, byte[] b, int[][] dist, int md) {
        int l1 = a.length;
        int l2 = b.length;

        if (dist == null) {
            dist = new int[l1+1][l2+1];
        }
        for (int i = 0; i <= l1; ++i) {
            dist[i][0] = i;
        }
        for (int i = 0; i <= l2; ++i) {
            dist[0][i] = i;
        }
        int l = l1 + l2;
        for (int ij = 2; ij <= l; ++ij) {
            int cmd = Integer.MAX_VALUE;
            for (int i = 1 < ij - l2 ? ij - l2 : 1; (i <= l1) && (i < ij); ++i) {
                int j = ij - i;
                if ((a[i - 1] < 0) || (a[i - 1] > 3) || (b[j - 1] < 0) || (b[j - 1] > 3)) {
                    System.err.println(a[i - 1] + " " + b[j - 1]);
                }
                dist[i][j] = dist[i - 1][j - 1] + byteDist[a[i - 1]][b[j - 1]];
                if (dist[i][j] > dist[i - 1][j] + 1) {
                    dist[i][j] = dist[i - 1][j] + 1;
                }
                if (dist[i][j] > dist[i][j - 1] + 1) {
                    dist[i][j] = dist[i][j - 1] + 1;
                }
                if (dist[i][j] < cmd) {
                    cmd = dist[i][j];
                }
            }
            if ((cmd > md) && (cmd < Integer.MAX_VALUE)) {
                return cmd;
            }
        }
        return dist[l1][l2];
    }

    private static void reverseComplement(byte[] ar) {
        int m = (ar.length + 1) / 2;
        for (int k = 0; k < m; ++k) {
            byte b1 = (byte)(3 - ar[k]);
            byte b2 = (byte)(3 - ar[ar.length - 1 - k]);
            ar[k] = b2;
            ar[ar.length - 1 - k] = b1;
        }
    }

    private static byte[] readDnaQ(MultiFile2MemoryMap mf, long pos) throws IOException {
        int dnaqLen = mf.readInt(pos);
        byte[] dnaqBytes = new byte[dnaqLen];
        mf.read(pos + 4, dnaqBytes);
        for (int j = 0; j < dnaqLen; ++j) {
            dnaqBytes[j] &= 3;
        }
        return dnaqBytes;
    }

    private static boolean match(Kmer a, Kmer b, int w) {
        int l = a.length();
        for (int i = 0; i < w; ++i) {
            if (a.nucAt(i) != b.nucAt(i)) {
                return false;
            }
            if (a.nucAt(l - 1 - i) != b.nucAt(l - 1 - i)) {
                return false;
            }
        }
        return true;
    }

}
