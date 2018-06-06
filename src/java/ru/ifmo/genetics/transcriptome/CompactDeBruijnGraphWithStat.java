package ru.ifmo.genetics.transcriptome;

import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.structures.map.BigLong2IntHashMap;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class CompactDeBruijnGraphWithStat {
    private static int MAX_K = 31;
    private BigLong2IntHashMap edges;

    public int k;
    public int k2; // k * 2
    public long incomeEdgeIncrement;
    public long vertexMask;
    public long edgeMask;

    private int unusedBits;

    public CompactDeBruijnGraphWithStat() {
        edges = new BigLong2IntHashMap(6, 1);
    }

    public CompactDeBruijnGraphWithStat(int k, long memSize) {
        if ((k > MAX_K) || (k <= 0)) {
            throw new IllegalArgumentException("k should be in range 1.." + MAX_K);
        }
        edges = new BigLong2IntHashMap(memSize/12);
        this.k = k;
        k2 = k * 2;
        incomeEdgeIncrement = 1L << k2;
        edgeMask = (1L << (k2+2))-1;
        vertexMask = incomeEdgeIncrement - 1;
        unusedBits = 64 - k2 - 2;
    }

    public void reset() {
        edges.reset();
    }



    /*
    public void addEdges(LightDna dna) {
        if (dna.length() <= k)
            return;
        long cur = 0;
        for (int i = 0; i < k; i++) {
            cur = (cur << 2) | dna.nucAt(i);
        }
        // String s = dnaq.toString();
        for (int i = k; i < dna.length(); i++) {
            cur = cur & vertexMask;
            cur = (cur << 2) | dna.nucAt(i);
            addEdge(cur);
        }
    }
    */

    public boolean containsEdges(LightDna dna) {
        if (dna.length() <= k)
            return true;
        long cur = 0;
        for (int i = 0; i < k; i++) {
            cur = (cur << 2) | dna.nucAt(i);
        }
        // String s = dnaq.toString();
        for (int i = k; i < dna.length(); i++) {
            cur = cur & vertexMask;
            cur = (cur << 2) | dna.nucAt(i);
            if (!containsEdge(cur)) {
                return false;
            }
        }
        return true;

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

    private long getEdgesKey(long e, long rcE) {
        return Math.min(e&edgeMask, rcE&edgeMask);
    }

    public long getEdgeKey(long e) {
        return getEdgesKey(e, reverseComplementEdge(e&edgeMask));
    }

    public int addEdge(long e,int value) {
        return edges.put(getEdgeKey(e),value);
    }

    public boolean containsEdge(long e) {
        return edges.contains(getEdgeKey(e));
    }

    public int getValue(long v){
        return edges.valueAt(getEdgeKey(v));
    }

    public long[] outcomeEdges(long v) {
        long e = v << 2;
        long rcE = reverseComplementEdge(e);
        long[] t = new long[4];
        int tl = 0;
        for (int i = 0; i < 4; i++, e++, rcE -= incomeEdgeIncrement) {
            assert reverseComplementEdge(e) == rcE;
            if (edges.contains(getEdgesKey(e, rcE))) {
                t[tl++] = e;
            }
        }

        long[] res = new long[tl];
        System.arraycopy(t, 0, res, 0, tl);
        return res;
    }

    public long[] incomeEdges(long v) {
        long e = v;
        long rcE = reverseComplementEdge(e);
        long[] t = new long[4];
        int tl = 0;
        for (int i = 0; i < 4; i++, e += incomeEdgeIncrement, rcE--) {
            assert reverseComplementEdge(e) == rcE;
            if (edges.contains(getEdgesKey(e, rcE))) {
                t[tl++] = e;
            }
        }

        long[] res = new long[tl];
        System.arraycopy(t, 0, res, 0, tl);
        return res;
    }

    /*
    public static void main(String[] args) {
        int[][] bases = new int[][]{
                {2, 1, 0, 3, 2, 0, 1, 0, 2, 3, 0, 3, 1, 3, 2, 0, 1},
                {2, 1, 0, 3, 2, 0, 1, 0, 2, 3, 0, 3, 1, 3, 2, 0, 3},
                {0, 2, 1, 0, 3, 2, 0, 1, 0, 2, 3, 0, 3, 1, 3, 2, 0},
                {1, 2, 1, 0, 3, 2, 0, 1, 0, 2, 3, 0, 3, 1, 3, 2, 0},
                {3, 2, 1, 0, 3, 2, 0, 1, 0, 2, 3, 0, 3, 1, 3, 2, 0},
        };
        int p = bases[0].length;
        int k = p - 1;
        int sz = bases.length;

        CompactDeBruijnGraphWithStat g = new CompactDeBruijnGraphWithStat(k, 1024 * 8L);

        long[] es = new long[sz];
        long[] rcEs = new long[sz];
        for (int i = 0; i < sz; ++i) {
            for (int j = 0; j < p; ++j) {
                es[i] <<= 2;
                rcEs[i] <<= 2;

                es[i] += bases[i][j];
                rcEs[i] += bases[i][p - j - 1] ^ 3;
            }
            assert g.reverseComplementEdge(es[i]) == rcEs[i];
            assert g.reverseComplementEdge(rcEs[i]) == es[i];
            g.addEdge(es[i]);
            g.addEdge(rcEs[i]);
            assert g.containsEdge(es[i]);
            assert g.containsEdge(rcEs[i]);
        }

        assert Arrays.equals(g.outcomeEdges(es[0] >>> 2), new long[]{es[0], es[1]});
        assert Arrays.equals(g.incomeEdges(es[0] >>> 2), new long[]{es[2], es[3], es[4]});
        System.out.println("ok");
    } */

    public long edgesSize() {
        return edges.size();
    }

/*
    @Override
    public void write(DataOutput out) throws IOException {
        edges.write(out);
        out.writeInt(k);
        out.writeInt(k2);
        out.writeLong(incomeEdgeIncrement);
        out.writeLong(vertexMask);
        out.writeInt(unusedBits);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        edges.readFields(in);
        k = in.readInt();
        k2 = in.readInt();
        incomeEdgeIncrement = in.readLong();
        vertexMask = in.readLong();
        unusedBits = in.readInt();
    }
    */
}



