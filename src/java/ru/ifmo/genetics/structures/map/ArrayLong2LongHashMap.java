package ru.ifmo.genetics.structures.map;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;

public class ArrayLong2LongHashMap {

    public Long2LongOpenHashMap[] hm;
    long mask;

    public ArrayLong2LongHashMap(int logMapsNumber) {
        int mapsNumber = 1 << logMapsNumber;
        mask = mapsNumber - 1;

        hm = new Long2LongOpenHashMap[mapsNumber];
        for (int i = 0; i < mapsNumber; ++i) {
            hm[i] = new Long2LongOpenHashMap();
        }
    }

    public long put(long key, long value) {
        int ind = (int)(key & mask);
        synchronized (hm[ind]) {
            return hm[ind].put(key, value);
        }
    }

    public long get(long key) {
        int ind = (int)(key & mask);
        return hm[ind].get(key);
    }

    public boolean containsKey(long key) {
        int ind = (int)(key & mask);
        return hm[ind].containsKey(key);
    }

    public long size() {
        long size = 0;
        for (Long2LongMap m : hm) {
            size += m.size();
        }
        return size;
    }

    public LongSet[] keySets() {
        LongSet[] res = new LongSet[hm.length];
        for (int i = 0; i < hm.length; i++) {
            res[i] = hm[i].keySet();
        }
        return  res;
    }

}
