package ru.ifmo.genetics.tools.scaffolder;

public class MatePair {

	@Override
	public String toString() {
		return d1 + "\t" + d2;
	}

	public MatePair(SAMAlignment a, SAMAlignment b, Library library) {
		d1 = a;
		d2 = b;
		lib = library;
	}

	public boolean isReverse() {
		return d1.isReverseComplimented() == d2.isReverseComplimented();
	}

	public int getD1() {
		return getD1(false);
	}

	public int getD2() {
		return getD2(false);
	}

	SAMAlignment d1, d2;
	Library lib;

	public int getD1(boolean r) {
		return (d1.isReverseComplimented() ^ r ? (d1.pos - 1) : Contig
				.getInfo(d1.rname).len - (d1.pos - 1) - lib.readLength);
	}

	public int getD2(boolean r) {
		return (d2.isReverseComplimented() ^ r ? (d2.pos - 1) : Contig
				.getInfo(d2.rname).len - (d2.pos - 1) - lib.readLength);
	}

}