package ru.ifmo.genetics.distributed.errorsCorrection.types;

import org.junit.Test;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;

import static org.junit.Assert.*;
public class KmerPositionWritableTest {
    @Test
    public void testCopyConstructor() {
        KmerPositionWritable p1 = new KmerPositionWritable(new Int128WritableComparable(41, 11), 312, (byte)1);
        KmerPositionWritable p2 = new KmerPositionWritable(p1);
        assertEquals(p1, p2);
    }
}
