package ru.ifmo.genetics.dna;

public class DnaQViewFromDna implements LightDnaQ {
    LightDna dna;
    byte phred;

    public DnaQViewFromDna(LightDna dna, byte phred) {
        this.dna = dna;
        this.phred = phred;
    }

    @Override
    public int length() {
        return dna.length();
    }

    @Override
    public byte nucAt(int index) {
        return dna.nucAt(index);
    }

    @Override
    public byte phredAt(int index) {
        return phred;
    }
}
