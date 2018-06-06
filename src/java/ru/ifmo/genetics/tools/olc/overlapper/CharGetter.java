package ru.ifmo.genetics.tools.olc.overlapper;

public interface CharGetter {
    /**
     * Gets a char at <code>posInStr</code> position in string,
     * that is located in some list of strings at <code>indexInList</code> position.
     */
    int getChar(int indexInList, int posInStr);
}
