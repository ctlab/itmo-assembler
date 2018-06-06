package ru.ifmo.genetics.tools.microassembly.types;

import org.apache.hadoop.io.Text;
import org.junit.Test;
import ru.ifmo.genetics.distributed.io.writable.DnaQWritable;
import ru.ifmo.genetics.io.formats.Sanger;
import ru.ifmo.genetics.utils.TestUtils;

import static org.junit.Assert.*;

public class BowtieAlignmentWritableTest {
    Text line = new Text("SRR001665.25780 071112_SLXA-EAS1_s_4:1:2:932:113 length=72/2	+	10	13879	ACACGGACCGCCGCCACCGCCGCGCCACCCTTAGAA	IIIIIII&IIIII+<%II@'I3IC+I&I#%*&9I\"A	0	29:A>C,31:A>T");

    @Test
    public void testParseFromLine() throws Exception {
        BowtieAlignmentWritable alignment = new BowtieAlignmentWritable();
        alignment.parseFromLine(line, new Sanger());

        assertEquals(alignment.readId, new Text("SRR001665.25780 071112_SLXA-EAS1_s_4:1:2:932:113 length=72/2"));
        assertEquals(alignment.onForwardStrand, true);
        assertEquals(alignment.contigId, new Text("10"));
        assertEquals(alignment.offset, 13879);
        assertEquals(alignment.sequence, new DnaQWritable(
                new Text("ACACGGACCGCCGCCACCGCCGCGCCACCCTTAGAA"),
                new Text("IIIIIII&IIIII+<%II@'I3IC+I&I#%*&9I\"A"),
                new Sanger()));
        assertEquals(alignment.magic, 0);
        assertEquals(alignment.mismatches, new Text("29:A>C,31:A>T"));
    }

    @Test
    public void testReadWrite() throws Exception {
        BowtieAlignmentWritable alignment1 = new BowtieAlignmentWritable();
        alignment1.parseFromLine(line, new Sanger());

        BowtieAlignmentWritable alignment2 = new BowtieAlignmentWritable();
        TestUtils.testWritable(alignment1, alignment2);
    }
}
