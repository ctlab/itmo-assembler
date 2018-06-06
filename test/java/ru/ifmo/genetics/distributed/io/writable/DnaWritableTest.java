package ru.ifmo.genetics.distributed.io.writable;

import org.apache.hadoop.io.Text;
import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.utils.TestUtils;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;

public class DnaWritableTest {
    @Test
    public void testReadWrite() throws IOException {
        DnaWritable dna1 = new DnaWritable(new Dna("ACACGGACCGCCGCCACCGCCGCGCCACCCTTAGA"));
        DnaWritable dna3 = new DnaWritable(new Dna("ACACGGACCGCCGTCCGTTGCCACCGCCGCGCCACCCTTAGA"));
        DnaWritable dna2 = new DnaWritable();

        TestUtils.copyWritable(dna3, dna2);
        TestUtils.testWritable(dna1, dna2);
    }
    
    @Test
    public void testSet() {
        DnaWritable dna1 = new DnaWritable(new Dna("ACACGGACCGCCGCCACCGCCGCGCCACCCTTAGA"));
        DnaWritable dna2 = new DnaWritable(new Dna("ACACGGACCGCCGTCCGTTGCCACCGCCGCGCCACCCTTAGA"));
        
        assertThat(dna1, not(equalTo(dna2)));
        dna2.set(new Dna("ACACGGACCGCCGCCACCGCCGCGCCACCCTTAGA"));
        assertEquals(dna1, dna2);

        DnaWritable dna3 = new DnaWritable(new Dna("ACACGGACCGCCGTCCGTTGCCACCGCCGCGCCACCCTTAGA"));
        dna3.set(new Text("ACACGGACCGCCGCCACCGCCGCGCCACCCTTAGA"));
        assertEquals(dna1, dna3);

        assertEquals(dna2, dna3);
    }
}

