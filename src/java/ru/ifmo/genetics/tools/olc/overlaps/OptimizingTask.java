package ru.ifmo.genetics.tools.olc.overlaps;

import java.util.concurrent.atomic.AtomicReferenceArray;

public class OptimizingTask implements Runnable {
    Overlaps overlaps;
    final int begin;
    final int end;

    OverlapsList oldOverlaps;
    OverlapsList ends;
    OverlapsList.OverlapsSortTraits sortTraits;
    AtomicReferenceArray<OverlapsList> newOverlaps;


    public OptimizingTask(Overlaps overlaps, Overlaps newOverlaps, int begin, int end) {
        this.overlaps = overlaps;
        this.newOverlaps = newOverlaps.overlaps;
        this.begin = begin;
        this.end = end;
        sortTraits = new OverlapsList.OverlapsSortTraits();
        oldOverlaps = new OverlapsList(overlaps.withWeights);
        ends = new OverlapsList(false);
    }

    void optimize(int i) {
        overlaps.getForwardOverlaps(i, oldOverlaps);
        oldOverlaps.sort(sortTraits);

        ends.clear();

        for (int j = oldOverlaps.size() - 1; j >= 0; --j) {
            int eto = oldOverlaps.getTo(j);
            int eshift = oldOverlaps.getCenterShift(j);
            int eweight = oldOverlaps.getWeight(j);
            if (eto == i && eshift == 0) {
                continue;
            }

            for (int endIt = 0; endIt < ends.size(); ++endIt) {
                int prevto = ends.getTo(endIt);
                int prevshift = ends.getCenterShift(endIt);

                int seqShift = (eshift - prevshift);

                if (overlaps.containsOverlap(prevto, eto, seqShift)) {
                    ends.remove(endIt--);
                }
            }

            ends.add(eto, eshift, eweight);
        }

        newOverlaps.set(i, new OverlapsList(ends.size(), true));
        newOverlaps.get(i).addAllAddingErrorWeight(ends);
    }

    public void run() {
        for (int i = begin; i < end; ++i) {
            if (overlaps.getList(i) == null) {
                newOverlaps.set(i, null);
                continue;
            }
            optimize(i);
        }
    }
}
