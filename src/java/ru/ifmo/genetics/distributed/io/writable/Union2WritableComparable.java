package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

public class Union2WritableComparable<A extends WritableComparable/*<A>*/, B extends WritableComparable/*<B>*/>
        extends Union2Writable<A, B>
        implements WritableComparable<Union2WritableComparable<A, B>> {

    protected Union2WritableComparable(A first, B second, byte type) {
        super(first, second, type);
    }

    @Override
    public int compareTo(Union2WritableComparable<A, B> o) {
        int res = type - o.type;
        if (res != 0) {
            return res;
        }

        switch (type) {
            case 0:
                return getFirst().compareTo(o.getFirst());
            case 1:
                return getSecond().compareTo(o.getSecond());
            default:
                throw new RuntimeException("Unexpected type " + type);
        }
    }


}
