package ru.ifmo.genetics.tools.olc.overlaps;

import org.apache.commons.configuration.Configuration;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.DnaView;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.io.readers.DedicatedLineReader;
import ru.ifmo.genetics.executors.BlockingThreadPoolExecutor;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.NumUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReferenceArray;


public class Overlaps<T extends LightDna> {
    final AtomicReferenceArray<OverlapsList> overlaps;

    public final ArrayList<T> reads;
    public final int readsNumber;

    public final boolean withWeights;
    final int availableProcessors;
    private static int defaultCapacity = 2;


    protected Overlaps(ArrayList<T> reads, int availableProcessors, boolean createLists, boolean withWeights) {
        assert assertReadsAreWithRCCopy(reads);

        this.reads = reads;
        readsNumber = reads.size();
        
        overlaps = new AtomicReferenceArray<OverlapsList>(readsNumber);
        
        this.availableProcessors = availableProcessors;

        this.withWeights = withWeights;
        if (createLists) {
            for (int i = 0; i < readsNumber; ++i) {
                overlaps.set(i, new OverlapsList(withWeights));
            }
        } else {
            // :ToDo: may be option createLists should be removed?
            for (int i = 0; i < readsNumber; ++i) {
                overlaps.set(i, FAKE_LIST);
            }
        }
    }

    protected Overlaps(ArrayList<T> reads, Configuration config, boolean createLists, boolean withWeights) {
        this(reads, config.getInt("available_processors"), createLists, withWeights);
    }
    
    public Overlaps(Overlaps other, boolean createLists, boolean withWeights) {
        this(other.reads, other.availableProcessors, createLists, withWeights);
    }

    public Overlaps(ArrayList<T> reads, int availableProcessors, boolean withWeights) {
        this(reads, availableProcessors, true, withWeights);
    }

    public Overlaps(ArrayList<T> reads, boolean withWeights) {
        this(reads, Runtime.getRuntime().availableProcessors(), true, withWeights);
    }

    public Overlaps(ArrayList<T> reads, File[] overlapsFiles, int availableProcessors, boolean[] removedReads) throws IOException, InterruptedException {
        this(reads, availableProcessors, false, !overlapsFiles[0].getName().contains(".raw"));

        // marking removed reads
        if (removedReads != null) {
            for (int i = 0; i < readsNumber; i++) {
                if (removedReads[i]) {
                    markReadRemoved(i);
                }
            }
        }
        
        // loading
        long inputOverlapsNumber = 0;
        for (File f : overlapsFiles) {
            inputOverlapsNumber += FileUtils.linesNumber(f);
        }
        defaultCapacity = (inputOverlapsNumber == 0) ? 0 :
                (int)(inputOverlapsNumber / readsNumber / 2) + 1;

        for (File f : overlapsFiles) {
            DedicatedLineReader overlapsReader = new DedicatedLineReader(f, availableProcessors);
            overlapsReader.start();

            BlockingThreadPoolExecutor executor = new BlockingThreadPoolExecutor(availableProcessors);
            while (true) {
                ByteBuffer buffer = overlapsReader.getBuffer();
                if (buffer == null) {
                    break;
                }

                executor.blockingExecute(new AddingTask(this, buffer, !withWeights, overlapsReader));
            }

            executor.shutdownAndAwaitTermination();
        }
    }

    public Overlaps(ArrayList<T> reads, File[] overlapsFiles, int availableProcessors) throws IOException, InterruptedException {
        this(reads, overlapsFiles, availableProcessors, null);
    }

    public Overlaps(ArrayList<T> reads, File[] overlapsFiles) throws IOException, InterruptedException {
        this(reads, overlapsFiles, Runtime.getRuntime().availableProcessors());
    }



    public static <T extends LightDna> boolean assertReadsAreWithRCCopy(ArrayList<T> reads) {
        assert reads.size() % 2 == 0;
        for (int i = 0; i < reads.size(); i += 2) {
            assert DnaTools.equals(DnaView.rcView(reads.get(i)), reads.get(i + 1));
        }
        return true;
    }


    public OverlapsList getList(int i) {
        ensureListNotFake(i);
        return overlaps.get(i);
    }

    public boolean isReadRemoved(int i) {
        assert (overlaps.get(i) == null) == (overlaps.get(i ^ 1) == null);
        return overlaps.get(i) == null;
    }
    
    public void markReadRemoved(int i) {
        overlaps.set(i, null);
        overlaps.set(i ^ 1, null);
    }

