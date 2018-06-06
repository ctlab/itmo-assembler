package ru.ifmo.genetics.tools.scaffolder;
public class InfoPair implements Comparable<InfoPair> {
	Contig s1, s2;

	public InfoPair(String s1, String s2) {
		this.s1 = Contig.getInfo(s1);
		this.s2 = Contig.getInfo(s2);
	}

	public InfoPair(Contig s1, Contig s2) {
		this.s1 = s1;
		this.s2 = s2;
	}

	public InfoPair reverse() {
		return new InfoPair(s2, s1);
	}

	@Override
	public int hashCode() {
		return s1.hashCode() * 997 + s2.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof InfoPair) && s1.equals(((InfoPair) o).s1)
				&& s2.equals(((InfoPair) o).s2);
	}

	@Override
	public int compareTo(InfoPair p) {
		if (s1.equals(p.s1)) {
			return s2.name.compareTo(p.s2.name);
		}
		return s1.name.compareTo(p.s1.name);
	}
}