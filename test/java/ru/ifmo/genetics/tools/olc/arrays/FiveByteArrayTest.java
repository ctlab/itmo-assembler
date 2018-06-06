package ru.ifmo.genetics.tools.olc.arrays;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class FiveByteArrayTest {

	private static final int SIZE = (int) 1e7;
	private static final long MAX_VALUE = (1L << 40) - 1;

	@Test
	public void testRandomNumbers() {
		Random rand = new Random();
		long[] a = new long[SIZE];
		FiveByteArray array = new FiveByteArray(SIZE);
		for (int i = 0; i < a.length; i++) {
			a[i] = (long) (MAX_VALUE * rand.nextDouble());
			array.set(i, a[i]);
		}
		for (int i = 0; i < a.length; i++) {
//            assertTrue("i = " + i + ", a[i] = " + a[i] + ", array.get(i) = " + array.get(i), a[i] == array.get(i));
            assertTrue(a[i] == array.get(i));
		}
	}

}
