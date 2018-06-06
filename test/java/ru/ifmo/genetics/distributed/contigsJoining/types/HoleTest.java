package ru.ifmo.genetics.distributed.contigsJoining.types;

import org.junit.Test;
import ru.ifmo.genetics.tools.microassembly.types.PairedMaybeAlignedDnaQWritable;
import ru.ifmo.genetics.utils.TestUtils;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class HoleTest {
    @Test
    public void testSet() throws Exception {
        Hole a = new Hole();
        a.set(3, true, 10, false);
        assertEquals(a.leftContigId, 3);
        assertEquals(a.leftComplemented, true);
        assertEquals(a.rightContigId, 10);
        assertEquals(a.rightComplemented, false);

        Hole b = new Hole();
        b.set(10, true, 3, false);
        assertEquals(a, b);

        Hole c = new Hole();
        c.set(10, true, 3, true);
        assertThat(c, not(equalTo(a)));

        Hole d = new Hole();
        d.set(10, false, 4, false);
        assertThat(c, not(equalTo(a)));
    }
    
    @Test
    public void testReadWrite() throws IOException {
        Hole a = new Hole();
        a.set(3, true, 10, false);
        TestUtils.testWritable(a, new Hole());
    }

    @Test
    public void testSetFromAlign() {
        PairedMaybeAlignedDnaQWritable pread = new PairedMaybeAlignedDnaQWritable();
        pread.first.isAligned = true;
        pread.first.alignment.contigId = 1;
        pread.first.alignment.offset = 10;
        pread.first.alignment.onForwardStrand = true;

        pread.second.isAligned = true;
        pread.second.alignment.contigId = 2;
        pread.second.alignment.offset = 10;
        pread.second.alignment.onForwardStrand = false;

        Hole a = new Hole();
        a.set(pread);

        assertEquals(1, a.leftContigId);
        assertEquals(false, a.leftComplemented);
        assertEquals(2, a.rightContigId);
        assertEquals(false, a.rightComplemented);

        pread.first.alignment.contigId = 1;
        pread.first.alignment.offset = 10;
        pread.first.alignment.onForwardStrand = false;

        pread.second.alignment.contigId = 2;
        pread.second.alignment.offset = 10;
        pread.second.alignment.onForwardStrand = true;

        a.set(pread);
        assertEquals(1, a.leftContigId);
        assertEquals(true, a.leftComplemented);
        assertEquals(2, a.rightContigId);
        assertEquals(true, a.rightComplemented);
    }
}
