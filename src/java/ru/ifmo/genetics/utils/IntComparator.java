package ru.ifmo.genetics.utils;

import java.util.Comparator;

public abstract class IntComparator implements Comparator<Integer> {
    public abstract int compare(int a, int b);
    
    @Override
    public int compare(Integer o1, Integer o2) {
        return compare(o1.intValue(), o2.intValue());
    }
}
