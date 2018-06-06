package ru.ifmo.genetics.utils;

import java.util.Formatter;

public class NumUtils {

    public static int signum(int x) {
        if (x < 0)
            return -1;
        if (x > 0)
            return 1;
        return 0;
    }

    public static int addAndBound(int value, int inc) {
        if (value > Integer.MAX_VALUE - inc) {
            return Integer.MAX_VALUE;
        }
        return value + inc;
    }
    public static short addAndBound(short value, short inc) {
        if (value > Short.MAX_VALUE - inc) {
            return Short.MAX_VALUE;
        }
        return (short) (value + inc);
    }
    public static long addAndBound(long value, long inc) {
        if (value > Long.MAX_VALUE - inc) {
            return Long.MAX_VALUE;
        }
        return value + inc;
    }

    public static int getPowerOf2(long value) {
        if (value <= 0) {
            return 0;
        }
        return (int) Math.ceil(Math.log(value) / Math.log(2));
    }


    public static long highestBits(long value, int bitsNumber) {
        long hb = Long.highestOneBit(value);
        long mask = 0;
        for (int shift = 0; shift < bitsNumber; shift++) {
            mask |= hb >>> shift;
        }
        return value & mask;
    }

    // returns number with at max bitsNumber highest bits that is not less than value
    public static long highestBitsUpperBound(long value, int bitsNumber) {
        long hb = Long.highestOneBit(value);
        long mask = 0;
        for (int shift = 0; shift < bitsNumber; shift++) {
            mask |= hb >>> shift;
        }
        if ((value & mask) == value) {
            return value & mask;
        }
        return (value & mask) + (hb >>> (bitsNumber - 1));
    }

    /**
     * Compares prefixes of arrays.
     *
     * @param a
     * @param aLength
     * @param b
     * @param bLength
     * @return
     */
    public static boolean equals(int[] a, int aLength, int[] b, int bLength) {
        if (aLength != bLength) {
            return false;
        }

        for (int i = 0; i < aLength; ++i) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculate hash code for array's prefix
     *
     * @param a
     * @param aLength
     * @return
     */
    public static int hashCode(int[] a, int aLength) {
        int res = 0;

        for (int i = 0; i < aLength; ++i) {
            res *= 31;
            res += a[i];
        }
        return res;
    }

    public static void swap(int[] x, int i, int j) {
        int t = x[i];
        x[i] = x[j];
        x[j] = t;
    }

    public static void swap(short[] x, int i, int j) {
        short t = x[i];
        x[i] = x[j];
        x[j] = t;
    }

    public static int compare(long a, long b) {
        return (a < b) ? -1 : (a == b ? 0 : 1);
    }

    public static int compare(int a, int b) {
        return (a < b) ? -1 : (a == b ? 0 : 1);
    }

    public static int compare(byte a, byte b) {
        return a - b;
    }


    private static final String[] suffixes = {"", "K", "M", "G", "T", "P", "E"};
    private static final String[] formats = {"%1.2f %s", "%2.1f %s", "%.0f %s"};
    private static String makeHumanReadable(long n, int base) {
        if (n < base) {
            return n + "";
        }
        double cur = n;
        int index = 0;
        while (cur >= base) {
            cur /= base;
            index++;
        }
        int mainDigits = Integer.toString((int) cur).length();
        int formatIndex = (mainDigits >= 3) ? 2 : (mainDigits - 1);

        return String.format(formats[formatIndex], cur, suffixes[index]);
    }

    public static String makeHumanReadable(long n) {
        return makeHumanReadable(n, 1000);
    }

    public static String memoryAsString(long memoryInBytes) {
        String hr = makeHumanReadable(memoryInBytes, 1024);
        if (Character.isDigit(hr.charAt(hr.length() - 1))) {
            return hr + " bytes";
        }
        return hr + "b";
    }


    /**
     * Converts number to string with digit grouping.
     * For example, 123456789 -> 123'456'789
     */
    public static String groupDigits(long v) {
        String vs = Long.toString(v);

        String ans = "";
        while (vs.length() > 3) {   // i.e. need to add separator
            ans = "'" + vs.substring(vs.length()-3) + ans;
            vs = vs.substring(0, vs.length()-3);
        }
        ans = vs + ans;

        return ans;
    }


    public static double[] normalize(double[] distribution) {
        double[] result = distribution.clone();
        double sum = 0;
        for (int i = 0; i < distribution.length; i++) {
            sum += result[i];
        }
        for (int i = 0; i < distribution.length; i++) {
            result[i] /= sum;
        }
        return result;
    }
}
