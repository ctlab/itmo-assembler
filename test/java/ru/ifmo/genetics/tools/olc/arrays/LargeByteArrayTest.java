package ru.ifmo.genetics.tools.olc.arrays;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;


public class LargeByteArrayTest {

	private static final int SIZE = (int) 1e7;

	@Test
	public void testSmallNumbers() {
		int[] a = new int[SIZE];
		LargeByteArray l5a = new LargeByteArray(SIZE);
		for (int i = 0; i < a.length; i++) {
			a[i] = i & 0xff;
			l5a.set(i, a[i]);
		}
		for (int i = 0; i < a.length; i++) {
			assertTrue(a[i] == l5a.get(i));
		}
	}

	@Test
	public void testRandomNumbers() {
		Random rand = new Random();
		int[] a = new int[SIZE];
		LargeByteArray l5a = new LargeByteArray(SIZE);
		for (int i = 0; i < a.length; i++) {
			a[i] = rand.nextInt() & 0xff;
			l5a.set(i, a[i]);
		}
		for (int i = 0; i < a.length; i++) {
			assertTrue("i = " + i + ", a[i] = " + a[i] + ", l5a.get(i) = " + l5a.get(i), a[i] == l5a.get(i));
		}
	}

	@Test
	public void testByteArrayConstructor() {
		Random rand = new Random();
		byte[] a = new byte[SIZE];
		for (int i = 0; i < a.length; i++) {
			a[i] = (byte) rand.nextInt();
		}
		LargeByteArray l5a = new LargeByteArray(a);
		for (int i = 0; i < a.length; i++) {
			assertTrue("i = " + i + ", a[i] = " + a[i] + ", l5a.get(i) = " + l5a.get(i), (a[i] & 0xff) == l5a.get(i));
		}
	}

}
