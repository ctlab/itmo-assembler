package ru.ifmo.genetics.dna;

public class DnaView extends AbstractDnaView<LightDna> {
    public DnaView(LightDna dna, int begin, int end) {
        this(dna, begin, end, false, false);
    }

    public DnaView(LightDna dna, int begin, int end, boolean rev, boolean compl) {
        super(dna, begin, end, rev, compl);
    }

    public static DnaView rcView(LightDna dna) {
        return new DnaView(dna, 0, dna.length(), true, true);
    }

}
