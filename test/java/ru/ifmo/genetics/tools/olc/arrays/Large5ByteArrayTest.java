package ru.ifmo.genetics.tools.olc.arrays;

import org.junit.Test;

import java.io.*;
import java.util.Random;

import static org.junit.Assert.assertTrue;
import static ru.ifmo.genetics.tools.olc.arrays.Util.read5ByteFromStream;
import static ru.ifmo.genetics.tools.olc.arrays.Util.write5ByteToStream;

public class Large5ByteArrayTest {

	private static final int SIZE = (int) 1e6;
	private static final long MAX = (1L << 40) - 1;

	@Test
	public void testSmallNumbers() {
		long[] a = new long[SIZE];
		Large5ByteArray l5a = new Large5ByteArray(SIZE);
		for (int i = 0; i < a.length; i++) {
			a[i] = i + 1;
			l5a.set(i, a[i]);
		}
		for (int i = 0; i < a.length; i++) {
			assertTrue(a[i] == l5a.get(i));
		}
	}

	@Test
	public void testLargeNumbers() {
		long[] a = new long[SIZE];
		Large5ByteArray l5a = new Large5ByteArray(SIZE);
		for (int i = 0; i < a.length; i++) {
			a[i] = MAX - i;
			l5a.set(i, a[i]);
		}
		for (int i = 0; i < a.length; i++) {
			assertTrue("i = " + i + ", a[i] = " + a[i] + ", l5a.get(i) = " + l5a.get(i), a[i] == l5a.get(i));
		}
	}

	@Test
	public void testRandomNumbers() {
		Random rand = new Random();
		long[] a = new long[SIZE];
		Large5ByteArray l5a = new Large5ByteArray(SIZE);
		for (int i = 0; i < a.length; i++) {
			a[i] = Math.abs(rand.nextLong()) % (MAX + 1);
			l5a.set(i, a[i]);
		}
		for (int i = 0; i < a.length; i++) {
			assertTrue("i = " + i + ", a[i] = " + a[i] + ", l5a.get(i) = " + l5a.get(i), a[i] == l5a.get(i));
		}
	}

	@Test
	public void readWriteTest() throws IOException {
		Random rand = new Random();
		OutputStream os = new BufferedOutputStream(new FileOutputStream("testfile"));
		long[] a = new long[SIZE];
		for (int i = 0; i < a.length; i++) {
			a[i] = Math.abs(rand.nextLong()) % (MAX + 1);
			write5ByteToStream(os, a[i]);
		}
		os.close();
		InputStream is = new BufferedInputStream(new FileInputStream("testfile"));
		for (int i = 0; i < a.length; i++) {
			long read = read5ByteFromStream(is);
			assertTrue("i = " + i + ", a[i] = " + a[i] + ", read = " + read, a[i] == read);
		}
		is.close();

	}
}
