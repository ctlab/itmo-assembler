package ru.ifmo.genetics.dna;

public interface LightDnaQ extends LightDna {
    byte nucAt(int index);

    byte phredAt(int index);
}
