package ru.ifmo.genetics.structures.set;

import it.unimi.dsi.fastutil.HashCommon;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.log4j.Logger;
import ru.ifmo.genetics.utils.NumUtils;
import ru.ifmo.genetics.utils.tool.Tool;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Memory-efficient set based on many hash tables with open addressing.<br></br>
 * It is resizable (not full support!). Set is synchronized.<br></br>
 * It can contain up to 2^60 (~10^18) elements.<br></br>
 * <br></br>
 */
public class BigLongHashSet implements LongHashSetInterface {
    private static final Logger logger = Logger.getLogger("BigLongHashSet");

    protected LongHashSet[] sets;
    protected int mask;


    public BigLongHashSet(long capacity) {
        this(capacity, LongHashSet.DEFAULT_MAX_LOAD_FACTOR);
    }
    public BigLongHashSet(long capacity, float maxLoadFactor) {
        this(
                NumUtils.getPowerOf2(capacity >> 20 +
                        ((capacity & ((1 << 20) - 1)) == 0 ? 0 : 1)
                ),
                20,    // 1 M elements per small set
                maxLoadFactor
        );
    }

    public BigLongHashSet(int logSmallSetNumber, int logSmallCapacity) {
        this(logSmallSetNumber, logSmallCapacity, LongHashSet.DEFAULT_MAX_LOAD_FACTOR);
    }
    public BigLongHashSet(int logSmallSetNumber, int logSmallCapacity, float maxLoadFactor) {
        if (logSmallSetNumber > 30) {
            throw new IllegalArgumentException("logSmallSetNumber > 30!");
        }

        int smallSetNumber = 1 << logSmallSetNumber;
        mask = smallSetNumber - 1;

        sets = new LongHashSet[smallSetNumber];
        for (int i = 0; i < smallSetNumber; i++) {
            sets[i] = new LongHashSet(logSmallCapacity, maxLoadFactor);
        }
        Tool.debug(logger, "Created " + NumUtils.groupDigits(smallSetNumber) + " small LongHashSets");
    }


    @Override
    public boolean add(long key) {
        int n = HashCommon.murmurHash3((int) key) & mask;
        return sets[n].add(key);
    }

    @Override
    public boolean contains(long key) {
        int n = HashCommon.murmurHash3((int) key) & mask;
        return sets[n].contains(key);
    }

    @Override
    public long size() {
        long size = 0;
        for (LongHashSet set : sets) {
            size += set.size();
        }
        return size;
    }

    @Override
    public long capacity() {
        long capacity = 0;
        for (LongHashSet set : sets) {
            capacity += set.capacity();
        }
        return capacity;
    }



    // --------------  Other methods from interface LongHashSetInterface  ---------------

    @Override
    public void reset() {
        for (LongHashSet set : sets) {
            set.reset();
        }
    }


    long[] off;

    @Override
    public void prepare() {
        off = new long[sets.length];
        off[0] = 0;
        for (int i = 1; i < sets.length; i++) {
            off[i] = off[i - 1] + sets[i - 1].maxPosition() + 1;
        }
    }

    @Override
    public long maxPosition() {
        return sets.length == 0 ? -1 :
                off[sets.length - 1] + sets[sets.length - 1].maxPosition();
    }

    /**
     * Working ONLY if small sets stay unchanged!
     * Call prepare() before using.
     */
    @Override
    public long getPosition(long key) {
        int n = HashCommon.murmurHash3((int) key) & mask;
        long pos = sets[n].getPosition(key);
        return pos == -1 ? -1 : (off[n] + pos);
    }

    @Override
    public long elementAt(long pos) {
        int n = Arrays.binarySearch(off, pos);
        if (n < 0) {
            n = (-n - 1) - 1;
        }
        return sets[n].elementAt(pos - off[n]);
    }
    @Override
    public boolean containsAt(long pos) {
        int n = Arrays.binarySearch(off, pos);
        if (n < 0) {
            n = (-n - 1) - 1;
        }
        return sets[n].containsAt(pos - off[n]);
    }



    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(sets.length);

        for (LongHashSet set : sets) {
            set.write(out);
        }
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        int len = in.readInt();
        if (Integer.bitCount(len) != 1) {
            throw new RuntimeException("Length is not a power of two!");
        }
        sets = new LongHashSet[len];
        mask = sets.length - 1;

        for (int i = 0; i < len; i++) {
            sets[i] = new LongHashSet(1);
            sets[i].readFields(in);
        }
    }


    @Override
    public Iterator<MutableLong> iterator() {
        return new MyIterator(sets);
    }

    public static class MyIterator implements Iterator<MutableLong> {
        private int index;
        private Iterator<MutableLong> it = null;
        private final Iterable<MutableLong>[] sets;

        public MyIterator(Iterable<MutableLong>[] sets) {
            this.sets = sets;
            index = 0;
            if (index < sets.length) {
                it = sets[index].iterator();
            }
        }

        @Override
        public boolean hasNext() {
            while (index < sets.length) {
                if (it.hasNext()) {
                    return true;
                }
                index++;
                if (index < sets.length) {
                    it = sets[index].iterator();
                }
            }
            return false;
        }

        @Override
        public MutableLong next() {
            if (hasNext()){
                return it.next();
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
