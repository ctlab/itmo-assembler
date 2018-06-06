package ru.ifmo.genetics.structures.debriujn;

import ru.ifmo.genetics.dna.kmers.BigKmer;

public interface DeBruijnGraph {
    boolean addEdge(BigKmer e);
    boolean containsEdge(BigKmer e);
}
