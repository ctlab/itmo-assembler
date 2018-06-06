package ru.ifmo.genetics.utils;

public class Sorter {
    public interface SortTraits {
        public int compare(int i, int j);
        public void swap(int i, int j);
    }

    /**
     * Returns the index of the median of the three indexed items.
     */
    private static int med3(int a, int b, int c, SortTraits traits) {
        return (traits.compare(a, b) < 0 ?
            (traits.compare(b, c) < 0 ? b : traits.compare(a, c) < 0 ? c : a) :
            (traits.compare(b, c) > 0 ? b : traits.compare(a, c) > 0 ? c : a));
    }

    /**
     * Sorts the specified sub-array of items into ascending order.
     */
    public static void sort(int off, int len, SortTraits traits) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++)
                for (int j = i; j > off && traits.compare(j - 1, j) > 0; j--)
                    traits.swap(j, j-1);
            return;
        }


        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len/8;
                l = med3(l,     l+s, l+2*s, traits);
                m = med3(m-s,   m,   m+s, traits);
                n = med3(n-2*s, n-s, n, traits);
            }
            m = med3(l, m, n, traits); // Mid-size, med of 3
        }

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while(true) {
            while (b <= c && traits.compare(b, m) <= 0) {
                if (traits.compare(b, m) == 0) {
                    traits.swap(a++, b);
                    m = a - 1;
                }
                b++;
            }
            while (c >= b && traits.compare(c, m) >= 0) {
                if (traits.compare(c, m) == 0) {
                    traits.swap(c, d--);
                    m = d + 1;
                }
                c--;
            }
            if (b > c)
                break;
            traits.swap(b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a-off, b-a  );  vecswap(off, b-s, s, traits);
        s = Math.min(d-c,   n-d-1);  vecswap(b,   n-s, s, traits);

        // Recursively sort non-partition-elements
        if ((s = b-a) > 1)
            sort(off, s, traits);
        if ((s = d-c) > 1)
            sort(n-s, s, traits);
    }


    private static void vecswap(int a, int b, int n, SortTraits traits) {
        for (int i=0; i<n; i++, a++, b++)
            traits.swap(a, b);
    }
}

