package ru.ifmo.genetics.tools.olc.suffixArray;

import org.junit.Test;
import ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString;
import ru.ifmo.genetics.utils.NumUtils;
import ru.ifmo.genetics.utils.TestUtils;

import java.util.Random;

import static org.junit.Assert.*;

public class BucketsSorterTest {
    Random r;
    {
        long seed = (long) (Long.MAX_VALUE * Math.random());
        r = new Random(seed);
        System.err.println("Seed = " + seed);
    }

    @Test
    public void testFastCompareTo$() throws Exception {
//        String s = "$TGC$GCA$";
        String s = TestUtils.generateGluedDnasString(r, 10, 500).allToString();
        GluedDnasString ar = new GluedDnasString(s);
        
        System.err.println("String length : " + s.length());

        for (int i = 0; i < s.length(); ++i) {
            for (int j = 0; j < s.length(); ++j) {
//                System.err.println(i + " " + j);

                int resS = NumUtils.signum(BucketsSorter.compareTill$(ar, i, j));
                int resF = NumUtils.signum(BucketsSorter.fastCompareTill$(ar, i, j));

                if (resS == 0) {
                    // any resF
                } else {
                    assertEquals(resS, resF);
                }
            }
        }
    }
}
