package ru.ifmo.genetics.utils;

import java.util.ArrayList;
import java.util.Iterator;

public class IteratorUtils {
    public static <T>ArrayList<T> head(int n, Iterator<T> it) {
        ArrayList<T> res = new ArrayList<T>(n);
        for (int i = 0; it.hasNext() && i < n; ++i) {
            res.add(it.next());
        }
        return res;
    }
}
