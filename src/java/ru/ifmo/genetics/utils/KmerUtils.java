package ru.ifmo.genetics.utils;


import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.LightDna;

public class KmerUtils {
    private KmerUtils() {
    }

    public static long reverseComplement(long kmer, long k) {
        kmer = ((kmer & 0x3333333333333333L) << 2) | ((kmer & 0xccccccccccccccccL) >>> 2);
        kmer = ((kmer & 0x0f0f0f0f0f0f0f0fL) << 4) | ((kmer & 0xf0f0f0f0f0f0f0f0L) >>> 4);
        kmer = ((kmer & 0x00ff00ff00ff00ffL) << 8) | ((kmer & 0xff00ff00ff00ff00L) >>> 8);
        kmer = ((kmer & 0x0000ffff0000ffffL) << 16) | ((kmer & 0xffff0000ffff0000L) >>> 16);
        kmer = ((kmer & 0x00000000ffffffffL) << 32) | ((kmer & 0xffffffff00000000L) >>> 32);

        kmer = ~kmer;

        return kmer >>> (64 - 2 * k);
    }

    public static long toLong(LightDna dna, int begin, int end) {
        assert end - begin <= Long.SIZE / 2;
        assert begin - end <= Long.SIZE / 2;
        long res = 0;
        if (begin > end) {
            for (int i = begin - 1; i >= end; --i) {
                res <<= 2;
                res |= dna.nucAt(i) ^ 3;
            }
        } else {
            for (int i = begin; i < end; ++i) {
                res <<= 2;
                res |= dna.nucAt(i);
            }
        }
        return res;
    }

    public static long toLong(LightDna dna) {
        return toLong(dna, 0, dna.length());
    }

    public static LightDna kmer2dna(long kmer, int k) {
        return new Dna(kmer2String(kmer, k));
    }

    public static String kmer2String(long kmer, int k) {
        StringBuilder s = new StringBuilder();
        for (int i = k - 1; i >= 0; --i) {
            byte c = (byte) ((kmer >> (2 * i)) & 3);
            s.append(DnaTools.toChar(c));
        }
        return s.toString();
    }
    public static long getKmerKey(long kmer, int k) {
        return Math.min(kmer, reverseComplement(kmer, k));
    }
}