    /**
     *
     * @param x
     * @return ~x if x is odd and x otherwise
     */
    private static int getKey(int x) {
        if ((x & 1) == 1) {
            return ~x;
        } else {
            return x;
        }
    }
    
    public static int compare(int read1, int centerShift1, int read2, int centerShift2) {
        int cmp = centerShift1 - centerShift2;
        if (cmp != 0) {
            return cmp;
        }

        return NumUtils.compare(getKey(read1), getKey(read2));
    }

    /**
     * Checks if overlap is oriented from from to to
     *
     * Orientations from from to to means that a center of the first read
     * goes earlier than a center of the second. If they are on the same
     * position then getKey(id)s are compared.
     */
    public static boolean isWellOriented(int from, int to, int centerShift) {
        return compare(from, 0, to, centerShift) <= 0;
    }

    /**
     * Add forward and reverse overlaps. Not thread-safe.
     * No need the overlap to be well-oriented.
     *
     * @return number of added overlaps, usually it's 0 or 2, but sometimes mb 1
     */
    public int addOverlap(int from, int to, int centerShift, int weight) {
        if (!isWellOriented(from, to, centerShift)) {
            return addOverlap(to, from, -centerShift, weight);
        }

        OverlapsList fromList = getList(from);

        if (getList(from).contains(to, centerShift)) {
            /*
            int i = getList(from).find(to, centerShift);
            getList(from).setWeight(i, getList(from).getWeight(i) + weight);
            int j = getList(to ^ 1).find(from ^ 1, centerShift);
            getList(to ^ 1).setWeight(j, getList(from).getWeight(i));
            */
            return 0;
        }

        getList(from).add(to, centerShift, weight);
        int res = 1;

        if (from != (to ^ 1)) {
            int cfrom = to ^ 1;
            int cto = from ^ 1;
            assert isWellOriented(cfrom, cto, centerShift);
            getList(cfrom).add(cto, centerShift, weight);
            res++;
        }

        return res;
    }

    public int addRawOverlap(int from, int to, int beginShift, int weight) {
        return addOverlap(from, to, beginShiftToCenterShift(from, to, beginShift), weight);
    }


    public boolean containsOverlap(int from, int to, int centerShift) {
        if (!isWellOriented(from, to, centerShift)) {
            return containsOverlap(to, from, -centerShift);
        }

        return getList(from).contains(to, centerShift);
    }

    private void ensureListNotFake(int i) {
        i = i & (i ^ 1);
        if (overlaps.get(i) == FAKE_LIST) {
            overlaps.compareAndSet(i, FAKE_LIST, new OverlapsList(defaultCapacity, withWeights));
        }
        
        if (overlaps.get(i ^ 1) == FAKE_LIST) {
            overlaps.compareAndSet(i ^ 1, FAKE_LIST, new OverlapsList(defaultCapacity, withWeights));
        }
    }

    /**
     * Add forward and reverse overlaps. Thread-safe.
     *
     * Overlap need not to be well-oriented.
     *
     * @param from
     * @param to
     * @param centerShift
     * @return number of added overlaps, usually it's 0 or 2, but sometimes mb 1
     */
    public int addOverlapWithSync(int from, int to, int centerShift, int weight) {
        // System.err.println("aOWS " + from + " " + to + " " + centerShift + " " + weight);
        int x = from & (from ^ 1);
        int y = to & (to ^ 1);

        if (isReadRemoved(x) || isReadRemoved(y)) {
            return 0;
        }

        ensureListNotFake(x);
        ensureListNotFake(y);

        int minXY = Math.min(x, y);
        int maxXY = Math.max(x, y);

        int res = -1;
        synchronized (getList(minXY)) {
            synchronized (getList(maxXY)) {
                res = addOverlap(from, to, centerShift, weight);
            }
        }
        return res;
    }


    public int addRawOverlapWithSyncUsingBeginShift(int from, int to, int beginShift) {
        // System.err.println("aOWSUBS " + from + " " + to + " " + beginShift);
        return addOverlapWithSync(from, to, beginShiftToCenterShift(from, to, beginShift), 0);
    }
    
    public int addRawOverlapWithSync(int from, int to, int centerShift) {
        // System.err.println("aOWS " + from + " " + to + " " + centerShift);
        return addOverlapWithSync(from, to, centerShift, 0);
    }

