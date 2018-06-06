package ru.ifmo.genetics.distributed.clusterization.types;

import org.apache.hadoop.io.WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.Union2WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Author: Sergey Melnikov
 */
public class VertexOrKmerWritableComparable
        extends Union2WritableComparable<Vertex, Kmer> {

    public VertexOrKmerWritableComparable() {
        this((byte) 0);
    }
    public VertexOrKmerWritableComparable(Vertex vertex) {
        super(vertex, new Kmer(), (byte) 0);
    }
    public VertexOrKmerWritableComparable(byte type) {
        super(new Vertex(), new Kmer(), type);

    }



}
