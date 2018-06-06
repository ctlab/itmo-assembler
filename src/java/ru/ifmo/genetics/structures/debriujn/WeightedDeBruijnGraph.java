package ru.ifmo.genetics.structures.debriujn;

import org.apache.hadoop.io.Writable;
import org.jetbrains.annotations.NotNull;
import ru.ifmo.genetics.dna.*;
import ru.ifmo.genetics.dna.kmers.*;
import ru.ifmo.genetics.structures.map.BigLong2IntHashMap;
import ru.ifmo.genetics.structures.map.Long2IntHashMapInterface;
import ru.ifmo.genetics.utils.iterators.IterableIterator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class WeightedDeBruijnGraph extends AbstractDeBruijnGraph implements Writable {
    public int minWeightToRealyAdd;
    private Long2IntHashMapInterface edges;;
    private int size = 0;

    public WeightedDeBruijnGraph() {
        edges = new BigLong2IntHashMap(6, 1);
    }

    public WeightedDeBruijnGraph(int k, long graphSizeBytes) {
        this(k, graphSizeBytes, 1);
    }

    public WeightedDeBruijnGraph(int k, long graphSizeBytes, int minWeightToRealyAdd) {
        setK(k);
        edges = new BigLong2IntHashMap(graphSizeBytes);
        this.minWeightToRealyAdd = minWeightToRealyAdd;
    }


    public boolean containsEdge(BigKmer e) {
        return getWeight(e) >= minWeightToRealyAdd;
    }

    public boolean addEdge(BigKmer e) {
        return addEdge(e, 1);
    }

    public boolean addEdge(BigKmer e, int weight) {
        assert e.length() == k + 1;
        int count = edges.getWithZero(e.biLongHashCode());
        count += weight;
        edges.put(e.biLongHashCode(), count);
        return count >= minWeightToRealyAdd;
    }

    public void putEdge(BigKmer e, int weight) {
        assert e.length() == k + 1;
        putEdge(e.biLongHashCode(), weight);
    }

    public void addEdges(LightDnaQ dna, int phredTrim) {
        int trim = 0;
        for (; trim < dna.length(); ++trim) {
            if (dna.phredAt(trim) < phredTrim) {
                break;
            }
        }
        super.addEdges(new DnaView(dna, 0, trim));
    }

    public void addEdgesWithWeight(LightDna dna, int weight) {
        for (int i = 0; i < weight; ++i) {
            super.addEdges(dna);
        }
    }

    public int[] countStat10() {
        int n = 10;
        int[] stat = new int[n];
        for (long i = 0; i < edges.capacity(); ++i) {
            if (!edges.containsAt(i)) {
                continue;
            }
            int val = edges.valueAt(i);
            if (val < n) {
                stat[val]++;
            }
        }
        return stat;
    }

    /**
     * @param e edge to check
     * @return returns weight of the edge or zero if edge doesn't exist
     */
    public int getWeight(BigKmer e) {
        return edges.getWithZero(e.biLongHashCode());
    }

    public int getWeight(long e) {
        throw new UnsupportedOperationException();
    }

    public long edgesSize() {
        return edges.size();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        edges.write(out);
        out.writeInt(minWeightToRealyAdd);
        out.writeInt(k);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        edges.readFields(in);
        minWeightToRealyAdd = in.readInt();
        k = in.readInt();
        setK(k);
    }

    public Iterable<OutEdge> outEdges(ImmutableBigKmer v) {
        return new IterableIterator<OutEdge>(new OutEdgesIterator(v));
    }

    public IterableIterator<InEdge> inEdges(ImmutableBigKmer v) {
        return new IterableIterator<InEdge>(new InEdgesIterator(v));
    }

    public void putEdge(long kmerHash, int weight) {
        int oldWeight = edges.put(kmerHash, weight);
        if (oldWeight < minWeightToRealyAdd && weight >= minWeightToRealyAdd) {
            size++;
        }
    }

    public static class OutEdge {
        @NotNull
        public final ImmutableBigKmer to;
        public final int weight;

        public OutEdge(@NotNull ImmutableBigKmer to, int weight) {
            this.to = to;
            this.weight = weight;
        }
    }

    private class OutEdgesIterator implements Iterator<OutEdge> {
        private ShallowBigKmer e;
        private ImmutableBigKmer v;

        private int i = 0;
        private boolean hasNext = false;
        private int weight = -1;

        public OutEdgesIterator(ImmutableBigKmer v) {
            this.v = v;
            e = new ShallowBigKmer(v);
            e.appendRight((byte)0);
        }


        @Override
        public boolean hasNext() {
            if (hasNext) {
                return true;
            }

            for (; i < 4; ++i) {
                e.updateAt(k, (byte) (Math.max(i - 1, 0)), (byte)i);
                weight = getWeight(e);

                if (weight >= minWeightToRealyAdd) {
                    return hasNext = true;
                }
            }
            return false;
        }

        @Override
        public OutEdge next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            hasNext = false;
            return new OutEdge(v.shiftRight((byte)i++), weight);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    public static class InEdge {
        @NotNull
        public final ImmutableBigKmer from;
        public final int weight;

        public InEdge(@NotNull ImmutableBigKmer from, int weight) {
            this.from = from;
            this.weight = weight;
        }
    }

    private class InEdgesIterator implements Iterator<InEdge> {
        private ShallowBigKmer e;
        private ImmutableBigKmer v;

        private int i = 0;
        private boolean hasNext = false;
        private int weight = -1;

        public InEdgesIterator(ImmutableBigKmer v) {
            this.v = v;
            e = new ShallowBigKmer(v);
            e.appendLeft((byte) 0);
        }


        @Override
        public boolean hasNext() {
            if (hasNext) {
                return true;
            }

            for (; i < 4; ++i) {
                e.updateAt(0, (byte) (Math.max(i - 1, 0)), (byte)i);
                weight = getWeight(e);

                if (weight >= minWeightToRealyAdd) {
                    return hasNext = true;
                }
            }
            return false;
        }

        @Override
        public InEdge next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            hasNext = false;
            return new InEdge(v.shiftLeft((byte) i++), weight);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
