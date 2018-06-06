package ru.ifmo.genetics.dna;

public class AbstractDnaView<D extends LightDna> extends AbstractLightDna {
    protected final D dna;
    protected final int begin;
    protected final int end;
    protected final boolean rev;
    protected final boolean compl;

    protected AbstractDnaView(D dna, int begin, int end, boolean rev, boolean compl) {
        this.dna = dna;
        this.begin = begin;
        this.end = end;
        this.rev = rev;
        this.compl = compl;
    }

    protected int getInternalIndex(int index) {
        return (rev ? end - index - 1 : begin + index);
    }

    @Override
    public int length() {
        return end - begin;
    }

    @Override
    public byte nucAt(int index) {
        byte nuc = dna.nucAt(getInternalIndex(index));
        return (byte) (compl ? nuc ^ 3 : nuc);
    }
}
