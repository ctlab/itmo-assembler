package ru.ifmo.genetics.structures.set;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class BigBitSetTest {
    @Test
    public void smallTest() {
        int size = 1 << 20;   // 1 M elements
        BigBitSet set = new BigBitSet(size);
        boolean[] set2 = new boolean[size];

        Random r = new Random(this.hashCode());
        for (int i = 0; i < set2.length; i++) {
            assertTrue(set.get(i) == set2[i]);
        }

        for (int i = 0; i < (int) 5e5; i++) {
            int ind = r.nextInt(size);
            set.set(ind);
            set2[ind] = true;
        }

        for (int i = 0; i < set2.length; i++) {
            assertTrue(set.get(i) == set2[i]);
        }

        for (int i = 0; i < (int) 5e5; i++) {
            int ind = r.nextInt(size);
            set.clear(ind);
            set2[ind] = false;
        }

        for (int i = 0; i < set2.length; i++) {
            assertTrue(set.get(i) == set2[i]);
        }
    }

    @Test
    public void bigTest() {
        int size = 20 * (1 << 20);   // 20 M elements
        BigBitSet set = new BigBitSet(size);
        boolean[] set2 = new boolean[size];

        Random r = new Random(this.hashCode());
        for (int i = 0; i < set2.length; i++) {
            assertTrue(set.get(i) == set2[i]);
        }

        for (int i = 0; i < (int) 1e7; i++) {
            int ind = r.nextInt(size);
            set.set(ind);
            set2[ind] = true;
        }

        for (int i = 0; i < set2.length; i++) {
            assertTrue(set.get(i) == set2[i]);
        }

        for (int i = 0; i < (int) 1e7; i++) {
            int ind = r.nextInt(size);
            set.clear(ind);
            set2[ind] = false;
        }

        for (int i = 0; i < set2.length; i++) {
            assertTrue(set.get(i) == set2[i]);
        }
    }

}
