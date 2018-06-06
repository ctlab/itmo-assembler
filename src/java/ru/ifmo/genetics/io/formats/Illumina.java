package ru.ifmo.genetics.io.formats;

public class Illumina implements QualityFormat {
    public static Illumina instance = new Illumina();

    @Override
	public byte getPhred(char c) {
		if (c < 64 || c > 126) {
            throw new IllegalQualityValueException("Invalid quality code char: \"" + c + "\"" + " char code = " + ((int) c));
		}
		return (byte) (c - 64);
	}

    @Override
    public char getPhredChar(byte b) {
        if ((b < 0) || (b > 62)) {
            throw new RuntimeException("Invalid quality code byte: " + b);
        }
        return (char) (b + 64);
    }

    public double getProbability(byte b) {
        if (b == 2) {
            return 0.75;
        }
        return Math.pow(10, b / -10.0);
    }

	@Override
	public double getProbability(char c) {
		return getProbability(getPhred(c));
	}

    @Override
    public String toExtString() {
        return "Illumina (Phred+64)";
    }
    public String toString() {
        return "Illumina";
    }
}
