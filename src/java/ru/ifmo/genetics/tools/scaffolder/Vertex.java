package ru.ifmo.genetics.tools.scaffolder;

import java.util.ArrayList;

public class Vertex {
	public Vertex(Contig contigInfo) {
		info = contigInfo;
		edges = new ArrayList<Edge>();
	}

	public int distTo(Vertex v) {
		return Math.abs(pos - v.pos) - (info.len + v.info.len) / 2;
	}

	public ArrayList<Edge> edges;
	boolean u;
	int d;
	public Contig info;
	Edge prev;
	int color;
	double tau;
	public Scaffold s;
	int or = 0;
	int pos;

	public int realDistTo(Vertex v) {
		return info.realDistTo(v.info);
	}

	public double getCover() {
		return info.getCover();
	}

	public boolean isOnBorder() {
		return isFirst() || isLast();
	}

	public boolean isFirst() {
		return s.first() == this;
	}

	public boolean isLast() {
		return s.last() == this;
	}

	public boolean isSecondToBorder() {
		return isSecondToFirst() || isSecondToLast();
	}

	public boolean isSecondToFirst() {
		return s.size() > 1 && s.secondToFirst() == this;
	}

	public boolean isSecondToLast() {
		return s.size() > 1 && s.secondToLast() == this;
	}

	public Vertex copy() {
		Vertex v = new Vertex(this.info);
		for (Edge e : edges) {
			Edge en = new Edge(v, e.v2, e.len, e.err);
			en.pairs = e.pairs;
			v.edges.add(en);
			en.v2.edges.add(en.rev());
		}
		return v;
	}

	public int getScaffoldIndex() {
		return s.getIndex(this);
	}

	public String getOrientation() {
		return or == 0 ? "TBD" : or < 0 ? "EB" : "BE";
	}

}