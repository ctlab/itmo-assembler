package ru.ifmo.genetics.io.sources;

import org.jetbrains.annotations.NotNull;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.io.PairedLibraryInfo;

public class ZippingPairedLibrary<T extends LightDna> extends PairSource<T> implements PairedLibrary<T> {
    private final String name;
    private final PairedLibraryInfo info;

    public ZippingPairedLibrary(NamedSource<? extends T> source1, NamedSource<? extends T> source2, PairedLibraryInfo info) {
        super(source1, source2);
        this.info = info;
        String name1 = source1.name();
        String name2 = source2.name();
        // :ToDo: more intellectual dataset name?
        name = name1 + "+" + name2;
    }


    @Override
    public String name() {
        return name;
    }
    public static <T extends LightDna> ZippingPairedLibrary<T> create(
            NamedSource<? extends T> source1,
            NamedSource<? extends T> source2,
            PairedLibraryInfo info) {
        return new ZippingPairedLibrary<T>(source1, source2, info);
    }

    @NotNull
    @Override
    public PairedLibraryInfo info() {
        return info;
    }
}
