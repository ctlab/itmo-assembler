package ru.ifmo.genetics.tools.ec.simple;

import java.util.concurrent.CountDownLatch;

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import ru.ifmo.genetics.structures.map.ArrayLong2LongHashMap;

public class CleanWorker implements Runnable {

    public final int MAXIMAL_SUBS_NUMBER;
    public final int MAXIMAL_INDELS_NUMBER;

    private CleanDispatcher d;
    private LongSet goodKMers;
    private ArrayLong2LongHashMap fixes = new ArrayLong2LongHashMap(8);
    private final int len;
    private boolean verbose;

    public long del = 0;

    CountDownLatch latch;
    boolean interrupted = false;

    LongSet cfixes = new LongOpenHashSet();

    public CleanWorker(CleanDispatcher d, LongSet goodKMers, int len, CountDownLatch latch,
                       int MAXIMAL_SUBS_NUMBER, int MAXIMAL_INDELS_NUMBER) {
        this(d, goodKMers, len, latch, MAXIMAL_SUBS_NUMBER, MAXIMAL_INDELS_NUMBER, false);
    }

    public CleanWorker(CleanDispatcher d, LongSet goodKMers, int len, CountDownLatch latch,
                       int MAXIMAL_SUBS_NUMBER, int MAXIMAL_INDELS_NUMBER, boolean verbose) {
        this.MAXIMAL_SUBS_NUMBER = MAXIMAL_SUBS_NUMBER;
        this.MAXIMAL_INDELS_NUMBER = MAXIMAL_INDELS_NUMBER;
        this.d = d;
        this.goodKMers = goodKMers;
        this.len = len;
        this.latch = latch;
        this.verbose = verbose;
    }

    public ArrayLong2LongHashMap getResults() {
        return fixes;
    }

    public void interrupt() {
        interrupted = true;
    }

    long cur;

    private Long findFixes(long str) {
        int MAXIMAL_ERRORS_NUMBER = MAXIMAL_INDELS_NUMBER + MAXIMAL_SUBS_NUMBER;
        for (int s = 0; s <= MAXIMAL_ERRORS_NUMBER; s++) {
            for (int m = 0; (m <= s) && (m <= MAXIMAL_INDELS_NUMBER); ++m) {
                int n = s - m;
                cfixes.clear();
                cur = str;
                findNMFixes(str, n, m, 0, len, 0, 0);
                if (cfixes.size() == 1) {
                    return cfixes.iterator().next();
                }
            }
        }
        return null;
    }

    private int calcShift(long cfix, int ind, int pos) {
        int shift = 0;
        for (int i = 0; i < ind; ++i) {
            long tfix = (cfix >> (8 * i)) & 255;
            if (((tfix & 31) == tfix) && (tfix - 1 != pos)) {
                ++shift;
            }
            if ((tfix & 128) != 0) {
                --shift;
            }
        }
        return shift;
    }

    private long addDup(long cfix, int ind, int pos) {
        pos += 1 - calcShift(cfix, ind, pos);
        cfix |= ((long)pos) << (8 * ind);
        return cfix;
    }

    private long addDel(long cfix, int ind, int pos) {
        pos += 1 - calcShift(cfix, ind, pos);
        cfix |= (128L | pos) << (8 * ind);

        return cfix;
    }

    private long addSub(long cfix, int ind, int pos, long xor) {
        pos += 1 - calcShift(cfix, ind, pos);
        cfix |= (xor << (8 * ind + 5)) | (((long)pos) << (8 * ind));
        return cfix;
    }

    private void findNMFixes(long str, int n, int m, int begin, int end, long cfix, int nfix) {
        if (n < 0 || m < 0) {
            return;
        }
        if (n + m == 0) {
            // let's just check the leftmost k-mer
            if (len(cfix) >= len) {
                long cstr = (str >> (2 * (len(cfix) - len))) & ((1L << (2 * len)) - 1);
                if (!goodKMers.contains(cstr)) {
                    return;
                }
            } else {
                boolean ok = false;
                long t = 1 << (2 * (len - len(cfix)));
                long cstr = str << (2 * (len - len(cfix)));
                for (long i = 0; !ok && (i < t); ++i) {
                    ok |= goodKMers.contains(cstr + i);
                }
                if (!ok) {
                    return;
                }
            }

            cfixes.add(cfix);
            return;
        }

        int maxPos = len(cfix) - 1;
        for (int pos = begin; (pos < end) && (pos <= maxPos) && (cfixes.size() < 2); pos++) {
            long prev = (pos != 0) ? ((str >> (2 * pos - 2)) & 3) : 4;
            long cur = (str >> (2 * pos)) & 3;
            long next = (pos < maxPos) ? ((str >> (2 * pos + 2)) & 3) : 4;
            for (int xor = -1; xor <= 3; xor++) {
                long nstr;
                if ((xor == 0) && (m > 0) && (cur != next) && (cur == prev)) {
                    nstr = dup(str, pos, cfix);
                    findNMFixes(nstr, n, m - 1, pos + 1, pos + 2, addDup(cfix, nfix, pos), nfix + 1);
                } else if ((xor == -1) && (m > 0) && (cur == next) && (cur != prev)) {
                    nstr = del(str, pos, cfix);
                    findNMFixes(nstr, n, m - 1, pos, pos + 1, addDel(cfix, nfix, pos), nfix + 1);
                } else {
                    if ((xor >= 1) && (xor <= 3) && (n > 0) && ((pos != 0) && (pos != maxPos))) {
                        nstr = str ^ (((long)xor) << (2 * pos));
                        findNMFixes(nstr, n - 1, m, pos + 1, end, addSub(cfix, nfix, pos, xor), nfix + 1);
                    }
                }
            }
        }
    }

    private int len(long cfix) {
        int len = this.len;
        for (int i = 0; i < 8; ++i) {
            long tfix = (cfix >> (8 * i)) & 255;
            long type = tfix >> 5;
            if (tfix == 0) {
                break;
            }
            if (type == 0) {
                ++len;
            } else {
                if (type == 4) {
                    --len;
                }
            }
        }
        return len;
    }

    private long dup(long str, int pos, long cfix) {
        long left = (str >> (2 * pos + 2)) << (2 * pos + 2);
        long mid = ((str >> (2 * pos)) & 3) << (2 * pos);
        long rightAndMid = str & ((1L << (2 * pos + 2)) - 1);

        str = (left << 2) + (mid << 2) + rightAndMid;
        return str;
    }

    private long del(long str, int pos, long cfix) {
        long left = (str >> (2 * pos + 2)) << (2 * pos + 2);
        long mid = ((str >> (2 * pos)) & 3) << (2 * pos);
        long rightAndMid = str & ((1L << (2 * pos + 2)) - 1);
        long right = rightAndMid - mid;

        str = (left >> 2) + right;
        return str;
    }

    @Override
    public void run() {
        long s = 0;
        while (!interrupted) {
            LongList p = d.getWorkRange();
            if (p == null)
                break;
            s += p.size();
            for (long str : p) {
                Long fix = findFixes(str);
                if (fix == null) {
                    continue;
                }
                fixes.put(str, (long)fix);
            }
        }
        latch.countDown();
    }

}
