package ru.ifmo.genetics.tools.microassembly.types;

import org.apache.hadoop.io.Text;
import org.junit.Test;
import ru.ifmo.genetics.io.formats.Illumina;
import ru.ifmo.genetics.io.formats.QualityFormatFactory;
import ru.ifmo.genetics.utils.TestUtils;

import java.io.IOException;

public class MaybeAlignedDnaQWritableTest {
    @Test
    public void testReadWrite() throws IOException {
        MaybeAlignedDnaQWritable a = new MaybeAlignedDnaQWritable();
        a.dnaq.clear();
        a.isAligned = false;
        
        MaybeAlignedDnaQWritable b = new MaybeAlignedDnaQWritable();
        b.dnaq.set(new Text("ATG"), new Text("BBB"), Illumina.instance);
        b.isAligned = false;
        
        MaybeAlignedDnaQWritable c = new MaybeAlignedDnaQWritable();
        c.dnaq.clear();
        c.isAligned = true;
        c.alignment.contigId = 1;
        c.alignment.onForwardStrand = true;
        c.alignment.offset = 42;

        MaybeAlignedDnaQWritable d = new MaybeAlignedDnaQWritable();
        d.dnaq.set(new Text("ATG"), new Text("BBB"), Illumina.instance);
        d.isAligned = true;
        d.alignment.contigId = 1;
        d.alignment.onForwardStrand = true;
        d.alignment.offset = 42;

        MaybeAlignedDnaQWritable x = new MaybeAlignedDnaQWritable();
        TestUtils.testWritable(a, x);
        TestUtils.testWritable(b, x);
        TestUtils.testWritable(c, x);
        TestUtils.testWritable(d, x);

    }
}