    /**
     * Remove forward and reverse overlaps. Not thread-safe. Complement to addOverlap()
     *
     * Overlap need not to be well-oriented.
     *
     * @param from
     * @param to
     * @param centerShift
     * @return number of removed overlaps, usually it's 0 or 2, but sometimes mb 1
     */
    public int removeOverlap(int from, int to, int centerShift) {
        assert !isReadRemoved(from) : "Read " + from + " removed, to = " + to;
        assert !isReadRemoved(to) : "Read " + to + " removed, from = " + from;
        if (!isWellOriented(from, to, centerShift)) {
            return removeOverlap(to, from, -centerShift);
        }

        if (!getList(from).remove(to, centerShift)) {
            return 0;
        }

        int res = 1;

        if (from != (to ^ 1)) {
            int cfrom = to ^ 1;
            int cto = from ^ 1;
            assert isWellOriented(cfrom, cto, centerShift);
            getList(cfrom).remove(cto, centerShift);
            res++;
        }

        return res;
    }


    public int getOutDegree(int from) {
        return getList(from).size();
    }

    public OverlapsList getForwardOverlaps(int from) {
        return getForwardOverlaps(from, new OverlapsList(withWeights));
    }

    /**
     * Adds forward overlaps from from to list.
     *
     * @param result list to add to
     */
    public OverlapsList getForwardOverlaps(int from, OverlapsList result) {
        result.clear();
        result.addAll(getList(from));
        return result;
    }

    public int getInDegree(int from) {
        return getList(from ^ 1).size();
    }

    public OverlapsList getBackwardOverlaps(int from) {
        return getBackwardOverlaps(from, new OverlapsList(withWeights));
    }

    /**
     * Adds backward overlaps from from to list.
     *
     * @param result list to add to
     */
    public OverlapsList getBackwardOverlaps(int from, OverlapsList result) {
        result.clear();
        OverlapsList rcOverlaps = getList(from ^ 1);
        assert rcOverlaps != null : "Getting backward overlaps from removed read: " + from;
        result.ensureCapacity(rcOverlaps.size());

        for (int i = 0; i < rcOverlaps.size(); ++i) {
            int to = rcOverlaps.getTo(i) ^ 1;
            int shift = -rcOverlaps.getCenterShift(i);
            if (withWeights) {
                result.add(to, shift, rcOverlaps.getWeight(i));
            } else {
                result.add(to, shift);
            }
        }
        return result;
    }

    /**
     * Adds all overlaps from from to list.
     *
     * @param result list to add to
     */
    public OverlapsList getAllOverlaps(int from, OverlapsList result) {
        result.clear();

        if (!isReadRemoved(from)) {
            getBackwardOverlaps(from, result);
            // adding forward overlaps without clearing
            result.addAll(getList(from));
        }

        return result;
    }

    public int removeOverlapsWithNull(int i) {
        OverlapsList list = getList(i);
        int size = list.size();
        int removed = 0;
        for (int j = 0; j < size; ++j) {
            if (getList(list.getTo(j)) == null) {
                removed++;
                continue;
            }
            list.copy(j, j - removed);
        }
        for (int j = 0; j < removed; ++j) {
            list.removeLast();
        }
        return removed;
    }


    public void sortAll() throws InterruptedException {
        BlockingThreadPoolExecutor sortExecutor = new BlockingThreadPoolExecutor(availableProcessors);
        int taskSize = readsNumber / availableProcessors + 1;
        for (int i = 0; i < readsNumber; i += taskSize) {
            sortExecutor.blockingExecute(new SortingTask(this, i, Math.min(i + taskSize, readsNumber)));
        }
        sortExecutor.shutdownAndAwaitTermination();
    }

    public void printToFile(String outputFile) throws FileNotFoundException {
        printToFile(new File(outputFile));
    }
    public void printToFile(File outputFile) throws FileNotFoundException {
        if (outputFile.getName().endsWith(".raw") != !withWeights) {
            System.err.println("WARNING: file extension (" + outputFile + ") and withWeights (" + withWeights + ") field don't correspond");
        }
        PrintWriter out = new PrintWriter(outputFile);
        for (int i = 0; i < readsNumber; ++i) {
            if (getList(i) == null) {
                if (withWeights) {
                    out.println(i + " -1 0 0");
                } else {
                    out.println(i + " -1 0");
                }
                continue;
            }
            for (int j = 0; j < getList(i).size(); ++j) {
                int to = getList(i).getTo(j);
                if (overlaps.get(to) == null) {
                    continue;
                }

                if (withWeights) {
                    out.println(i + " " + to + " " + getList(i).getCenterShift(j) + " " + getList(i).getWeight(j));
                } else {
                    out.println(i + " " + to + " " + getList(i).getCenterShift(j));
                }
            }
        }
        out.close();
    }


