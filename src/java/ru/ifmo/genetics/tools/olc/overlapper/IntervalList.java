package ru.ifmo.genetics.tools.olc.overlapper;

import java.util.Arrays;

public class IntervalList {
    private final static int DEFAULT_CAPACITY = 2;
    private final static int RESIZE_FACTOR_NUM = 3;
    private final static int RESIZE_FACTOR_DENUM = 2;

    
    int[] l, r;
    int[][] lastErrors;

    int size;
    

    public IntervalList() {
        this(DEFAULT_CAPACITY);
    }

    public IntervalList(int capacity) {
        l = new int[capacity];
        r = new int[capacity];
        lastErrors = new int[capacity][];
        size = 0;
    }

    
    public void add(int l, int r, int[] lastErrors) {
        ensureCapacity(size + 1);
        this.l[size] = l;
        this.r[size] = r;
        this.lastErrors[size] = lastErrors;
        ++size;
    }

    public void ensureCapacity(int minCapacity) {
        if (l.length >= minCapacity)
            return;


        int newCapacity = l.length;
        while (newCapacity < minCapacity) {
            newCapacity = (newCapacity * RESIZE_FACTOR_NUM) / RESIZE_FACTOR_DENUM + 1;
        }

        l = Arrays.copyOf(l, newCapacity);
        r = Arrays.copyOf(r, newCapacity);
        lastErrors = Arrays.copyOf(lastErrors, newCapacity);
    }

    public void clear() {
        size = 0;
    }

}
