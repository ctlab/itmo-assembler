package ru.ifmo.genetics.io.sources;

import org.jetbrains.annotations.NotNull;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.io.PairedLibraryInfo;
import ru.ifmo.genetics.utils.pairs.UniPair;

public interface PairedLibrary<T extends LightDna> extends NamedSource<UniPair<T>> {
    @NotNull
    public PairedLibraryInfo info();
}
