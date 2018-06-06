package ru.ifmo.genetics.io.formats;

public class Sanger implements QualityFormat {
    public static Sanger instance = new Sanger();

    @Override
	public byte getPhred(char c) {
		if (c < 33 || c > 126) {
			throw new IllegalQualityValueException("Invalid quality code char: \"" + c + "\"" + " char code = " + ((int) c));
		}
		return (byte) (c - 33);
	}

    @Override
    public char getPhredChar(byte b) {
        if ((b < 0) || (b > 62)) {
            throw new RuntimeException("Invalid quality code byte: \"" + b);
        }
        return (char) (b + 33);
    }

	@Override
	public double getProbability(char c) {
		return Math.pow(10, getPhred(c) / -10.0);
	}

    @Override
    public String toExtString() {
        return "Sanger (Phred+33)";
    }
    public String toString() {
        return "Sanger";
    }
}
