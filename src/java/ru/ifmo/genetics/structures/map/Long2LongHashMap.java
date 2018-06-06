package ru.ifmo.genetics.structures.map;

import ru.ifmo.genetics.structures.set.LongHashSet;
import ru.ifmo.genetics.utils.NumUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Resizable memory-efficient map based on hash table with open addressing.<br></br>
 * Map is synchronized.<br></br>
 * It can contain not more than 2^30 (~10^9) elements.<br></br>
 * <br></br>
 *
 * Special key value is used for marking free cell,
 * but you still can work with this key because it is processed manually.<br></br>
 * <br></br>
 *
 * You can extend it for any need.
 */
public class Long2LongHashMap extends LongHashSet implements Long2LongHashMapInterface {

    /**
     * Class guarantees that links to keys and values arrays and all other values (except containsFreeKey,
     * valueForFreeKey and size) will be unchanged during usage!
     */
    protected class MapData extends SetData {
        protected final long[] values;
        protected volatile long valueForFreeKey;

        public MapData(int capacity, float maxLoadFactor) {
            super(capacity, maxLoadFactor);
            values = new long[capacity];
            valueForFreeKey = 0;
        }
    }

    protected volatile MapData data;



    // constructors
    public Long2LongHashMap() {
        this(20, DEFAULT_MAX_LOAD_FACTOR);  // 1 M elements
    }
    public Long2LongHashMap(int capacity) {
        this(
                NumUtils.getPowerOf2(capacity),
                DEFAULT_MAX_LOAD_FACTOR
        );
    }
    public Long2LongHashMap(int logCapacity, float maxLoadFactor) {
        if (logCapacity > 30) {
            throw new IllegalArgumentException("log capacity > 30!");
        }

        this.maxLoadFactor = maxLoadFactor;
        int capacity = 1 << logCapacity;
        data = new MapData(capacity, maxLoadFactor);
        super.data = data;
    }



    // methods
    @Override
    public boolean add(long key) {
        return put(key, 0) == -1;
    }

    @Override
    public long put(long key, long value) {
        if (key == FREE) {
            writeLock.lock();
            try {
                MapData curData = data;
                long prev = curData.valueForFreeKey;
                curData.valueForFreeKey = value;
                if (!curData.containsFreeKey) {
                    curData.containsFreeKey = true;
                    curData.size++;
                    return -1;
                }
                return prev;
            } finally {
                writeLock.unlock();
            }
        }

        while (true) {
            MapData curData = data;
            int pos = getPositionInt(curData, key);
            writeLock.lock();
            try {
                if (curData == data && (curData.keys[pos] == FREE || curData.keys[pos] == key)) {  // i.e. nothing has changed
                    long prev = curData.values[pos];
                    curData.values[pos] = value;
                    if (curData.keys[pos] == FREE) {
                        curData.keys[pos] = key;
                        curData.size++;
                        if (curData.size >= curData.maxFill) {
                            enlargeAndRehash();
                        }
                        return -1;
                    }
                    return prev;
                }
            } finally {
                writeLock.unlock();
            }
        }
    }

    @Override
    public long addAndBound(long key, long incValue) {
        if (key == FREE) {
            writeLock.lock();
            try {
                MapData curData = data;
                long prev = curData.valueForFreeKey;
                curData.valueForFreeKey = NumUtils.addAndBound(prev, incValue);
                if (!curData.containsFreeKey) {
                    curData.containsFreeKey = true;
                    curData.size++;
                }
                return prev;
            } finally {
                writeLock.unlock();
            }
        }

        while (true) {
            MapData curData = data;
            int pos = getPositionInt(curData, key);
            writeLock.lock();
            try {
                if (curData == data && (curData.keys[pos] == FREE || curData.keys[pos] == key)) {  // i.e. nothing has changed
                    long prev = curData.values[pos];
                    curData.values[pos] = NumUtils.addAndBound(prev, incValue);
                    if (curData.keys[pos] == FREE) {
                        curData.keys[pos] = key;
                        curData.size++;
                        if (curData.size >= curData.maxFill) {
                            enlargeAndRehash();
                        }
                    }
                    return prev;
                }
            } finally {
                writeLock.unlock();
            }
        }
    }

    @Override
    public long get(long key) {
        MapData curData = data;
        if (key == FREE) {
            if (!curData.containsFreeKey) {
                return -1;
            }
            return curData.valueForFreeKey;
        }
        int pos = getPositionInt(curData, key);
        if (curData.keys[pos] == key) {
            return curData.values[pos];
        } else {
            // assuming keys[pos] == FREE
            return -1;
        }
    }

