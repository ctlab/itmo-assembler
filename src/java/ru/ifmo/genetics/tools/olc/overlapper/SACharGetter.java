package ru.ifmo.genetics.tools.olc.overlapper;

public class SACharGetter implements CharGetter {
    private OverlapTaskContext context;

    public SACharGetter(OverlapTaskContext context) {
        this.context = context;
    }

    @Override
    public int getChar(int indexInList, int posInStr) {
        long suffix = context.sa.get(indexInList);
        return context.fullString.get(suffix + posInStr);
    }
}
