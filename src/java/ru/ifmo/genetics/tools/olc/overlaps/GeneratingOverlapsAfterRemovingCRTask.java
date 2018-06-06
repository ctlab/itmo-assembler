package ru.ifmo.genetics.tools.olc.overlaps;

import ru.ifmo.genetics.io.readers.DedicatedLineReader;
import ru.ifmo.genetics.io.writers.DedicatedLineWriter;
import ru.ifmo.genetics.tools.olc.optimizer.RemovingCRTaskContext;

import java.io.IOException;
import java.nio.ByteBuffer;

public class GeneratingOverlapsAfterRemovingCRTask implements  Runnable {
    RemovingCRTaskContext context;
    ByteBuffer task;
    boolean rawOverlaps;
    DedicatedLineReader reader;
    DedicatedLineWriter writer;

    private long overlapsAll = 0, overlapsOK = 0;
    

    public GeneratingOverlapsAfterRemovingCRTask(RemovingCRTaskContext context, ByteBuffer task, boolean rawOverlaps,
                                                 DedicatedLineReader reader, DedicatedLineWriter writer) {
        this.context = context;
        this.task = task;
        this.rawOverlaps = rawOverlaps;
        this.reader = reader;
        this.writer = writer;
    }


    @Override
    public void run() {
        boolean withWeights = !rawOverlaps;
        ByteBuffer output = null;
        try {
            output = writer.getBuffer();
            String overlap = null;
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

                if (to == -1) {     // already removed
                    assert context.readRemoved[to];
                    if (withWeights) {
                        overlap = from + " -1 0 0";
                    } else {
                        overlap = from + " -1 0";
                    }
                } else {
                    overlapsAll++;
                    if (!context.removingRead[from] && !context.readRemoved[from] &&
                            !context.removingRead[to] && !context.readRemoved[to]) {
                        overlap = from + " " + to + " " + shift;
                        if (withWeights) {
                            overlap += " " + weight;
                        }
                        overlapsOK++;
                    }
                }

                if (overlap != null) {
                    output = writer.writeLine(output, overlap);
                    overlap = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            reader.returnBuffer(task);
            try {
                writer.writeBuffer(output);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            context.overlapsAll.addAndGet(overlapsAll);
            context.overlapsOK.addAndGet(overlapsOK);
        }
    }
}
