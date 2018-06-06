package ru.ifmo.genetics.distributed.clusterization.types;

import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Author: Sergey Melnikov
 */
public class DirectEdge implements Writable {
    Int128WritableComparable to = new Int128WritableComparable();
    int weight;

    public DirectEdge() {
    }


    DirectEdge(Int128WritableComparable to, int weight) {
        this.to = to;
        this.weight = weight;
    }

    public Int128WritableComparable getTo() {
        return to;
    }

    public int getWeight() {
        return weight;
    }

    public void setTo(Int128WritableComparable to) {
        this.to = to;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        to.write(out);
        out.writeInt(weight);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        to.readFields(in);
        weight = in.readInt();
    }

    @Override
    public String toString() {
        return "DirectEdge{" +
                "to=" + to +
                ", weight=" + weight +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DirectEdge that = (DirectEdge) o;

        if (weight != that.weight) return false;
        if (to != null ? !to.equals(that.to) : that.to != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = to != null ? to.hashCode() : 0;
        result = 31 * result + weight;
        return result;
    }
}