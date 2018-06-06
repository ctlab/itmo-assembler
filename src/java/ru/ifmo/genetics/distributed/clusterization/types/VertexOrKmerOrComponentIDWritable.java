package ru.ifmo.genetics.distributed.clusterization.types;


import ru.ifmo.genetics.distributed.io.writable.Union3Writable;

/**
 * Author: Sergey Melnikov
 */
public class VertexOrKmerOrComponentIDWritable extends Union3Writable<Vertex, Kmer, ComponentID> {

    public VertexOrKmerOrComponentIDWritable() {
        this((byte) 0);
    }

    public VertexOrKmerOrComponentIDWritable(byte type) {
        super(new Vertex(), new Kmer(), new ComponentID(), type);
    }


    public VertexOrKmerOrComponentIDWritable(ComponentID componentID) {
        super(new Vertex(), new Kmer(), componentID, (byte) 2);
    }
}
