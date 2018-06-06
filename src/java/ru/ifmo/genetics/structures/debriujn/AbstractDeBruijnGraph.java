package ru.ifmo.genetics.structures.debriujn;

import ru.ifmo.genetics.dna.kmers.BigKmer;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.dna.kmers.MutableBigKmer;

public abstract class AbstractDeBruijnGraph implements DeBruijnGraph {
    @Override
    public abstract boolean containsEdge(BigKmer e);

    @Override
    public abstract boolean addEdge(BigKmer e);

    public int k;
    public int k2; // k * 2
    public long incomeEdgeIncrement;
    public long vertexMask;

    protected int unusedBits;

    protected void setK(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("k should be positive");
        }
        this.k = k;
        k2 = k * 2;
        incomeEdgeIncrement = 1L << k2;
        vertexMask = incomeEdgeIncrement - 1;
        unusedBits = 64 - k2 - 2;
    }

    public void addEdges(LightDna dna) {
        for (MutableBigKmer kmer: MutableBigKmer.kmersOf(dna, k + 1)) {
            addEdge(kmer);
        }
    }

    public boolean containsEdges(LightDna dna) {
        for (MutableBigKmer kmer: MutableBigKmer.kmersOf(dna, k + 1)) {
            if (!containsEdge(kmer)) {
                return false;
            }
        }
        return true;
    }
}
