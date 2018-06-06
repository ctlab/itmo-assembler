package ru.ifmo.genetics.tools.olc.overlaps;

import ru.ifmo.genetics.io.readers.DedicatedLineReader;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AddingTask implements  Runnable {
    Overlaps overlaps;
    ByteBuffer task;
    boolean rawAdding;
    DedicatedLineReader reader;

    public AddingTask(Overlaps overlaps, ByteBuffer task, boolean rawAdding, DedicatedLineReader reader) {
        this.overlaps = overlaps;
        this.task = task;
        this.rawAdding = rawAdding;
        this.reader = reader;
    }

    @Override
    public void run() {
        boolean withWeights = !rawAdding;
        try {
            while (true) {
                int from = DedicatedLineReader.readInteger(task);
                if (from == -1) {
                    break;
                }
                int to = DedicatedLineReader.readInteger(task);
                int shift = DedicatedLineReader.readInteger(task);
                int weight = -1;
                if (withWeights) {
                    weight = DedicatedLineReader.readInteger(task);
                }
                
                if (to == -1) {
                    overlaps.markReadRemoved(from);
                    continue;
                }

                if (rawAdding) {
                    overlaps.addRawOverlapWithSync(from, to, shift);
                } else {
                    overlaps.addOverlapWithSync(from, to, shift, weight);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            reader.returnBuffer(task);
        }
    }
}
