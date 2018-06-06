package ru.ifmo.genetics.tools.olc.layouter;

import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import static org.junit.Assert.*;

public class ConsensusTest {
    @Test
    public void testGetDna() throws Exception {
        String s = "ACACGGACCGCCGTCCGTTGCCACCGCCGCGCCACCCTTAGA";
        Consensus consensus = new Consensus(null, 0.8);
        consensus.addDna(new Dna(s.substring(0, 15)), 0);
        consensus.addDna(new Dna(s.substring(5, 20)), 5);
        consensus.addDna(new Dna(s.substring(10, 25)), 10);
        consensus.addDna(new Dna(s.substring(10, 15)), 10);
        consensus.addDna(new Dna(s.substring(10, 15)), 10);
        consensus.addDna(new Dna(s.substring(10, 15)), 10);
        consensus.addDna(new Dna(s.substring(10, 15)), 10);
        consensus.addDna(new Dna(s.substring(10, 15)), 10);
        consensus.addDna(new Dna(s.substring(10, 15)), 10);
        consensus.addDna(new Dna(s.substring(10, 15)), 10);
        consensus.addDna(new Dna(s.substring(10, 15)), 10);

        Dna dna1 = new Dna(s.substring(0, 25));
        Dna dna2 = consensus.getDna();
        assertEquals(dna1, dna2);
    }
}