    public static int beginShiftToCenterShiftUsingLen(int length1, int length2, int beginShift) {
        return 2 * beginShift + length2 - length1;
    }
    public static int centerShiftToBeginShiftUsingLen(int length1, int length2, int centerShift) {
        int doubledBeginShift = centerShift + length1 - length2;
        assert doubledBeginShift % 2 == 0;
        return doubledBeginShift / 2;
    }


    public int beginShiftToCenterShift(int from, int to, int beginShift) {
        int length1 = reads.get(from).length();
        int length2 = reads.get(to).length();

        return 2 * beginShift + length2 - length1;
    }

    public int centerShiftToBeginShift(int from, int to, int centerShift) {
        int length1 = reads.get(from).length();
        int length2 = reads.get(to).length();
        int doubledBeginShift = centerShift + length1 - length2;
        assert doubledBeginShift % 2 == 0;
        return doubledBeginShift / 2;
    }
    
    public int calculateOverlapLen(int from, int to, int centerShift) {
        int beginShift = centerShiftToBeginShift(from, to, centerShift);
        int len1 = reads.get(from).length();
        return len1 - beginShift;
    }
    

    public int getWeight(int from, int to, int centerShift) {
        int i = getList(from).find(to, centerShift);
        return getList(from).getWeight(i);
    }

    public int getAverageWeight() {
        long weightsSum = 0;
        int n = 0;
        for (int i = 0; i < readsNumber; ++i) {
            OverlapsList list = getList(i);
            if (list == null) {
                continue;
            }
            for (int j = 0; j < list.size(); ++j) {
                weightsSum += getList(i).getWeight(j);
                ++n;
            }
        }
        if (n == 0) {
            return 0;
        }
        return (int)(weightsSum / n);
    }

    public long calculateSize() {
        long ans = 0;
        for (int i = 0; i < readsNumber; i++) {
            if (getList(i) != null) {
                ans += getList(i).size();
            }
        }
        return ans;
    }
    public int calculateRealReadsNumber() {
        int ans = 0;
        for (int i = 0; i < readsNumber; i++) {
            if (!isReadRemoved(i)) {
                ans++;
            }
        }
        return ans;
    }


    private final static OverlapsList FAKE_LIST = new OverlapsList(0, false) {
        public Edge get(int i) { throw new UnsupportedOperationException(); }
        public int getTo(int i) { throw new UnsupportedOperationException(); }
        public int getCenterShift(int i) { throw new UnsupportedOperationException(); }
        public int getWeight(int i) { throw new UnsupportedOperationException(); }
        public void setTo(int i, int to) { throw new UnsupportedOperationException(); }
        public void setCenterShift(int i, int centerShift) { throw new UnsupportedOperationException(); }
        public void setWeight(int i, int weight) { throw new UnsupportedOperationException(); }
        public boolean isEmpty() { throw new UnsupportedOperationException(); }
        public int size() { throw new UnsupportedOperationException(); }
        public boolean isWithWeights() { throw new UnsupportedOperationException(); }
        public boolean contains(Edge e) { throw new UnsupportedOperationException(); }
        public boolean contains(int to, int centerShift) { throw new UnsupportedOperationException(); }
        public int capacity() { throw new UnsupportedOperationException(); }
        public int find(int to, int centerShift) { throw new UnsupportedOperationException(); }
        public int find(Edge e) { throw new UnsupportedOperationException(); }
        public void add(Edge e) { throw new UnsupportedOperationException(); }
        public void add(int to, int centerShift) { throw new UnsupportedOperationException(); }
        public void add(int to, int centerShift, int weight) { throw new UnsupportedOperationException(); }
        public void addAll(OverlapsList list) { throw new UnsupportedOperationException(); }
        void addAllAddingErrorWeight(OverlapsList list) { throw new UnsupportedOperationException(); }
        public void ensureCapacity(int minCapacity) { throw new UnsupportedOperationException(); }
        public boolean remove(int to, int shift) { throw new UnsupportedOperationException(); }
        public void remove(int i) { throw new UnsupportedOperationException(); }
        public void removeLast() { throw new UnsupportedOperationException(); }
        public void clear() { throw new UnsupportedOperationException(); }
        public void sort(OverlapsSortTraits sortTraits) { throw new UnsupportedOperationException(); }
        public String toString() { return "FAKE_LIST"; }
    };

}
