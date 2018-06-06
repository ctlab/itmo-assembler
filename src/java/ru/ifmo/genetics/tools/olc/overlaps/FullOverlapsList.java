package ru.ifmo.genetics.tools.olc.overlaps;

import java.util.Arrays;

public class FullOverlapsList {
    private int size;

    private final static int DEFAULT_SIZE = 4;

    private int[] froms;
    private int[] tos;
    private int[] shifts;
    private int[] weights;

    public FullOverlapsList() {
        this(DEFAULT_SIZE);
    }

    public FullOverlapsList(int capacity) {
        froms = new int[capacity];
        tos = new int[capacity];
        shifts = new int[capacity];
        weights = new int[capacity];
    }

    public void setFrom(int i, int to) {
        froms[i] = to;
    }
    public void setTo(int i, int to) {
        tos[i] = to;
    }

    public void setShift(int i, int shift) {
        shifts[i] = shift;
    }

    public int getFrom(int i) {
        return froms[i];
    }

    public int getTo(int i) {
        return tos[i];
    }
    
    public int getWeight(int i) {
        return weights[i];
    }

    public int getShift(int i) {
        return shifts[i];
    }

    public boolean isEmpty() {
        return (size == 0);
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return tos.length;
    }

    public void add(int from, int to, int shift, int weight) {
        ensureCapacity(size + 1);
        froms[size] = from;
        tos[size] = to;
        shifts[size] = shift;
        weights[size] = weight;
        ++size;
    }

    private void ensureCapacity(int minCapacity) {
        if (tos.length >= minCapacity)
            return;


        int newCapacity = tos.length;
        while (newCapacity < minCapacity) {
            newCapacity *= 4;
        }

        froms = Arrays.copyOf(froms, newCapacity);
        tos = Arrays.copyOf(tos, newCapacity);
        shifts = Arrays.copyOf(shifts, newCapacity);
        weights = Arrays.copyOf(weights, newCapacity);
    }

    public void clear() {
        size = 0;
    }

    public String toString() {
        StringBuilder res = new StringBuilder("[");
        boolean firstly = true;
        for (int i = 0; i < size; ++i) {
            if (!firstly) {
                res.append(", ");
            }
            res.append("[from=" + getFrom(i) + ", to=" + getTo(i) + ", shift=" + getShift(i)+"]");
        }
        res.append("]");
        return res.toString();
    }
}
