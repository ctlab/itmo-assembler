package ru.ifmo.genetics.structures.debriujn;

import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.dna.kmers.BigKmer;
import ru.ifmo.genetics.structures.set.BigLongHashSet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class CompactDeBruijnGraph extends AbstractDeBruijnGraph implements Writable, DeBruijnGraph {
    private BigLongHashSet edges;

    public CompactDeBruijnGraph() {
        edges = new BigLongHashSet(6, 1);
    }

    public CompactDeBruijnGraph(int k, long memSize) {
        setK(k);
        edges = new BigLongHashSet(memSize/8);
    }

    public void reset() {
        edges.reset();
    }

    @Override
    public boolean addEdge(BigKmer e) {
        return putEdge(e.biLongHashCode());
    }

    public boolean put(long eLongHashCode) {
        return edges.add(eLongHashCode);
    }

    @Override
    public boolean containsEdge(BigKmer e) {
        return edges.contains(e.biLongHashCode());
    }

    public long edgesSize() {
        return edges.size();
    }


    @Override
    public void write(DataOutput out) throws IOException {
        edges.write(out);
        out.writeInt(k);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        edges.readFields(in);
        k = in.readInt();
        setK(k);
    }

    public boolean putEdge(long kmerHash) {
        return edges.add(kmerHash);
    }
}

