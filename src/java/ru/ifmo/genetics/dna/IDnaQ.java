package ru.ifmo.genetics.dna;

public interface IDnaQ extends IDna, LightDnaQ {
    public void setPhred(int index, int phred);

    @Override
    public IDnaQ reverse();

    @Override
    public IDnaQ complement();
}
