package ru.ifmo.genetics.dna;

public class ConcatenatingDnaQView implements LightDnaQ {
    LightDnaQ dna1;
    LightDnaQ dna2;

    public ConcatenatingDnaQView(LightDnaQ dna1, LightDnaQ dna2) {
        this.dna1 = dna1;
        this.dna2 = dna2;
    }

    @Override
    public int length() {
        return dna1.length() + dna2.length();
    }

    @Override
    public byte nucAt(int index) {
        assert index < length() : index + " " + length();
        if (index < dna1.length()) {
            return dna1.nucAt(index);
        }
        return dna2.nucAt(index - dna1.length());
    }

    @Override
    public byte phredAt(int index) {
        assert index < length() : index + " " + length();
        if (index < dna1.length()) {
            return dna1.phredAt(index);
        }
        return dna2.phredAt(index - dna1.length());
    }

    @Override
    public String toString() {
        return DnaTools.toString(this);
    }
}
