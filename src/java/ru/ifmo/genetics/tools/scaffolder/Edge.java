package ru.ifmo.genetics.tools.scaffolder;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Edge {

	@Override
	public String toString() {
		return len + " (" + realDist() + ")";
	}

	public Edge(Vertex v1, Vertex v2, int len, int err) {
		this.v1 = v1;
		this.v2 = v2;
		this.len = len;
		this.err = err;
	}

	public int realDist() {
		return v1.realDistTo(v2);
	}

	public int[] getD1() {
		return getD1(false);
	}

	public int[] getD2() {
		return getD2(false);
	}

	public int[] getD1(boolean r) {
		int[] d1 = new int[pairs.length];
		for (int i = 0; i < d1.length; i++) {
			d1[i] = pairs[i].getD1(r);
		}
		return d1;
	}

	public int[] getD2(boolean r) {
		int[] d2 = new int[pairs.length];
		for (int i = 0; i < d2.length; i++) {
			d2[i] = pairs[i].getD2(r);
		}
		return d2;
	}

	// public void setReverse(int rev) {
	// reverse = rev;
	// rev().reverse = rev;
	// }

	public void setReads(MatePair[] pairs) {
		this.pairs = pairs.clone();
		rev();
		dSq = DistanceFinder.dSq(pairs);
		dLin = DistanceFinder.dLin(pairs);
		dCon = DistanceFinder.dCon(pairs);
		cnt = new int[Data.libraries.length];
		r.cnt = cnt;
		for (MatePair p : pairs) {
			cnt[p.lib.id]++;
		}
		r.dSq = dSq;
		r.dLin = dLin;
		r.dCon = dCon;

		// int end1 = 0;
		// int end2 = 0;
		// for (Pair p : pairs) {
		// if (p.d1.pos > v1.info.len / 2) {
		// end1++;
		// }
		// if (p.d2.pos > v2.info.len / 2) {
		// end2++;
		// }
		// }
		// endFirst = end1 > pairs.length / 2 ? 1 : -1;
		// endSecond = end2 > pairs.length / 2 ? 1 : -1;
	}

	public void setErr(int err) {
		this.err = err;
		if (r == null) {
			rev();
		}
		r.err = err;
	}

	public void setLen(int mpd) {
		this.len = mpd;
		if (r == null) {
			rev();
		}
		r.len = mpd;
	}

	public Edge rev() {
		if (r != null) {
			return r;
		}
		Edge e = new Edge(v2, v1, len, err);
		e.r = this;
		this.r = e;
		r.pairs = new MatePair[pairs.length];
		for (int i = 0; i < r.pairs.length; i++) {
			r.pairs[i] = new MatePair(pairs[i].d2, pairs[i].d1, pairs[i].lib);
		}
		r.ghost = ghost;
		return e;
	}

	public Vertex v1;
	public Vertex v2;
	public int len;
	int err;
	Edge r;
	// int reverse;
	boolean good;
	public MatePair[] pairs;
	int[] cnt;
	public double dSq;
	public double dLin;
	public double dCon;
	boolean ghost;
	double tau;

	// double exp;
	// boolean rev1;
	// boolean rev2;

	public boolean isReverse() {
		int rev = 0;
		for (MatePair p : pairs) {
			if (p.d1.isReverseComplimented() == p.d2.isReverseComplimented()) {
				rev++;
			}
		}
		return 2 * rev > pairs.length;
	}

	public String getStyle() {
		return ghost ? "dashed"
				: Math.abs(v1.info.id - v2.info.id) == 1 ? "bold" : "solid";
	}

	public int getAvLen() {
		long av_len = 0;
		for (MatePair p : pairs) {
			av_len += p.lib.insertSize - 2 * p.lib.readLength - p.getD1()
					- p.getD2();
		}
		av_len /= pairs.length;
		return (int) av_len;
	}

	public boolean isStrange() throws FileNotFoundException {
		return false;
	}

	static PrintWriter edges, ge;

	public void setGood(boolean b) {
		good = b;
		rev().good = b;
	}
}