    @Override
    public long getWithZero(long key) {
        long value = get(key);
        if (value == -1)
            return 0;
        return value;
    }

    @Override
    public boolean contains(long key) {
        return contains(data, key);
    }


    private void enlargeAndRehash() {
        MapData curData = data;
        if (curData.capacity > Integer.MAX_VALUE / 2) {
            throw new RuntimeException("Can't enlarge map (can't create single array of 2^31 elements)!");
        }
        int newCapacity = 2 * curData.capacity;
        MapData newData = new MapData(newCapacity, maxLoadFactor);

        // coping elements
        for (int oldPos = 0; oldPos < curData.keys.length; oldPos++) {
            long key = curData.keys[oldPos];
            if (key != FREE) {
                int pos = getPositionInt(newData, key);
                newData.keys[pos] = key;
                newData.values[pos] = curData.values[oldPos];
            }
        }
        newData.containsFreeKey = curData.containsFreeKey;
        newData.valueForFreeKey = curData.valueForFreeKey;
        newData.size = curData.size;

        data = newData;
        super.data = newData;
    }

    @Override
    public long size() { return data.size; }

    @Override
    public long capacity() { return data.capacity; }



    // --------------  Other methods from interface Long2LongHashMapInterface  ---------------

    /**
     * USE ONLY then no other threads are working with this set!!!
     */
    @Override
    public void reset() {
        writeLock.lock();
        MapData curData = data;
        try {
            Arrays.fill(curData.keys, FREE);
            Arrays.fill(curData.values, 0);
            curData.containsFreeKey = false;
            curData.valueForFreeKey = 0;
            curData.size = 0;
        } finally {
            writeLock.unlock();
        }
    }
    /**
     * USE ONLY then no other threads are working with this set!!!
     */
    @Override
    public void resetValues() {
        writeLock.lock();
        MapData curData = data;
        try {
            Arrays.fill(curData.values, 0);
            curData.valueForFreeKey = 0;
        } finally {
            writeLock.unlock();
        }
    }


    @Override
    public long keyAt(long pos) {
        return elementAt(pos);
    }

    @Override
    public long valueAt(long pos) {
        MapData curData = data;
        if (pos == curData.capacity) {
            if (!curData.containsFreeKey) {
                return -1;
            }
            return curData.valueForFreeKey;
        }

        if (curData.keys[(int) pos] == FREE) {
            return -1;
        }
        return curData.values[(int) pos];
    }


    @Override
    public void write(DataOutput out) throws IOException {
        MapData curData = data;

        out.writeInt(curData.capacity);
        out.writeInt(curData.size);
        out.writeFloat(maxLoadFactor);

        for (int i = 0; i < curData.capacity; i++) {
            out.writeLong(curData.keys[i]);
            out.writeLong(curData.values[i]);
        }
        out.writeBoolean(curData.containsFreeKey);
        out.writeLong(curData.valueForFreeKey);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        int capacity = in.readInt();
        int size = in.readInt();
        maxLoadFactor = in.readFloat();

        MapData newData = new MapData(capacity, maxLoadFactor);

        for (int i = 0; i < capacity; i++) {
            newData.keys[i] = in.readLong();
            newData.values[i] = in.readLong();
        }
        newData.containsFreeKey = in.readBoolean();
        newData.valueForFreeKey = in.readLong();
        newData.size = size;

        data = newData;
        super.data = newData;
    }

    @Override
    public Iterator<MutableLongLongEntry> entryIterator() {
        return new MyIterator(data);
    }

    protected class MyIterator implements Iterator<MutableLongLongEntry> {
        private final MapData curData;
        private int index = 0;
        private final MutableLongLongEntry entry = new MutableLongLongEntry();

        MyIterator(MapData curData) {
            this.curData = curData;
        }

        @Override
        public boolean hasNext() {
            while ((index < curData.capacity) && (curData.keys[index] == FREE)) {
                index++;
            }
            if (index < curData.capacity) {
                return true;
            }
            if (index == curData.capacity && curData.containsFreeKey) {
                return true;
            }
            return false;
        }

        @Override
        public MutableLongLongEntry next() {
            if (hasNext()){
                if (index < curData.capacity) {
                    entry.setKey(curData.keys[index]);
                    entry.setValue(curData.values[index]);
                }
                if (index == curData.capacity) {
                    entry.setKey(FREE);
                    entry.setValue(curData.valueForFreeKey);
                }
                index++;
                return entry;
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
