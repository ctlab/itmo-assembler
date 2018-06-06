package ru.ifmo.genetics.tools.olc.layouter;

import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.LightDna;

import java.util.ArrayList;

public class Consensus {
    private final double percentForElect;
    private final int minimalCoverage;
    private final ArrayList<Dna> reads;
    private final ArrayList<NucleotideConsensus> arPositive = new ArrayList<NucleotideConsensus>();
    private final ArrayList<NucleotideConsensus> arNegative = new ArrayList<NucleotideConsensus>();
    private int layersNumber = 0;
    
    private int positiveSize = 0;
    private int negativeSize = 0;


    public Consensus(ArrayList<Dna> reads, double percentForElect, int minimalCoverage) {
        this.percentForElect = percentForElect;
        this.minimalCoverage = minimalCoverage;
        this.reads = reads;
    }

    public Consensus(ArrayList<Dna> reads, double percentForElect) {
        this(reads, percentForElect, NucleotideConsensus.DEFAULT_MINIMAL_COVERAGE);
    }

    public byte get(int pos) {
        return getNucleotideConsensus(pos).get();
    }

    public int startIndex() {
        return -negativeSize();
    }

    public int endIndex() {
        return positiveSize();
    }

    public int positiveSize() {
        return positiveSize;
    }

    public int negativeSize() {
        return negativeSize;
    }

    public int totalSize() {
        return positiveSize() + negativeSize();
    }

    public void reset() {
        for (int i = -negativeSize; i < positiveSize; ++i) {
            getNucleotideConsensus(i).reset();
        }
        positiveSize = negativeSize = 0;
        layersNumber = 0;
    }

    public NucleotideConsensus getNucleotideConsensus(int pos) {
        return (pos >= 0) ? arPositive.get(pos) : arNegative.get(-pos - 1);
    }
    
    public void addDna(LightDna read, int shift) {
        layersNumber++;

        positiveSize = Math.max(positiveSize, shift + read.length());
        for (int i = arPositive.size(); i < positiveSize; ++i) {
            arPositive.add(new NucleotideConsensus(percentForElect, minimalCoverage));
        }

        negativeSize = Math.max(negativeSize, -shift);
        for (int i = arNegative.size(); i < negativeSize; ++i) {
            arNegative.add(new NucleotideConsensus(percentForElect, minimalCoverage));
        }

        for (int i = 0; i < read.length(); ++i) {
            getNucleotideConsensus(i + shift).put(read.nucAt(i));
        }
    }

    public void addLayoutPart(LayoutPart part) {
        addDna(reads.get(part.readNum), part.shift);
    }

    public Dna getDna() {
        byte[] array = new byte[totalSize()];
        for (int i = -negativeSize; i < positiveSize; ++i) {
            byte nuc = get(i);
            if (nuc < 0)  {
                nuc = (byte)(-nuc - 1);
            }
            array[i + negativeSize] = nuc;
        }
        return new Dna(array);
    }
    
    @Override
    public String toString() {
        return toString(-negativeSize, positiveSize);
    }

    public int getLayersNumber() {
        return layersNumber;
    }

    public String toString(int startOffset, int endOffset) {
        StringBuilder sb = new StringBuilder();
        for (int i = startOffset; i < endOffset; ++i) {
            NucleotideConsensus c = getNucleotideConsensus(i);
            if (c.size() == 0) {
                sb.append('N');
                continue;
            }
            byte nuc = c.get();
            if (nuc < 0) {
                nuc = (byte)(-nuc - 1);
            }

            sb.append(DnaTools.toChar(nuc));
        }
        return sb.toString();
    }
}
