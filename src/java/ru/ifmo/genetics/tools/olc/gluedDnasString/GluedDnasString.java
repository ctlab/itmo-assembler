package ru.ifmo.genetics.tools.olc.gluedDnasString;


import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.tools.olc.arrays.LargeLongArray;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;

import java.io.*;
import java.util.Arrays;
import java.util.List;


/**
 * Glued doubled dnas string. Immutable.
 * Stores nucs in long, small indexes in high bits.
 *
 * Assuming string "$dna1$dna2$...$dnaN$" is got as input in constructor,
 * $-char will be appended to it to fit length, then
 * this string will be transformed to "$dna1$dna2$...$dnaN$$$$...$$rc-dnaN$...$rc-dna2$rc-dna1$".
 */
public class GluedDnasString {
    //                          indexes =    0,   1,   2,   3,   4,   5,   6,   7
    public final static byte[] charCodes = {'$', 'A', 'C', 'G', 'T'};
    public final static byte[] rcIndex =   { 0,   4,   3,   2,   1};
    public final static int ALPHABET = charCodes.length;

    final static byte[] transform = new byte[256];
    static {
        Arrays.fill(transform, (byte) -1);
        for (byte index = 0; index < charCodes.length; index++) {
            transform[charCodes[index]] = index;
        }
        transform[0] = -1;
    }

    public final static byte[] DNAindexes = {transform['A'], transform['C'], transform['G'], transform['T']};
    public final static byte $index = transform['$'];

    public final static int nucWidth = 3;
    public final static int nucsInLong = (Long.SIZE - 1) / nucWidth;

    final static long nucMask = (1 << nucWidth) - 1;
    final static long nucsPackMask = (1L << (nucsInLong * nucWidth)) - 1;

    final static long[] clearMask = new long[nucsInLong];
    static {
        for (int i = 0; i < nucsInLong; i++) {
            clearMask[i] = nucsPackMask ^ (nucMask << (i * nucWidth));
        }
    }

    final static long smallBitInNucsMask;
    static {
        long tmp = 0;
        for (int i = 0; i < nucsInLong; i++) {
            tmp <<= nucWidth;
            tmp |= 1;
        }
        smallBitInNucsMask = tmp;
    }
    
    final static int rcNucs = 5;
    final static int rcLen = nucWidth * rcNucs;
    final static int rcMask = (1 << rcLen) - 1;
    final static int rcMapLen = charCodes.length * (1 << (rcLen - nucWidth));
    final static short[] rcMap = new short[rcMapLen];
    static {
        Arrays.fill(rcMap, (short) -1);
        for (int v = 0; v < rcMapLen; v++) {
            int nv = 0;
            boolean good = true;
            for (int i = 0; i < rcNucs; i++) {
                int nuc = (int) ((v >> (i * nucWidth)) & nucMask);
                if (nuc >= rcIndex.length) {
                    good = false;
                    break;
                }
                nuc = rcIndex[nuc];
                nv |= nuc << ((rcNucs - 1 - i) * nucWidth);
            }
            if (good) {
                rcMap[v] = (short) nv;
            }
        }
    }


    
    final LargeLongArray internalArray;

    // Doubled length (i.e. with dnas' rc-copes)
    public final long length;

    public final long realLen;



    /**
     * Assuming string "$dna1$dna2$...$dnaN$" is in file.
     * This string will be transformed to "$dna1$dna2$...$dnaN$$$$...$$rc-dnaN$...$rc-dna2$rc-dna1$".
     */
    public GluedDnasString(File file) throws IOException {
        this(file.length());
        InputStream br = new BufferedInputStream(new FileInputStream(file));
        byte[] buf = new byte[8192];
        long x = 0;
        while (true) {
            int read = br.read(buf);
            if (read == -1)
                break;
            for (int i = 0; i < read; i++) {
                set(x++, transform[buf[i]]);
            }
        }
        br.close();
        assert get(0) == $index;
        assert get(file.length() - 1) == $index;
    }

    /**
     * Assuming string s is formed as "$dna1$dna2$...$dnaN$"
     * This string will be transformed to "$dna1$dna2$...$dnaN$$$$...$$rc-dnaN$...$rc-dna2$rc-dna1$".
     */
    public GluedDnasString(String s) {
        this(s.length());
        for (int i = 0; i < s.length(); i++) {
            set(i, transform[s.charAt(i)]);
        }
        assert get(0) == $index;
        assert get(s.length() - 1) == $index;
    }

    private GluedDnasString(long realSize) {
        if (realSize == 0) {
            throw new IllegalArgumentException("Dna's string is empty!");
        }

        long internalArrayLen = (realSize - 1) / nucsInLong + 1;
        internalArray = new LargeLongArray(internalArrayLen);

        realLen = internalArrayLen * nucsInLong;
        length = 2 * realLen;
    }

