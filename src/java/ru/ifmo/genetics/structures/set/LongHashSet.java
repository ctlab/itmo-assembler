package ru.ifmo.genetics.structures.set;

import it.unimi.dsi.fastutil.HashCommon;
import org.apache.commons.lang.mutable.MutableLong;
import ru.ifmo.genetics.utils.NumUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Resizable memory-efficient set based on hash table with open addressing.<br></br>
 * Set is synchronized.<br></br>
 * It can contain not more than 2^30 (~10^9) elements.<br></br>
 * <br></br>
 *
 * Special key value is used for marking free cells,
 * but you still can work with this key because it is processed manually.<br></br>
 * <br></br>
 *
 * You can extend it for any need.
 */
public class LongHashSet implements LongHashSetInterface {
    public static final float DEFAULT_MAX_LOAD_FACTOR = 0.75f;

    /**
     * Key value to mark free cells.
     */
    public static final long FREE = 0;


    /**
     * Class guarantees that link to keys array and all other values (except containsFreeKey and size)
     * will be unchanged during usage!
     */
    protected class SetData {
        public final long[] keys;
        public volatile boolean containsFreeKey;

        public volatile int size;
        public final int capacity, capacityMask;
        public final int maxFill;

        protected SetData(int capacity, float maxLoadFactor) {
            if (Integer.bitCount(capacity) != 1) {  // i.e. not power of 2
                throw new RuntimeException("Bad capacity " + capacity + ".");
            }
            keys = new long[capacity];
            // keys array has already filled with FREE key (which is 0)
            containsFreeKey = false;
            size = 0;
            this.capacity = capacity;
            capacityMask = capacity - 1;
            maxFill = (int) Math.ceil(capacity * maxLoadFactor);
        }
    }

    protected volatile SetData data;
    protected final ReentrantLock writeLock = new ReentrantLock();
    protected float maxLoadFactor;



    // constructors
    public LongHashSet() {
        this(20, DEFAULT_MAX_LOAD_FACTOR);  // 1 M elements
    }
    public LongHashSet(int capacity) {
        this(
                NumUtils.getPowerOf2(capacity),
                DEFAULT_MAX_LOAD_FACTOR
        );
    }
    public LongHashSet(int logCapacity, float maxLoadFactor) {
        if (logCapacity > 30) {
            throw new IllegalArgumentException("log capacity > 30!");
        }

        this.maxLoadFactor = maxLoadFactor;
        int capacity = 1 << logCapacity;
        data = new SetData(capacity, maxLoadFactor);
    }



    // methods
    @Override
    public boolean add(long key) {
        SetData curData = data;

        if (key == FREE) {
            if (curData.containsFreeKey) {
                return false;
            }
            writeLock.lock();
            try {
                curData = data;
                if (curData.containsFreeKey) {
                    return false;
                } else {
                    curData.containsFreeKey = true;
                    curData.size++;
                    return true;
                }
            } finally {
                writeLock.unlock();
            }
        }

        while (true) {
            int pos = getPositionInt(curData, key);
            if (curData.keys[pos] == key) {
                return false;
            }
            // no such key, adding...
            writeLock.lock();
            try {
                if (curData == data && curData.keys[pos] == FREE) {  // i.e. nothing has changed
                    curData.keys[pos] = key;
                    curData.size++;
                    if (curData.size >= curData.maxFill) {
                        enlargeAndRehash();
                    }
                    return true;
                }
            } finally {
                writeLock.unlock();
            }
        }
    }


    @Override
    public boolean contains(long key) {
        return contains(data, key);
    }
    protected static boolean contains(SetData curData, long key) {
        if (key == FREE) {
            return curData.containsFreeKey;
        }
        int pos = getPositionInt(curData, key);
        return curData.keys[pos] == key;
    }


