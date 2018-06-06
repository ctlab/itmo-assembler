package ru.ifmo.genetics.dna;

public class ConcatenatingDnaView extends AbstractLightDna {
    LightDna dna1;
    LightDna dna2;

    public ConcatenatingDnaView(LightDna dna1, LightDna dna2) {
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
    public String toString() {
        return DnaTools.toString(this);
    }
}