    /**
     * Creating string "$dna1$dna2$...$dnaN$",
     * it will be transformed to "$dna1$dna2$...$dnaN$$$$...$$rc-dnaN$...$rc-dna2$rc-dna1$".
     */
    public static GluedDnasString createGluedDnasString(List<Dna> reads) {
        long size = 1;
        for (Dna dna : reads) {
            size += dna.length() + 1;
        }

        GluedDnasString s = new GluedDnasString(size);
        
        long x = 0;
        s.set(x, $index);
        x++;
        for (Dna dna : reads) {
            for (int i = 0; i < dna.length(); i++) {
                s.set(x, transform[DnaTools.toChar(dna.nucAt(i))]);
                x++;
            }
            s.set(x, $index);
            x++;
        }
        return s;
    }



    // only for i from real part of string
    private void set(long i, int nuc) {
        assert (nuc >>> nucWidth) == 0;

        long i1 = i / nucsInLong;
        int i2 = nucsInLong - 1 - (int)(i % nucsInLong);
        
        long x = internalArray.get(i1);
        x &= clearMask[i2];
        x ^= ((long) nuc) << (i2 * nucWidth);
        internalArray.set(i1, x);
    }

    // only for i from real part of string
    private int getInRealString(long i) {
        long i1 = i / nucsInLong;
        int i2 = nucsInLong - 1 - (int)(i % nucsInLong);

        long x = internalArray.get(i1);
        return (int)((x >>> (i2 * nucWidth)) & nucMask);
    }

    public int get(long i) {
        if (i >= realLen) {
            i = 2 * realLen - 1 - i;
            int nuc = getInRealString(i);
            nuc = rcIndex[nuc];
            return nuc;
        }
        return getInRealString(i);
    }

    public long getPackWithLastZeros(long i) {
        long i1 = i / nucsInLong;
        int i2 = (int)(i % nucsInLong);

        long x = getLongPack(i1);
        if (i2 == 0) {
            return x;
        } else {
            long y = getLongPack(i1 + 1);
            long res1 = (x << (i2 * nucWidth)) & nucsPackMask;
            long res2 = (y >>> ((nucsInLong - i2) * nucWidth));
            return res1 | res2;
        }
    }
    
    public int addValueToGoodPack(long i) {
        int i2 = (int)(i % nucsInLong);
        return nucsInLong - i2;
    }

    // i is index in internalArray
    private long getLongPack(long i) {
        if (i < internalArray.length) {
            return internalArray.get(i);
        } else {
            i = 2 * internalArray.length - 1 - i;
            if (i < 0) {
                return 0;
            }
            return getRC(internalArray.get(i));
        }
    }

    static {
        assert nucsInLong == 21; // used in function below
    }
    static long getRC(long x) {
        //       A        B      C      D        E
        //   | 5 nucs | 5 nucs |1nuc| 5 nucs | 5 nucs |
        //
        //                      | |
        //                     \   /
        //                      \ /
        //                       v
        //
        //       E        D      C      B        A
        //   | 5 nucs | 5 nucs |1nuc| 5 nucs | 5 nucs |

        long A = (x >> (16 * nucWidth)) & rcMask;
        long B = (x >> (11 * nucWidth)) & rcMask;
        long C = (x >> (10 * nucWidth)) & nucMask;
        long D = (x >> (5 * nucWidth)) & rcMask;
        long E = x & rcMask;

        // complement it
        A = rcMap[(int) A];
        B = rcMap[(int) B];
        C = rcIndex[(int) C];
        D = rcMap[(int) D];
        E = rcMap[(int) E];

        x = 0;
        x |= E << (16 * nucWidth);
        x |= D << (11 * nucWidth);
        x |= C << (10 * nucWidth);
        x |= B << (5 * nucWidth);
        x |= A;

        return x;
    }

    static {
        assert $index == 0;
        assert nucWidth == 3;
    }
    public static boolean nucsContain$(long nucs) {
        nucs = nucs | (nucs >>> 1) | (nucs >>> 2);
        return (nucs & smallBitInNucsMask) != smallBitInNucsMask;
    }

    private static final int xShift = ((nucsInLong - 1) * nucWidth);

    public static int getFirstNucFromPack(long nucs) {
        return (int) ((nucs >> xShift) & nucMask);
    }


    public int getWithLastZeros(long pos) {
        if (pos >= length) {
            return 0;
        }
        return get(pos);
    }

    public void dump(File file) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        for (long i = 0; i < realLen; i++) {
            out.write(charCodes[get(i)]);
        }
        out.close();
    }

    public String toString(long begin, long end) {
        StringBuilder sb = new StringBuilder();
        for (long i = begin; i < end; i++) {
            sb.append((char)charCodes[get(i)]);
        }
        return sb.toString();
    }

    public String allToString() {
        return toString(0, length);
    }

}

