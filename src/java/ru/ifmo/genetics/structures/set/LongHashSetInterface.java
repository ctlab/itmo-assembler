package ru.ifmo.genetics.structures.set;

import org.apache.commons.lang.mutable.MutableLong;
import org.apache.hadoop.io.Writable;


public interface LongHashSetInterface extends Writable, Iterable<MutableLong> {

    /**
     * @return true, if the key was added.
     */
    public boolean add(long key);
    public boolean contains(long key);

    public long size();
    public long capacity();

    public void reset();


    // Methods, that use information about the internal structure. May be unsupported.

    /**
     * Call this method to prepare to the future requests.
     * Assuming no other thread modifying set!
     */
    public void prepare();
    public long maxPosition();
    /**
     * @return pos or -1, if not found
     */
    public long getPosition(long key);
    /**
     * Returns FREE, if pos is free.
     */
    public long elementAt(long pos);
    public boolean containsAt(long pos);
}
