package ru.ifmo.genetics.tools.olc.layouter;

import java.util.Arrays;

public class NucleotideConsensus {
    public static final int DEFAULT_MINIMAL_COVERAGE = 5;
    final int NUCS_N = 4;
    int[] counts = new int[NUCS_N];
    int size = 0;
    final double percentForElect;
    byte maxNuc = -1;
    boolean changed = false;
    final int minimalCoverage;

    public NucleotideConsensus(double percentForElect, int minimalCoverage) {
        this.percentForElect = percentForElect;
        this.minimalCoverage = minimalCoverage;
    }

    public NucleotideConsensus(double percentForElect) {
        this(percentForElect, DEFAULT_MINIMAL_COVERAGE);
    }


    public void put(byte nuc) {
        counts[nuc]++;
        ++size;
        changed = true;
    }

    public byte get() {
        if (!changed) {
            return maxNuc;
        }
        maxNuc = -1;
        for (byte nuc = 0; nuc < NUCS_N; ++nuc) {
            if (maxNuc == -1 || counts[nuc] > counts[maxNuc]) {
                maxNuc = nuc;
            }
        }
        double percent = (counts[maxNuc] + 0.0) / size;
        if (size >= minimalCoverage & percent < percentForElect) {
            return (byte)(-maxNuc - 1);
        }
        changed = false;
        return maxNuc;
    }

    public int size() {
        return size;
    }

    public void reset() {
        size = 0;
        Arrays.fill(counts, 0);
    }


    /*
    private void addLayoutPartToConsensus(
            ArrayList<NucleotideConsensus> consensus,
            LayoutPart part,
            ArrayList<Dna> reads,
            double percentForElect) {
        for (int i = consensus.calculateSize(); i < part.shift + read.length(); ++i) {
            consensus.add(new NucleotideConsensus(percentForElect));
        }
        
        for (int i = 0; i < read.length(); ++i) {
            if (i + part.shift < 0) {
                  System.err.println(part);
            }
            consensus.get(i + part.shift).put(read.nucAt(i));
        }
    }
    */

}
