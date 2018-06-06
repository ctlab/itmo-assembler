package ru.ifmo.genetics.dna;

public abstract class AbstractDna implements IDna {
    protected int length;
    protected final static int sz_shift = 2;
    protected final static int sz = 1 << sz_shift;
    protected final static int sz_mod_mask = sz - 1; 


    @Override
    public int length() {
        return length;
    }

    @Override
    public abstract byte nucAt(int index);

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IDna) {
            IDna that = (IDna) obj;
            if (length != that.length())
                return false;
            for (int i = 0; i < length; i++) {
                if (nucAt(i) != that.nucAt(i))
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        char[] c = new char[length];
        for (int i = 0; i < length; i++) {
            c[i] = DnaTools.toChar(nucAt(i));
        }
        return new String(c);
    }
}
