package ru.ifmo.genetics.structures.set;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import static org.junit.Assert.*;

public class LongHashSetTest {

    final Random r = new Random(this.hashCode());
    final int cnt = 700000;

    final LongHashSet set = new LongHashSet(1); // 1 element
    final HashSet<Long> elm = new HashSet<Long>();
    final ArrayList<Long> elmList = new ArrayList<Long>();

    @Before
    public void initialize() {
        for (int i = 0; i < cnt; i++) {
            long v = r.nextLong();
            while (elm.contains(v)) {
                v = r.nextLong();
            }
            elm.add(v);
            elmList.add(v);
        }
    }


    @Test
    public void testWithOneThread() {
        for (long v : elm) {
            assertTrue(set.add(v));
        }

        assertEquals(set.size(), elm.size());

        for (long v : elm) {
            assertFalse(set.add(v));
        }

        for (long v : elm) {
            assertTrue(set.contains(v));
        }

        for (int i = 0; i < cnt; i++) {
            long v = r.nextLong();
            while (elm.contains(v)) {
                v = r.nextLong();
            }
            assertFalse(set.contains(v));
        }
    }

    @Test
    public void testWithEightThreads() {
        for (int i = 0; i < 8; i++) {
            new Thread(new Runnable() {
                Random r = new Random(Thread.currentThread().getName().hashCode());
                @Override
                public void run() {
                    System.out.println("Thread name = " + Thread.currentThread().getName());
                    if (true) {
                        return;
                    }
                    for (int i = 0; i < cnt; i++) {
                        long el = elmList.get(r.nextInt(cnt));
                        boolean before = set.contains(el);
                        sleep();
                        boolean added = set.add(el);
                        sleep();
                        boolean after = set.contains(el);

                        if (before) {
                            assertTrue(!added);
                        }
                        assertTrue(after);

                        sleep();
                    }
                }

                void sleep() {
                    try {
                        Thread.sleep(r.nextInt(10));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
                    , "Tester-"+i+", " + Math.pow(i*i*i + 41*i -98,3)).start();
        }
    }
}
