package ru.ifmo.genetics.structures.map;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class ArrayLong2IntHashMap {

    public Long2IntOpenHashMap[] hm;
    long mask;

    public ArrayLong2IntHashMap(int logMapsNumber) {
        int mapsNumber = 1 << logMapsNumber;
        mask = mapsNumber - 1;

        hm = new Long2IntOpenHashMap[mapsNumber];
        for (int i = 0; i < mapsNumber; ++i) {
            hm[i] = new Long2IntOpenHashMap();
        }
    }

    public int get(long key) {
        int ind = (int)(key & mask);
        return hm[ind].get(key);
    }

    public int add(long key, int incr) {
        int ind = (int)(key & mask);
        synchronized (hm[ind]) {
            return hm[ind].add(key, incr);
        }
    }

    public long size() {
        long size = 0;
        for (Long2IntMap m : hm) {
            size += m.size();
        }
        return size;
    }

}
