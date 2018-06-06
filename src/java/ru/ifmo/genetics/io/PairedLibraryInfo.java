package ru.ifmo.genetics.io;

public class PairedLibraryInfo {
    public final int minSize;
    public final int maxSize;
    public final int avgSize;
    public final int stdDev;

    public PairedLibraryInfo(int minSize, int maxSize, int avgSize, int stdDev) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.avgSize = avgSize;
        this.stdDev = stdDev;
    }

    @Override
    public String toString() {
        return "PairedLibraryInfo{" +
                "minSize=" + minSize +
                ", maxSize=" + maxSize +
                ", avgSize=" + avgSize +
                ", stdDev=" + stdDev +
                '}';
    }
}
