package ru.ifmo.genetics.transcriptome;

import org.apache.commons.lang.mutable.MutableLong;
import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.dna.kmers.BigKmer;
import ru.ifmo.genetics.structures.debriujn.AbstractDeBruijnGraph;
import ru.ifmo.genetics.structures.debriujn.DeBruijnGraph;
import ru.ifmo.genetics.structures.map.BigLong2IntHashMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

public class CompactDeBruijnGraphWF extends AbstractDeBruijnGraph implements Writable, DeBruijnGraph {
    private BigLong2IntHashMap edges;
    private long memSize;

    public CompactDeBruijnGraphWF() {
        edges = new BigLong2IntHashMap(6, 1);
    }

    public CompactDeBruijnGraphWF(int k, long memSize) {
        setK(k);
        edges = new BigLong2IntHashMap(memSize/12);
        this.memSize = memSize;
    }

    public void reset() {
        edges.reset();
    }


    public int addEdge(long e,int fr) {
        return edges.put(Math.min(e, reverseComplementEdge(e)),fr);
    }

    @Override
    public boolean containsEdge(BigKmer e) {
        return edges.contains(e.biLongHashCode());
    }

    @Override
    public boolean addEdge(BigKmer e) {
        throw new UnsupportedOperationException();
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

    public int getFreg(long e){
        return edges.getWithZero(Math.min(e,reverseComplementEdge(e)));
    }

    public long[] outcomeEdges(long v) {
        long e = (v&vertexMask) << 2;
        long rcE = reverseComplementEdge(e);
        long[] t = new long[4];
        int tl = 0;
        for (int i = 0; i < 4; i++, e++, rcE -= incomeEdgeIncrement) {
            assert reverseComplementEdge(e) == rcE;
            if (edges.contains(Math.min(e, rcE))) {
                t[tl++] = e;
            }
        }

        long[] res = new long[tl];
        System.arraycopy(t, 0, res, 0, tl);
        return res;
    }

    public long[] incomeEdges(long v) {
        long e = v&vertexMask;
        long rcE = reverseComplementEdge(e);
        long[] t = new long[4];
        int tl = 0;
        for (int i = 0; i < 4; i++, e += incomeEdgeIncrement, rcE--) {
            assert reverseComplementEdge(e) == rcE;
            if (edges.contains(Math.min(e, rcE))) {
                t[tl++] = e;
            }
        }

        long[] res = new long[tl];
        System.arraycopy(t, 0, res, 0, tl);
        return res;
    }

    public Iterator<MutableLong> getIterator(){
        return edges.iterator();
    }

    public long reverseComplementEdge(long e) {
        e = ((e & 0x3333333333333333L) << 2) | ((e & 0xccccccccccccccccL) >>> 2);
        e = ((e & 0x0f0f0f0f0f0f0f0fL) << 4) | ((e & 0xf0f0f0f0f0f0f0f0L) >>> 4);
        e = ((e & 0x00ff00ff00ff00ffL) << 8) | ((e & 0xff00ff00ff00ff00L) >>> 8);
        e = ((e & 0x0000ffff0000ffffL) << 16) | ((e & 0xffff0000ffff0000L) >>> 16);
        e = ((e & 0x00000000ffffffffL) << 32) | ((e & 0xffffffff00000000L) >>> 32);

        e = ~e;

        return e >>> unusedBits;
    }

    public long getMemSize(){
        return memSize;
    }
}