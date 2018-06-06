package ru.ifmo.genetics.utils;

import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static ru.ifmo.genetics.dna.DnaTools.NUCLEOTIDES;

public class TestUtils {
    /**
     * Generate random int i (min <= i <= max).
     */
    public static int genInt(Random r, int min, int max) {
        return min + r.nextInt(max - min + 1);
    }
    
    public static String[] generateReads(Random r, int minN, int maxN, int minL, int maxL) {
        HashSet<String> set = new HashSet<String>();
        
        int n = genInt(r, minN, maxN);
        for (int i = 0; i < n; i++) {
            String s;
            do {
                int l = genInt(r, minL, maxL);
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < l; j++) {
                    sb.append(NUCLEOTIDES[r.nextInt(NUCLEOTIDES.length)]);
                }
                s = sb.toString();
            } while (set.contains(s));

            set.add(s);
        }
        System.err.println("Generated reads size = " + set.size());

        return set.toArray(new String[set.size()]);
    }

    public static String[] generateReads(Random r, int maxN, int maxL) {
        return generateReads(r, maxN / 2, maxN, maxL / 2, maxL);
    }

    public static GluedDnasString generateGluedDnasString(Random r, int maxN, int maxL) {
        String[] stringReads = generateReads(r, maxN, maxL);
        List<Dna> reads = convert(stringReads);
        GluedDnasString string = GluedDnasString.createGluedDnasString(reads);
        return string;
    }
    

    public static ArrayList<Dna> convert(String[] stringReads) {
        ArrayList<Dna> reads = new ArrayList<Dna>();
        for (String s : stringReads) {
            reads.add(new Dna(s));
        }
        return reads;
    }


    public static ArrayList<Dna> doubleReads(List<Dna> reads) {
        ArrayList<Dna> dReads = new ArrayList<Dna>();
        for (Dna dna : reads) {
            dReads.add(dna);
            dReads.add(dna.reverseComplement());
        }
        return dReads;
    }


    /**
     * Copies one Writable to another via ByteArray streams
     *
     * @param a source
     * @param b destination
     */
    public static <T extends Writable> void copyWritable(T a, T b) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        a.write(dout);
        dout.close();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        DataInputStream din = new DataInputStream(in);
        b.readFields(din);
        din.close();

    }

    public static <T extends Writable> void testWritable(T a, T b) throws IOException {
        copyWritable(a, b);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
