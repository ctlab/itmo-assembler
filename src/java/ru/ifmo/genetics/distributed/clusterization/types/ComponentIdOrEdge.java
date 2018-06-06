package ru.ifmo.genetics.distributed.clusterization.types;

import org.apache.hadoop.io.GenericWritable;
import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.distributed.io.writable.Union2Writable;

/**
 * Author: Sergey Melnikov
 */
public class ComponentIdOrEdge extends Union2Writable<ComponentID, DirectEdge> {
    public ComponentIdOrEdge() {
        super(new ComponentID(), new DirectEdge(), (byte) 0);
    }

    public ComponentIdOrEdge(ComponentID componentID) {
        super(componentID, new DirectEdge(), (byte) 0);
    }

    public ComponentIdOrEdge(DirectEdge directEdge) {
        super(new ComponentID(), directEdge, (byte) 1);
    }
}
