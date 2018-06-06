package ru.ifmo.genetics.tools.scaffolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Contig {

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Contig)) {
			return false;
		}
		Contig c = (Contig) o;
		return c.name.equals(name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public Contig(int id, String name) {
		this.id = id;
		this.name = transform(name);
		v = new Vertex(this);
		realPos = new ArrayList<Integer>();
		realRev = new ArrayList<Boolean>();
	}

	public boolean isReversed() {
		return reversed;
	}

	public int len;
	public int id;
	int pos;
	public List<Integer> realPos;
	public List<Boolean> realRev;
	public final String name;
	String seq;
	boolean reversed;
	public int cover;
	Vertex v;

	static private Map<String, Contig> contigInfo = new TreeMap<String, Contig>();

	static public Map<String, Contig> getInfo() {
		return contigInfo;
	}

	static public Contig getInfo(String s) {
		if (!contigInfo.containsKey(s)) {
			contigInfo.put(s, new Contig(contigInfo.size(), s));
		}
		return contigInfo.get(s);
	}

	static public String transform(String name) {
		return name.replace(' ', '_');
	}

	public int realDistTo(Contig info) {
		int min = Integer.MAX_VALUE / 2;
		for (int x : realPos) {
			for (int y : info.realPos) {
				min = Math.min(min, Math.abs(x - y));
				min = Math.min(min, Math.abs(Data.dnaLength + x - y));
				min = Math.min(min, Math.abs(Data.dnaLength + y - x));
			}
		}
		return min - (len + info.len) / 2;
	}

	public double getCover() {
		return 1.0 * cover / len;
	}

}