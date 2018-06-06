package ru.ifmo.genetics.tools.olc.overlaps;

import ru.ifmo.genetics.io.readers.DedicatedLineReader;
import ru.ifmo.genetics.tools.olc.optimizer.RemovingCRTaskContext;

import java.io.IOException;
import java.nio.ByteBuffer;

public class RemovingCoveredReadsTask implements  Runnable {
    RemovingCRTaskContext context;
    ByteBuffer task;
    boolean rawOverlaps;
    DedicatedLineReader reader;

    public RemovingCoveredReadsTask(RemovingCRTaskContext context, ByteBuffer task, boolean rawOverlaps, DedicatedLineReader reader) {
        this.context = context;
        this.task = task;
        this.rawOverlaps = rawOverlaps;
        this.reader = reader;
    }


    private void checkCovers(int from, int to, int shift) {
        if (from == to) {
            assert shift != 0;
            return;
        }

        int fromLen = context.readLen[from];
        int toLen = context.readLen[to];

        int beginShift = Overlaps.centerShiftToBeginShiftUsingLen(
                fromLen, toLen, shift);

        if ((beginShift < 0) ||
                (beginShift == 0 && 
                    (toLen > fromLen || 
                        (toLen == fromLen && from > to)))) {
            checkCovers(to, from, -shift);
            return;
        }

        // checking FROM covers TO
        if (fromLen >= beginShift + toLen) {
            context.removingRead[to] = true;
        }
    }


    @Override
    public void run() {
        boolean withWeights = !rawOverlaps;
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
                    // already removed
                    context.readRemoved[to] = true;
                    continue;
                }

                checkCovers(from, to, shift);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            reader.returnBuffer(task);
        }
    }
}
