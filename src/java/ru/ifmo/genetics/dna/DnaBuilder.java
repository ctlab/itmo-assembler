package ru.ifmo.genetics.dna;

public class DnaBuilder {
    NucArray data;
    int length;

    public DnaBuilder() {
        this(16);
    }

    public DnaBuilder(int capacity) {
        data = new NucArray(capacity);
    }

    public DnaBuilder(LightDna dna) {
        this(dna, 16);
    }

    public DnaBuilder(LightDna dna, int additionalCapacity) {
        this(dna.length() + additionalCapacity);
        for (int i = 0; i < dna.length(); i++) {
            append(dna.nucAt(i));
        }
    }

    

    public int length() {
        return length;
    }

    private void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > data.length) {
            data = data.copy(2 * data.length + 1);
        }
    }

    public void append(byte nuc) {
        ensureCapacity(length + 1);
        data.set(length++, nuc);
    }
    
    public void removeLast() {
        length--;
        data.set(length, (byte)0);
    }

    public LightDna build() {
        Dna build = new Dna(data, 0, length);
        data = null;
        return build;
    }
    
    public LightDna snapshot() {
        return new Dna(new Dna(data, 0, length));
    }
}
