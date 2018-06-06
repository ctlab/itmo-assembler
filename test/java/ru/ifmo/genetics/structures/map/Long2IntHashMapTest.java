package ru.ifmo.genetics.structures.map;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class Long2IntHashMapTest {

    final Random r = new Random(this.hashCode());
    final int cnt = 700000;

    final Long2IntHashMap map = new Long2IntHashMap(1); // 1 element
    final HashMap<Long, Integer> elm = new HashMap<Long, Integer>();
    final ArrayList<Long> keyList = new ArrayList<Long>();
    final ArrayList<Integer> valList = new ArrayList<Integer>();

    @Before
    public void initialize() {
        for (int i = 0; i < cnt; i++) {
            long key = r.nextLong();
            while (elm.containsKey(key)) {
                key = r.nextLong();
            }
            int value = r.nextInt(1000);

            elm.put(key, value);
            keyList.add(key);
            valList.add(value);
        }
    }


    @Test
    public void testWithOneThread() {
        for (Map.Entry<Long, Integer> entry : elm.entrySet()) {
            assertTrue(map.put(entry.getKey(), entry.getValue()) == -1);
        }

        assertEquals(map.size(), elm.size());

        for (Map.Entry<Long, Integer> entry : elm.entrySet()) {
            assertTrue(map.put(entry.getKey(), entry.getValue()) != -1);
        }

        for (Map.Entry<Long, Integer> entry : elm.entrySet()) {
            assertTrue(map.contains(entry.getKey()));
        }

        for (int i = 0; i < cnt; i++) {
            long v = r.nextLong();
            while (elm.containsKey(v)) {
                v = r.nextLong();
            }
            assertFalse(map.contains(v));
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
                        int n = r.nextInt(cnt);
                        long key = keyList.get(n);
                        int value = valList.get(n);

                        int beforeV = map.get(key);
                        sleep();
                        int inV = map.put(key, value);
                        sleep();
                        int afterV = map.get(key);

                        boolean before = (beforeV != -1);
                        boolean in = (inV != -1);
                        boolean after = (afterV != -1);

                        if (before) {
                            assertTrue(in);
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

    @Test
    public void testAddWithEightThreads() {
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
                        int n = r.nextInt(cnt);
                        long key = keyList.get(n);
                        int incValue = r.nextInt(1000);

                        int prevV = map.addAndBound(key, incValue);
                        sleep();
                        int afterV = map.get(key);

                        assertTrue(afterV >= prevV + incValue);
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
