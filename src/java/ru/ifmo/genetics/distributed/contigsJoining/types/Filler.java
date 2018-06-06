package ru.ifmo.genetics.distributed.contigsJoining.types;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Filler implements Writable{
    public int distance;
    public int weight;
    public DnaWritable sequence = new DnaWritable();

    public Filler() {
    }

    public Filler(int distance, int weight, DnaWritable sequence) {
        this.distance = distance;
        this.weight = weight;
        this.sequence = sequence;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(distance);
        out.writeInt(weight);
        sequence.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        distance = in.readInt();
        weight = in.readInt();
        sequence.readFields(in);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Filler filler = (Filler) o;

        if (distance != filler.distance) return false;
        if (weight != filler.weight) return false;
        if (sequence != null ? !sequence.equals(filler.sequence) : filler.sequence != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = distance;
        result = 31 * result + weight;
        result = 31 * result + (sequence != null ? sequence.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {

        return distance + " " + weight + " " + (distance > 0 ? sequence.toString() : "");
    }

    public void parse(String fillerString) {
        String[] fillerParts = fillerString.split(" ");
        distance = Integer.parseInt(fillerParts[0]);

        weight = Integer.parseInt(fillerParts[1]);
        if (fillerParts.length > 2) {
            sequence.set(new Text(fillerParts[2]));
        }
    }
}
