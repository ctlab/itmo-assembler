package ru.ifmo.genetics.tools.olc.gluedDnasString;

import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString;
import ru.ifmo.genetics.utils.TestUtils;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.*;

public class GluedDnasStringTest {
    Random r = new Random(6458468465413L);

    @Test
    public void handTest() {
        GluedDnasString s = new GluedDnasString("$ACT$CAT$");
        System.out.println(GluedDnasString.rcNucs);
        System.out.println(GluedDnasString.rcLen);
        System.out.println(GluedDnasString.rcMapLen);
        //                                                           |    |    ||    |    |
        System.out.println(Long.toOctalString(GluedDnasString.getRC(0444444444444444144444L)));
        assert true;
    }


    @Test
    public void randomGetTests() {
        for (int maxReadsNumber = 2; maxReadsNumber < 200; maxReadsNumber++) {
            System.err.println();

//            String[] stringReads =  {"ACGT", "G"};
            String[] stringReads = TestUtils.generateReads(r, maxReadsNumber, 10);
            ArrayList<Dna> rReads = TestUtils.convert(stringReads);

            GluedDnasString text = GluedDnasString.createGluedDnasString(rReads);

            String s = getRightString(rReads, text.realLen);
            System.err.println("Right string length: " + s.length());
//            System.err.println("Glued string : " + text);

            for (int i = 0; i < s.length(); i++) {
                assertEquals(s.charAt(i), (char) GluedDnasString.charCodes[text.get(i)]);
            }
        }
    }

    @Test
    public void constructorsRandomTests() {
        for (int maxReadsNumber = 2; maxReadsNumber < 200; maxReadsNumber++) {
            System.err.println();

            String[] stringReads = TestUtils.generateReads(r, maxReadsNumber, 10);
            ArrayList<Dna> reads = TestUtils.convert(stringReads);
            String s = glueReads(reads);

            GluedDnasString text1 = GluedDnasString.createGluedDnasString(reads);
            GluedDnasString text2 = new GluedDnasString(s);

            String text1String = text1.allToString();
            String text2String = text2.allToString();

            assertEquals(text1String, text2String);
        }
    }


    @Test
    public void getPackRandomTests() {
        for (int maxReadsNumber = 2; maxReadsNumber < 200; maxReadsNumber++) {
            System.err.println();

//            String[] stringReads = {"ACGT", "G"};
            String[] stringReads = TestUtils.generateReads(r, maxReadsNumber, 10);
            ArrayList<Dna> rReads = TestUtils.convert(stringReads);

            GluedDnasString text = GluedDnasString.createGluedDnasString(rReads);

            String s = getRightString(rReads, text.realLen);
            System.err.println("Right string length : " + s.length());

            for (int i = 0; i < s.length(); i++) {
                long p = text.getPackWithLastZeros(i);

                for (int j = GluedDnasString.nucsInLong - 1; j >= 0; j--) {
                    int nuc = (int) (p & GluedDnasString.nucMask);
                    p >>>= GluedDnasString.nucWidth;

                    char c = '$';
                    if (i + j < s.length()) {
                        c = s.charAt(i + j);
                    }

                    assertEquals(c, (char) GluedDnasString.charCodes[nuc]);
                }
            }
        }
    }

    
    public static String getRightString(ArrayList<Dna> reads, long realLen) {
        StringBuilder sb = new StringBuilder();
        sb.append(glueReads(reads));
        while (sb.length() < realLen) {
            sb.append('$');
        }

        String s = sb.toString();
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == '$') {
                sb.append(c);
            } else {
                sb.append(DnaTools.complement(c));
            }
        }
        s = sb.toString();
        return s;
    }

    @Test
    public void testNucsContain$() {
        for (int i = 0; i < 10000; i++) {
            long x = 0;
            for (int j = 0; j < GluedDnasString.nucsInLong; j++) {
                int cn = r.nextInt(GluedDnasString.DNAindexes.length);
                cn = GluedDnasString.DNAindexes[cn];
                x <<= GluedDnasString.nucWidth;
                x |= cn;
            }

//            System.err.println(Long.toBinaryString(x));
            assertTrue(!GluedDnasString.nucsContain$(x));

            int p = r.nextInt(GluedDnasString.nucsInLong);
            int nw = GluedDnasString.nucWidth;
            int ls = (p + 1) * nw;
            int rs = p * nw;
            long rm = (1L << rs) - 1;
            x = ((x >>> ls) << ls) | (x & rm);

//            System.err.println(Long.toBinaryString(x));
            assertTrue(GluedDnasString.nucsContain$(x));
        }
    }


    public static String glueReads(ArrayList<Dna> reads) {
        StringBuilder sb = new StringBuilder();
        sb.append('$');
        for (Dna dna : reads) {
            sb.append(dna);
            sb.append('$');
        }
        return sb.toString();
    }

}
