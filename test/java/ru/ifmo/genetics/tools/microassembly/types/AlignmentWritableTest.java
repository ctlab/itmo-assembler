package ru.ifmo.genetics.tools.microassembly.types;

import org.junit.Test;
import static org.junit.Assert.*;

import ru.ifmo.genetics.utils.TestUtils;

import java.io.IOException;

public class AlignmentWritableTest {
    @Test
    public void testReadWrite() throws IOException {
        AlignmentWritable a = new AlignmentWritable();
        a.contigId = 1;
        a.onForwardStrand = true;
        a.offset = 42;

        AlignmentWritable b = new AlignmentWritable();
        b.contigId = 2;
        b.onForwardStrand = false;
        b.offset = 539;



        AlignmentWritable x = new AlignmentWritable();
        TestUtils.testWritable(a, x);
        TestUtils.testWritable(b, x);
    }
    
    @Test
    public void testReverseComplement() {
        /**
         * |-------contig--------->
         *     |--read-->
         * <-------rcContig-------|
         */
        
        int contigLength = 1321;
        int readLength = 36;
        
        AlignmentWritable a = new AlignmentWritable();
        a.contigId = 1;
        a.onForwardStrand = true;
        a.offset = 42;
        a.reverseComplement(contigLength, readLength);
        assertEquals(false, a.onForwardStrand);
        assertEquals(contigLength - readLength - 42, a.offset);
        a.reverseComplement(contigLength, readLength);

        assertEquals(true, a.onForwardStrand);
        assertEquals(42, a.offset);

        
    }

}
