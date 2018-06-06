package ru.ifmo.genetics.tools.olc.overlaps;

public class SortingTask implements Runnable {

    private Overlaps overlaps;
    private int begin;
    private int end;

    public SortingTask(Overlaps overlaps, int begin, int end) {
        this.overlaps = overlaps;
        this.begin = begin;
        this.end = end;
    }

    @Override
    public void run() {
        OverlapsList.OverlapsSortTraits sortTraits = new OverlapsList.OverlapsSortTraits();
        for (int i = begin; i < end; ++i) {
            if (overlaps.getList(i) != null) {
                overlaps.getList(i).sort(sortTraits);
            }
        }
    }
}
