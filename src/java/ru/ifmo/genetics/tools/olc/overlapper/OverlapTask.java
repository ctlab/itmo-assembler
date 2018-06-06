package ru.ifmo.genetics.tools.olc.overlapper;

import ru.ifmo.genetics.tools.olc.overlaps.Overlaps;
import java.nio.ByteBuffer;

import static ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString.*;

public class OverlapTask implements Runnable {
    private OverlapTaskContext context;

    private int saLeft, saRight; // interval in suffix array
    private IntervalList readsIntervals;
    private int pos;

    private int lastForkPos;
//    private final int K = ALPHABET;

    private final CharGetter readsCharGetter;
    private final CharGetter saCharGetter;

    private ByteBuffer buffer;
    private long foundOverlaps = 0;


    public OverlapTask(OverlapTaskContext context, int saLeft, int saRight, IntervalList readsIntervals,
                       int pos, int lastForkPos) {
        this.context = context;
        this.saLeft = saLeft;
        this.saRight = saRight;
        this.readsIntervals = readsIntervals;
        this.pos = pos;
        this.lastForkPos = lastForkPos;

        readsCharGetter = new ReadsCharGetter(context);
        saCharGetter = new SACharGetter(context);
    }

    public void run() {
        if (context.writer != null) {
            try {
                buffer = context.writer.getBuffer();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }

        findOverlaps(saLeft, saRight, readsIntervals, pos);

        if (context.writer != null) {
            context.writer.returnBuffer(buffer);
        }
        context.foundOverlaps.addAndGet(foundOverlaps);
    }



    private void findOverlapsWithForking(final int saLeft, final int saRight, final IntervalList readsIntervals,
                                         final int pos) {
        if (pos <= lastForkPos) {
            context.executor.execute(new OverlapTask(context, saLeft, saRight, readsIntervals, pos, lastForkPos));
        } else {
            findOverlaps(saLeft, saRight, readsIntervals, pos);
        }
    }

    void findOverlaps(int saLeft, int saRight, IntervalList readsIntervals, int pos) {
        // splitting suffix array interval
        int[] saBi = splitByCharAtPos(saLeft, saRight, pos, saCharGetter);

        IntervalList[] newReadIntervals = new IntervalList[ALPHABET];
        for (int saChar : DNAindexes) {
            if (saBi[saChar] < saBi[saChar + 1]) {
                newReadIntervals[saChar] = new IntervalList();
            }
        }

        // splitting reads intervals
        for (int i = 0; i < readsIntervals.size; i++) {
            int[] readsBi = splitByCharAtPos(readsIntervals.l[i], readsIntervals.r[i], pos, readsCharGetter);

            addOverlaps(saBi, readsBi, pos);

            // without error
            int[] lastErrors = readsIntervals.lastErrors[i];
            for (int ch : DNAindexes) {
                if ((readsBi[ch] < readsBi[ch + 1]) && (newReadIntervals[ch] != null)) {
                    newReadIntervals[ch].add(readsBi[ch], readsBi[ch + 1], lastErrors);
                }
            }
            // with error
            boolean canBeAnError = false;
            if (lastErrors.length > 0) {
                canBeAnError = (pos - lastErrors[0]) >= context.errorsWindowSize;
            }
            if (canBeAnError) {
                lastErrors = makeNewLastErrors(lastErrors, pos);
                for (int readChar : DNAindexes) {
                    if (readsBi[readChar] < readsBi[readChar + 1]) {
                        for (int saChar : DNAindexes) {
                            if ((readChar != saChar) && (newReadIntervals[saChar] != null)) {
                                newReadIntervals[saChar].add(readsBi[readChar], readsBi[readChar + 1], lastErrors);
                            }
                        }
                    }
                }
            }
        }

        // recursive executing
        for (int saChar : DNAindexes) {
            if ((newReadIntervals[saChar] != null) && (newReadIntervals[saChar].size > 0)) {
                findOverlapsWithForking(saBi[saChar], saBi[saChar + 1], newReadIntervals[saChar], pos + 1);
            }
        }
    }

    private void addOverlaps(int[] saBi, int[] readsBi, int pos) {
        if (pos >= context.minOverlap) {
            // first case - trying to append $ to all reads
            for (int saIndex = saBi[$index]; saIndex < saBi[$index + 1]; saIndex++) {
                for (int readIndex = readsBi[0]; readIndex < readsBi[ALPHABET]; readIndex++) {
                    printOverlap(saIndex, readIndex, false);
                }
            }

            // second case - one read is covered by another
            for (int readIndex = readsBi[$index]; readIndex < readsBi[$index + 1]; readIndex++) {
                for (int saIndex = saBi[$index + 1]; saIndex < saBi[ALPHABET]; saIndex++) {
                    printOverlap(saIndex, readIndex, true);
                }
            }
        }
    }

    private void printOverlap(int saIndex, int readIndex, boolean secondCase) {
        // readBegin[from]           suffix
        //  v                           v
        // $.....................................$      - from
        //                      read -  ..........      - to
        //                              ^        ^
        //                              0       pos

        long suffix = context.sa.get(saIndex);
        int from = getReadFromSuffix(suffix);
        long shift = suffix - context.readBegin[from];

        int to = context.readsInBucket.get(readIndex);

        if ((shift == 0) && ((from == to) || (secondCase))) {
            return;
        }

        int len1 = (int) (context.readBegin[from + 1] - context.readBegin[from]) - 1;
        int len2 = (int) (context.readBegin[to + 1] - context.readBegin[to]) - 1;
        int centerShift = Overlaps.beginShiftToCenterShiftUsingLen(len1, len2, (int) shift);

        from = calculateRealNumber(from);
        to = calculateRealNumber(to);
        
        if (Overlaps.isWellOriented(from, to, centerShift)) {
            writeOverlap(from, to, centerShift);
//            System.err.println("Overlapper: found overlap from " + from + " to " + to +
//                    ", centerShift " + centerShift + ", beginShift " + shift);
        } else {
            writeOverlap(to, from, -centerShift);
//            System.err.println("Overlapper: found overlap from " + to + " to " + from +
//                    ", centerShift " + -centerShift + ", beginShift " + -shift);
        }
    }

    private int calculateRealNumber(int n) {
        assert (0 <= n && n < context.readsNumber);
        if (n < context.realReadsNumber) {
            return 2 * n;
        } else {
            n = context.readsNumber - 1 - n;
            assert (n < context.realReadsNumber);
            return 2 * n + 1;
        }
    }

    private void writeOverlap(int from, int to, int centerShift) {
//        System.err.println("Overlapper: found overlap from " + from + " to " + to +
//                ", centerShift " + centerShift);
        foundOverlaps++;
        if (buffer != null) {
            buffer.put((from + " " + to + " " + centerShift + "\n").getBytes());
            if (buffer.position() > buffer.limit() / 2) {
                context.writer.returnBuffer(buffer);
                try {
                    buffer = context.writer.getBuffer();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            context.overlaps.addRawOverlapWithSync(from, to, centerShift);
        }
    }


    static {
        assert (ALPHABET == 5); // used in function below
    }
    /**
     * Divides the interval [l, r) into ALPHABET groups (at one group all strings in <code>pos</code> position
     * have the same char), and returns the array with begin index of each group. <br></br>
     * bi[0] = l and bi[ALPHABET] = r;
     */
    static int[] splitByCharAtPos(int l, int r, int pos, CharGetter charGetter) {
        int[] bi = new int[ALPHABET + 1];
        bi[0] = l;
        bi[ALPHABET] = r;

        bi[3] = binarySearchLeft(l, r, pos, 3, charGetter);
        bi[2] = binarySearchLeft(l, bi[3], pos, 2, charGetter);
        bi[1] = binarySearchLeft(l, bi[2], pos, 1, charGetter);
        bi[4] = binarySearchLeft(bi[3], r, pos, 4, charGetter);

        return bi;
    }

    static int binarySearchLeft(int l, int r, int pos, int ch, CharGetter charGetter) {
        l--;
        while (l < r - 1) {
            int m = (int) ((l + (long) r) >> 1);
            if (charGetter.getChar(m, pos) < ch) {
                l = m;
            } else {
                r = m;
            }
        }
        return r;
    }

    /**
     * Returns such read r that <code>readBegin[r] <= suffix < readBegin[r + 1]</code>.
     */
    private int getReadFromSuffix(long suffix) {
        int l = 0;
        int r = context.readsNumber;
        while (l < r - 1) {
            int m = (int) ((l + (long) r) >> 1);
            if (context.readBegin[m] <= suffix) {
                l = m;
            } else {
                r = m;
            }
        }
        assert (context.readBegin[l] <= suffix && suffix  < context.readBegin[l + 1]);
        return l;
    }

    private int[] makeNewLastErrors(int[] lastErrors, int pos) {
        int[] newLastErrors = new int[lastErrors.length];
        System.arraycopy(lastErrors, 1, newLastErrors, 0, lastErrors.length - 1);
        newLastErrors[lastErrors.length - 1] = pos;
        return newLastErrors;
    }


    @Override
    public String toString() {
        return "OverlapTask{" +
                "\n\tcontext=" + context +
                ", \n\tsaLeft=" + saLeft +
                ", \n\tsaRight=" + saRight +
                ", \n\treadsIntervals=" + readsIntervals +
                ", \n\tpos=" + pos +
                ", \n\tlastForkPos=" + lastForkPos +
                ", \n\treadsCharGetter=" + readsCharGetter +
                ", \n\tsaCharGetter=" + saCharGetter +
                ", \n\tbuffer=" + buffer +
                '}';
    }
}
