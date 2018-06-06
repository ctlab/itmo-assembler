package ru.ifmo.genetics.distributed.util;

import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.distributed.io.writable.Copyable;
import ru.ifmo.genetics.distributed.util.PublicCloneable;

public class ArrayListWritable<T extends Writable & PublicCloneable<T> & Copyable<T>> {
    private Object[] array;
    private int size;
    private static final int RESIZE_FACTOR = 2;

    public ArrayListWritable(int capacity) {
        array = new Object[capacity];
        size = 0;
    }
    
    public void add(T value) {
        ensureSize(size + 1);

        if (array[size] == null) {
            array[size] = value.publicClone();
        } else {
            ((T)array[size]).copyFieldsFrom(value);
        }
        size++;

    }
    
    public T get(int i) {
        assert i < size;
        return (T)array[i];
    }
    
    public int size() {
        return size;
    }

    private void ensureSize(int size) {
        if (size < array.length) {
            return;
        }
        
        int newSize = array.length;
        while (newSize < size) {
            newSize *= RESIZE_FACTOR;
        }
        
        Object[] new_array = new Object[newSize];
        System.arraycopy(array, 0, new_array, 0, array.length);
        array = new_array;
    }


    public void clear() {
        size = 0;
    }
}
