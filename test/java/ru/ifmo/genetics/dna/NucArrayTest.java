package ru.ifmo.genetics.dna;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class NucArrayTest {
    @Test
    public void testGet() throws Exception {
        String s = "ATGTGTATTTGTGTAGTTATGTAGATGAATGATAGTTTAGATGTACA";
        NucArray array = new NucArray(new Dna(s));
        assertEquals(s, array.toString());
    }

    @Test
    public void testSet() throws Exception {
        String s = "ATGTGTATTTGTGTAGTTATGTAGATGAATGATAGTTTAGATGTACA";
        NucArray array = new NucArray(new Dna(s));
        String s2 = "GTATTTCAGCATCAGCATTGCATCAGGATATAGCATACAGTCAGGAG";
        for (int i = 0; i < s2.length(); ++i) {
            array.set(i, DnaTools.fromChar(s2.charAt(i)));
        }

        assertEquals(s2, array.toString());

        String s3 = "GTCAGTCTCAGTCATCAGACG";
        String s4 = "ATCAGTCTCAGTCATCAGACG";
        NucArray array3 = new NucArray(new Dna(s3));
        array3.set(0, (byte)0);
        assertEquals(s4, array3.toString());
    }

}
