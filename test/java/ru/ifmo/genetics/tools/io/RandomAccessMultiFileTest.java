package ru.ifmo.genetics.tools.io;

import org.junit.Test;
import static junit.framework.Assert.*;

import ru.ifmo.genetics.io.MultiFile2MemoryMap;
import ru.ifmo.genetics.io.RandomAccessMultiFile;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

public class RandomAccessMultiFileTest {

    static int operationCount = 1000;
    static int length = 1000;

    @Test
    public void test() throws IOException {
        String file1 = "/tmp/file_1";
        String file2 = "/tmp/file_2";
        Random random = new Random();
        byte[] data = new byte[length];
        random.nextBytes(data);
//        println(data);
//        System.err.println();
        dump(file1, data, 0, data.length / 2);
        dump(file2, data, data.length / 2, data.length);

        RandomAccessMultiFile mf = new RandomAccessMultiFile(new String[]{file1, file2}, "rw");
        MultiFile2MemoryMap mp = new MultiFile2MemoryMap(new String[]{file1, file2});

        for (int i = 0; i < operationCount; ++i) {
            int begin = random.nextInt(length);
            int end = random.nextInt(length - begin) + begin;

            if (random.nextBoolean()) {
                // read
//                System.err.println("read " + begin + " " + end);
                byte[] temp = Arrays.copyOfRange(data, begin, end);
                byte[] ar1 = new byte[end - begin];
                byte[] ar2 = new byte[end - begin];
                mf.read(begin, ar1);
                mp.read(begin, ar2);
                assert(equals(ar1, temp) && equals(ar2, temp));
            } else {
                // write
                byte[] temp = new byte[end - begin];
                random.nextBytes(temp);
//                System.err.println("write " + begin + " " + end);
//                println(temp);
                for (int j = begin; j < end; ++j) {
                    data[j] = temp[j - begin];
                }
                mf.write(begin, temp);
                mp.write(begin, temp);
            }
        }
        new File(file1).delete();
        new File(file2).delete();
    }

    void dump(String file, byte[] ar, int begin, int end) throws IOException {
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        for (int i = begin; i < end; ++i) {
            out.writeByte(ar[i]);
        }
        out.close();
    }

    boolean equals(byte[] ar1, byte[] ar2) {
        if (ar1.length != ar2.length) {
            return false;
        }
        for (int i = 0; i < ar1.length; ++i) {
            if (ar1[i] != ar2[i]) {
                return false;
            }
        }
        return true;
    }

    void println(byte[] ar) {
        for (byte b : ar) {
            System.err.print(b + " ");
        }
        System.err.println();
    }

}
