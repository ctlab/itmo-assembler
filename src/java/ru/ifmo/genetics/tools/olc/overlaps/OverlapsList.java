package ru.ifmo.genetics.tools.olc.overlaps;

import ru.ifmo.genetics.utils.NumUtils;
import ru.ifmo.genetics.utils.Sorter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class OverlapsList implements Iterable<OverlapsList.Edge> {
    private final static int DEFAULT_CAPACITY = 2;
    private final static int RESIZE_FACTOR_NUM = 3;
    private final static int RESIZE_FACTOR_DENUM = 2;

    public static final int ERROR_WEIGHT = Integer.MIN_VALUE;


    private int[] tos;
    private int[] centerShifts;
    private int[] weights;

    private int size;
    private boolean sorted = true;


    public OverlapsList(boolean withWeights) {
        this(DEFAULT_CAPACITY, withWeights);
    }

    public OverlapsList(int capacity, boolean withWeights) {
        tos = new int[capacity];
        centerShifts = new int[capacity];
        if (withWeights) {
            weights = new int[capacity];
        }
    }

    public OverlapsList(OverlapsList other) {
        this(other.size(), other.isWithWeights());
        addAll(other);
    }


    public Edge get(int i) {
        return new Edge(tos[i], centerShifts[i]);
    }

    public int getTo(int i) {
        return tos[i];
    }

    public int getCenterShift(int i) {
        return centerShifts[i];
    }
    
    public int getWeight(int i) {
        if (isWithWeights()) {
            return weights[i];
        }
        return ERROR_WEIGHT;
    }


    public void setTo(int i, int to) {
        tos[i] = to;
    }

    public void setCenterShift(int i, int centerShift) {
        centerShifts[i] = centerShift;
    }

    public void setWeight(int i, int weight) {
        weights[i] = weight;
    }


    public boolean isEmpty() {
        return (size == 0);
    }

    public int size() {
        return size;
    }

    public boolean isWithWeights() {
        return weights != null;
    }

    public boolean contains(Edge e) {
        return contains(e.to, e.centerShift);
    }

    public boolean contains(int to, int centerShift) {
        return find(to, centerShift) >= 0;
    }

    public int capacity() {
        return tos.length;
    }

    public int find(int to, int centerShift) {
        if (size == 0)
            return -1;

        if (sorted) {
            int lo = 0;
            int hi = size;
            while (hi > lo + 1) {
                int mid = (lo + hi) >> 1;

                int c = OverlapsSortTraits.compare(this, mid, to, centerShift);
                
                if (c < 0) {
                    lo = mid;
                } else if (c > 0) {
                    hi = mid;
                } else {
                    return mid;
                }

            }

            int i = lo;
            if (tos[i] == to && centerShifts[i] == centerShift) {
                return i;
            }
            return -1;

        }

        for (int i = 0; i < size; ++i) {
            if (tos[i] == to && centerShifts[i] == centerShift) {
                return i;
            }
        }
        return -1;
    }

    public int find(Edge e) {
        return find(e.to, e.centerShift);
    }

    public void add(Edge e) {
        add(e.to, e.centerShift);
    }

    public void add(int to, int centerShift) {
        assert !isWithWeights();
        add(to, centerShift, ERROR_WEIGHT);
    }
    
    public void add(int to, int centerShift, int weight) {
        sorted = false;
        ensureCapacity(size + 1);
        tos[size] = to;
        centerShifts[size] = centerShift;
        if (isWithWeights()) {
            weights[size] = weight;
        }
        ++size;
    }

    public void addAll(OverlapsList list) {
        if (list == null) {
            return;
        }

        ensureCapacity(size + list.size());
        boolean willSBeSorted = isEmpty() && list.sorted;
        assert list.isWithWeights() == isWithWeights();
        if (list.isWithWeights()) {
            for (int i = 0; i < list.size(); ++i) {
                add(list.getTo(i), list.getCenterShift(i), list.getWeight(i));
            }
        } else {
            for (int i = 0; i < list.size(); ++i) {
                add(list.getTo(i), list.getCenterShift(i));
            }
        }
        sorted = willSBeSorted;
    }

    void addAllAddingErrorWeight(OverlapsList list) {
        assert !list.isWithWeights();
        assert isWithWeights();
        for (int i = 0; i < list.size(); ++i) {
            add(list.getTo(i), list.getCenterShift(i), ERROR_WEIGHT);
        }
    }

    public void ensureCapacity(int minCapacity) {
        if (tos.length >= minCapacity)
            return;

        int newCapacity = tos.length;
        while (newCapacity < minCapacity) {
            newCapacity = (newCapacity * RESIZE_FACTOR_NUM) / RESIZE_FACTOR_DENUM + 1;
        }

        tos = Arrays.copyOf(tos, newCapacity);
        centerShifts = Arrays.copyOf(centerShifts, newCapacity);
        if (weights != null) {
            weights = Arrays.copyOf(weights, newCapacity);
        }
    }

    public boolean remove(int to, int shift) {
        int i = find(to, shift);
        if (i < 0) {
            return false;
        }
        remove(i);
        return true;
    }

    public void remove(int i) {
        sorted = false;

        tos[i] = tos[size - 1];
        centerShifts[i] = centerShifts[size - 1];
        if (isWithWeights()) {
            weights[i] = weights[size - 1];
        }
        removeLast();
    }

    public void removeLast() {
        --size;
    }

    public void clear() {
        size = 0;
    }

    public void copy(int from, int to) {
        tos[to] = tos[from];
        centerShifts[to] = centerShifts[from];
        if (isWithWeights()) {
            weights[to] = weights[from];
        }
    }

    public void sort(OverlapsSortTraits sortTraits) {
        if (sorted) {
            return;
        }
        sortTraits.list = this;
        Sorter.sort(0, size, sortTraits);
        sorted = true;
    }

    public static class OverlapsSortTraits implements Sorter.SortTraits {
        public OverlapsList list;

        public OverlapsSortTraits() {
            this(null);
        }

        public OverlapsSortTraits(OverlapsList list) {
            this.list = list;
        }

        public int compare(int i, int j) {
            return compare(list, i, j);
//            if (list.centerShifts[i] != list.centerShifts[j])
//                return list.centerShifts[i] - list.centerShifts[j];
//            return list.tos[i] - list.tos[j];
        }
        
        public static int compare(OverlapsList list, int i, int j) {
            return compare(list, i, list.tos[j], list.centerShifts[j]);
        }

        public static int compare(OverlapsList list, int i, int to, int centerShift) {
            return Overlaps.compare(list.tos[i], list.centerShifts[i], to, centerShift);
        }

        public void swap(int i, int j) {
            NumUtils.swap(list.tos, i, j);
            NumUtils.swap(list.centerShifts, i, j);
            if (list.isWithWeights()) {
                NumUtils.swap(list.weights, i, j);
            }
        }
    }

    public String toString() {
        StringBuilder res = new StringBuilder("[");
        boolean firstly = true;
        for (int i = 0; i < size; ++i) {
            if (!firstly) {
                res.append(", ");
            }
            res.append(get(i));
            firstly = false;
        }
        res.append("]");
        return res.toString();
    }

    public static class Edge implements Comparable<Edge> {
        public int to;
        public int centerShift;

        public Edge(int to, int centerShift) {
            super();
            this.to = to;
            this.centerShift = centerShift;
        }

        public Edge(Edge edge) {
            to = edge.to;
            centerShift = edge.centerShift;
        }

        @Override
        public int hashCode() {
            return to * 239 + centerShift * 1000000007;
        }

        @Override
        public boolean equals(Object o) {
            Edge e = (Edge)o;
            return (to == e.to) && (centerShift == e.centerShift);
        }

        @Override
        public int compareTo(Edge e) {
            if (centerShift != e.centerShift)
                return centerShift - e.centerShift;
            return to - e.to;
        }

        @Override
        public String toString() {
            return "Edge [to=" + to + " (" + (to / 2) + (to % 2 == 0 ? "" : "rc" ) + ") , centerShift=" + centerShift + "]";
        }
    }

    @Override
    public Iterator<Edge> iterator() {
        return new MyIterator();
    }

    class MyIterator implements Iterator<Edge> {
        int i = 0;
        
        @Override
        public boolean hasNext() {
            return (i < size);
        }

        @Override
        public Edge next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Edge e = get(i);
            i++;
            return e;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OverlapsList that = (OverlapsList) o;

        if (!NumUtils.equals(tos, size, that.tos, size)) {
            return false;
        }

        return NumUtils.equals(centerShifts, size, that.centerShifts, size);
    }

    @Override
    public int hashCode() {
        int result = size;
        result = 31 * result + NumUtils.hashCode(tos, size);
        result = 31 * result + NumUtils.hashCode(centerShifts, size);
        return result;
    }
}