    /**
     * NOT WORKING for FREE key, check it manually!
     */
    protected static int getPositionInt(SetData curData, long key) {
        long h = HashCommon.murmurHash3(key);
        int pos = (int) (h & curData.capacityMask);
        long[] keys = curData.keys;

        while (keys[pos] != FREE && keys[pos] != key) {
            pos++;
            if (pos == keys.length) {
                pos = 0;
            }
        }
        return pos;
    }


    private void enlargeAndRehash() {
        SetData curData = data;
        if (curData.capacity > Integer.MAX_VALUE / 2) {
            throw new RuntimeException("Can't enlarge set (can't create single array of 2^31 elements)!");
        }
        int newCapacity = 2 * curData.capacity;
        SetData newData = new SetData(newCapacity, maxLoadFactor);

        // coping elements
        for (long key : curData.keys) {
            if (key != FREE) {
                int pos = getPositionInt(newData, key);
                newData.keys[pos] = key;
            }
        }
        newData.containsFreeKey = curData.containsFreeKey;
        newData.size = curData.size;

        data = newData;
    }

    @Override
    public long size() { return data.size; }

    @Override
    public long capacity() { return data.capacity; }



    // --------------  Other methods from interface LongHashSetInterface  ---------------

    /**
     * USE ONLY then no other thread is working with this set!!!
     */
    @Override
    public void reset() {
        writeLock.lock();
        SetData curData = data;
        try {
            Arrays.fill(curData.keys, FREE);
            curData.containsFreeKey = false;
            curData.size = 0;
        } finally {
            writeLock.unlock();
        }
    }


    @Override
    public void prepare() {}
    @Override
    public long maxPosition() { return data.capacity; }

    @Override
    public long getPosition(long key) {
        SetData curData = data;
        if (key == FREE) {
            return curData.capacity;
        }
        int pos = getPositionInt(curData, key);
        if (curData.keys[pos] == key) {
            return pos;
        }
        return -1;  // not true, if data link is updated
    }

    @Override
    public long elementAt(long pos) {
        SetData curData = data;
        if (pos == curData.capacity) {
            return FREE;    // ambiguous answer
        }
        return curData.keys[(int) pos];
    }
    @Override
    public boolean containsAt(long pos) {
        SetData curData = data;
        if (pos == curData.capacity) {
            return curData.containsFreeKey;
        }
        return curData.keys[(int) pos] != FREE;
    }



    @Override
    public void write(DataOutput out) throws IOException {
        SetData curData = data;

        out.writeInt(curData.capacity);
        out.writeInt(curData.size);
        out.writeFloat(maxLoadFactor);

        for (long key : curData.keys) {
            out.writeLong(key);
        }
        out.writeBoolean(curData.containsFreeKey);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        int capacity = in.readInt();
        int size = in.readInt();
        maxLoadFactor = in.readFloat();

        SetData newData = new SetData(capacity, maxLoadFactor);

        long[] keys = newData.keys;
        for (int i = 0; i < capacity; i++) {
            keys[i] = in.readLong();
        }
        newData.containsFreeKey = in.readBoolean();
        newData.size = size;

        data = newData;
    }


    @Override
    public Iterator<MutableLong> iterator() {
        return new MyIterator(data);
    }

    protected class MyIterator implements Iterator<MutableLong> {
        private final SetData curData;
        private final long[] keys;
        private int index = 0;
        private final MutableLong value = new MutableLong();

        MyIterator(SetData data) {
            curData = data;
            keys = curData.keys;
        }

        @Override
        public boolean hasNext() {
            while ((index < keys.length) && (keys[index] == FREE)) {
                index++;
            }
            if (index < keys.length) {
                return true;
            }
            if (index == keys.length && curData.containsFreeKey) {
                return true;
            }
            return false;
        }

        @Override
        public MutableLong next() {
            if (hasNext()){
                if (index < keys.length) {
                    value.setValue(keys[index]);
                }
                if (index == keys.length) {
                    value.setValue(FREE);
                }
                index++;
                return value;
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
