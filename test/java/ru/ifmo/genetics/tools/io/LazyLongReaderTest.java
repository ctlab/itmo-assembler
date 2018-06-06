package ru.ifmo.genetics.tools.io;

import org.junit.Test;

import java.io.*;

import static junit.framework.Assert.assertEquals;

public class LazyLongReaderTest {
    @Test
    public void test() throws Exception {
        String[] files = {"/tmp/LazyLongReaderTest_1", "/tmp/LazyLongReaderTest_2"};

        try {
            long x = 0;
            for (String file : files) {
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                int total = 10;
                for (int i = 0; i < total; ++i) {
                    out.writeLong(x++);
                }
                out.close();
            }

            int i = 0;
            LazyLongReader reader = new LazyLongReader(files);
            while (true) {
                try {
                    long l = reader.readLong();
                    assertEquals(i, l);
                    ++i;
                } catch (EOFException e) {
                    break;
                }
            }
        } finally {
            for (String file : files) {
                new File(file).delete();
            }
        }
    }
}
