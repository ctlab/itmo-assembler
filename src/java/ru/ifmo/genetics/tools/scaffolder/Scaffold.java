package ru.ifmo.genetics.tools.scaffolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math.MathException;

public class Scaffold {
	public int id;
	public List<Vertex> vertecies = new ArrayList<Vertex>();
	ScafEdge prev;
	@SuppressWarnings("unchecked")
	public
	ArrayList<ScafEdge>[] edges = new ArrayList[2];
	int d;
	boolean u;
	{
		for (int i = 0; i < edges.length; i++) {
			edges[i] = new ArrayList<ScafEdge>();
		}
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof Scaffold) && id == ((Scaffold) o).id;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < vertecies.size(); i++) {
			Vertex v = vertecies.get(i);
			int dist = 0;
			if (i + 1 < vertecies.size()) {
				Vertex v2 = vertecies.get(i + 1);
				dist = v.distTo(v2);
			}
			sb.append(v.info.name + "\t" + v.getOrientation() + "\t"
					+ v.info.len + "\t" + dist + "\t" + v.info.id + "\t"
					+ v.info.realPos + "\t" + v.info.realRev + "\t"
					+ v.info.cover + "\t" + (1.0 * v.info.cover / v.info.len)
					+ "\n");
		}
		return sb.toString();
	}

	public double getLikelihood() throws MathException {
		double qs = 0;
		// for (int i = 0; i < vertecies.size(); i++) {
		// Vertex v1 = vertecies.get(i);
		// for (int j = 0; j < i; j++) {
		// Vertex v2 = vertecies.get(j);
		// double q = DistanceFinder
		// .getProbabilityThatAtLeastOneMatepairMatches(
		// v1.distTo(v2), v1.info.len, v2.info.len);
		// qs += q;
		// }
		// }
		int reads = 0;
		for (Vertex v : vertecies) {
			for (Edge e : v.edges) {
				if (!vertecies.contains(e.v2) || e.v1.info.id > e.v2.info.id) {
					continue;
				}
				reads += e.pairs.length;
			}
		}
		double ans = 0;
		for (Vertex v : vertecies) {
			for (Edge e : v.edges) {
				if (!vertecies.contains(e.v2) || e.v1.info.id > e.v2.info.id) {
					continue;
				}
				double p = (e.dSq * e.len + e.dLin) * e.len + e.dCon;
				// double q = DistanceFinder
				// .getProbabilityThatAtLeastOneMatepairMatches(
				// e.v1.distTo(e.v2), e.v1.info.len, e.v2.info.len)
				// / qs;
				ans += p;
				// ans += (reads - e.pairs.length) * Math.log(1 - q);
			}
		}
		ans += (Data.allReads() - reads) * Math.log(1 - qs);
		return ans;
	}

	public int getSum() {
		int sum = 0;
		for (Vertex v : vertecies) {
			sum += v.info.len;
		}
		return sum;
	}

	public void bindVertecies() {
		for (Vertex v : vertecies) {
			v.s = this;
		}
	}

	public Vertex first() {
		return vertecies.get(0);
	}

	public Vertex last() {
		return vertecies.get(vertecies.size() - 1);
	}

	public Vertex secondToFirst() {
		return vertecies.get(1);
	}

	public Vertex secondToLast() {
		return vertecies.get(vertecies.size() - 2);
	}

	public int size() {
		return vertecies.size();
	}

	public void swapFirst() {
		ScaffoldBuilder.win.println("was (swap first)");
		ScaffoldBuilder.win.println(this);
		ScaffoldBuilder.win.flush();
		Vertex v1 = vertecies.get(0);
		Vertex v2 = vertecies.get(1);
		vertecies.set(0, v2);
		vertecies.set(1, v1);
		ScaffoldBuilder.win.println("now");
		ScaffoldBuilder.win.println(this);
		ScaffoldBuilder.win.flush();
	}

	public void swapLast() {
		ScaffoldBuilder.win.println("was (swap last)");
		ScaffoldBuilder.win.println(this);
		Vertex v1 = vertecies.get(vertecies.size() - 1);
		Vertex v2 = vertecies.get(vertecies.size() - 2);
		vertecies.set(vertecies.size() - 1, v2);
		vertecies.set(vertecies.size() - 2, v1);
		ScaffoldBuilder.win.println("now");
		ScaffoldBuilder.win.println(this);
	}

	public Vertex pollFirst() {
		return vertecies.remove(0);
	}

	public Vertex pollLast() {
		return vertecies.remove(vertecies.size() - 1);
	}

	public void reverse() {
		ArrayList<ScafEdge> tmp = edges[0];
		edges[0] = edges[1];
		edges[1] = tmp;
		Collections.reverse(vertecies);
	}

	public void clear() {
		vertecies.clear();
	}

	public int getIndex(Vertex vertex) {
		return vertecies.indexOf(vertex);
	}

	public void addFirst(Vertex v) {
		vertecies.add(0, v);
	}

	public void addLast(Vertex v) {
		vertecies.add(vertecies.size(), v);
	}

	public void insertContig(int pos, Vertex v) {
		vertecies.add(pos, v);
	}

	public boolean canSkipFirst() {
		if (size() == 1) {
			return false;
		}
		if (size() == 2) {
			return true;
		}
		if (size() == 3) {
			return Math.min(first().info.len, secondToFirst().info.len) < Data.getMaxInsertSize()
					&& Math.min(first().info.len, secondToFirst().info.len) > Data.getMaxInsertSize() / 6
					&& first().info.len < last().info.len;
		}
		return Math.min(first().info.len, secondToFirst().info.len) < Data.getMaxInsertSize()
				&& Math.min(first().info.len, secondToFirst().info.len) > Data.getMaxInsertSize() / 6;
	}

	public boolean canSkipLast() {
		if (size() == 1) {
			return false;
		}
		if (size() == 2) {
			return true;
		}
		if (size() == 3) {
			return Math.min(last().info.len, secondToLast().info.len) < Data.getMaxInsertSize()
					&& Math.min(last().info.len, secondToLast().info.len) > Data.getMaxInsertSize() / 6
					&& first().info.len > last().info.len;
		}
		return Math.min(last().info.len, secondToLast().info.len) < Data.getMaxInsertSize()
				&& Math.min(last().info.len, secondToLast().info.len) > Data.getMaxInsertSize() / 6;
	}

	// private void realign() {
	// if (vertecies.isEmpty()) {
	// return;
	// }
	// ArrayDeque<Vertex> tmp = new ArrayDeque<>();
	// Vertex v = vertecies.pollFirst();
	// v.pos = 0;
	// }
}
