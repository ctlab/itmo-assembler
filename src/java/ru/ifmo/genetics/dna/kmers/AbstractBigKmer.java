package ru.ifmo.genetics.dna.kmers;

import ru.ifmo.genetics.dna.AbstractLightDna;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.DnaView;
import ru.ifmo.genetics.dna.kmers.BigKmer;

public abstract class AbstractBigKmer extends AbstractLightDna implements BigKmer {
    @Override
    public long fwLongHashCode() {
        return DnaTools.longHashCode(this);
    }

    @Override
    public long rcLongHashCode() {
        return DnaTools.longHashCode(DnaView.rcView(this));
    }

    @Override
    public long longHashCode() {
        return fwLongHashCode();
    }

    @Override
    public long biLongHashCode() {
        return Math.min(fwLongHashCode(), rcLongHashCode());
    }

    @Override
    public long toLong() {
        return biLongHashCode();
    }

    public byte firstNuc() {
        return nucAt(0);
    }

    public byte lastNuc() {
        return nucAt(length() - 1);
    }
}
