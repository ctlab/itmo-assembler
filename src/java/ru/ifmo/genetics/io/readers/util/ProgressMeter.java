package ru.ifmo.genetics.io.readers.util;

public class ProgressMeter {
    public static float getProgress(long start, long pos, long end) {
        if (start == end) {
            return 0.0f;
        } else {
            return Math.min(1.0f, (pos - start) / (float) (end - start));
        }
    }
}
