package ru.ifmo.genetics.dna;

public interface IDna extends LightDna {
    public void setNuc(int index, int nuc);

    public IDna reverse();
    public IDna complement();
}