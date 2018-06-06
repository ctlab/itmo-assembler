package ru.ifmo.genetics.dna;

public class DnaQView extends AbstractDnaView<LightDnaQ> implements LightDnaQ {
    public DnaQView(LightDnaQ dna, int begin, int end) {
        this(dna, begin, end, false, false);
    }

    public DnaQView(LightDnaQ dna, int begin, int end, boolean rev, boolean compl) {
        super(dna, begin, end, rev, compl);
    }

    @Override
    public byte phredAt(int index) {
        return dna.phredAt(getInternalIndex(index));
    }

    public static DnaQView rcView(LightDnaQ dna) {
        return new DnaQView(dna, 0, dna.length(), true, true);
    }

    public static DnaQView complementView(LightDnaQ dna) {
        return new DnaQView(dna, 0, dna.length(), false, true);
    }
}
