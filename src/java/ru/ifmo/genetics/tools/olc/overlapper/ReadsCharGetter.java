package ru.ifmo.genetics.tools.olc.overlapper;

public class ReadsCharGetter implements CharGetter {
    private OverlapTaskContext context;

    ReadsCharGetter(OverlapTaskContext context) {
        this.context = context;
    }

    @Override
    public int getChar(int indexInList, int posInStr) {
        int read = context.readsInBucket.get(indexInList);
        long readBegin = context.readBegin[read];
        return context.fullString.get(readBegin + posInStr);
    }
}
