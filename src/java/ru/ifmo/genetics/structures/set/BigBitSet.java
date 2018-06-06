package ru.ifmo.genetics.structures.set;

import org.apache.log4j.Logger;
import ru.ifmo.genetics.utils.NumUtils;
import ru.ifmo.genetics.utils.tool.Tool;

import java.util.BitSet;

public class BigBitSet {
    private static final Logger logger = Logger.getLogger("BigBitSet");

    public static final int smallBitSetLogSize = 20; // 1 M values per small bit set (128 Kb)
    public static final int smallBitSetSize = 1 << smallBitSetLogSize;
    private static final long smallBitSetValueMask = smallBitSetSize - 1;


    protected final BitSet[] bitSets;

    public BigBitSet(long size) {
        assert size >= 0;

        int smallSetsNumber = 0;
        if (size > 0) {
            smallSetsNumber = (int) ((size - 1) >> smallBitSetLogSize) + 1;
        }

        bitSets = new BitSet[smallSetsNumber];
        for (int i = 0; i < bitSets.length; i++) {
            bitSets[i] = new BitSet(smallBitSetSize);
        }
        Tool.debug(logger, "Created " + NumUtils.groupDigits(bitSets.length) + " small BitSets");
    }


    public void set(long index) {
        int ind = (int) (index >> smallBitSetLogSize);
        bitSets[ind].set((int) (index & smallBitSetValueMask));
    }
    public boolean get(long index) {
        int ind = (int) (index >> smallBitSetLogSize);
        return bitSets[ind].get((int) (index & smallBitSetValueMask));
    }
    public void clear(long index) {
        int ind = (int) (index >> smallBitSetLogSize);
        bitSets[ind].clear((int) (index & smallBitSetValueMask));
    }

    public void set(long index, boolean value) {
        if (value)
            set(index);
        else
            clear(index);
    }


    /**
     * @return size of this BigBitSet, rounded up to 1 M elements.
     */
    public long size() {
        return bitSets.length * (long) smallBitSetSize;
    }
}
