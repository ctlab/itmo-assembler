package ru.ifmo.genetics.dna;

public class MutableDnaView extends AbstractLightDna {
    public LightDna dna;
    public int offset;
    public int length;

    public void set(LightDna dna, int offset, int length) {
        this.dna = dna;
        this.offset = offset;
        this.length = Math.min(length, dna.length() - offset);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public byte nucAt(int index) {
        return dna.nucAt(index + offset);
    }
}
