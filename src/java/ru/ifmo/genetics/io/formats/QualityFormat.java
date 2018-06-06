package ru.ifmo.genetics.io.formats;

public interface QualityFormat {
    public byte getPhred(char c);
    public char getPhredChar(byte b);
    public double getProbability(char c);

    public String toExtString();
}
