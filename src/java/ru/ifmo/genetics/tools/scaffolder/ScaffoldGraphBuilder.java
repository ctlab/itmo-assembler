package ru.ifmo.genetics.tools.scaffolder;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

public class ScaffoldGraphBuilder {
	public static void buildScafEdges(List<Scaffold> ans)
			throws FileNotFoundException {
		System.err.println("\tbuilding scafedges");
		rescale(ans);

		for (Scaffold s : ans) {
			s.edges[0].clear();
			s.edges[1].clear();
		}

		for (Scaffold s : ans) {
			for (Edge e : s.first().edges) {
				if (e.v1.info.id > e.v2.info.id || e.v1.s == e.v2.s) {
					continue;
				}
				if (e.v2.isFirst() || e.v2.isSecondToFirst()
						&& e.v2.s.canSkipFirst()) {
					ScafEdge se = new ScafEdge();
					se.edge = e;
					se.x = s;
					se.y = e.v2.s;
					se.ex = 0;
					se.ey = 0;
					s.edges[se.ex].add(se);
					se.y.edges[se.ey].add(se.rev());
				}
				if ((e.v2.isLast() || e.v2.isSecondToLast()
						&& e.v2.s.canSkipLast())
						&& e.v2.s.size() > 1) {
					ScafEdge se = new ScafEdge();
					se.edge = e;
					se.x = s;
					se.y = e.v2.s;
					se.ex = 0;
					se.ey = 1;
					s.edges[se.ex].add(se);
					se.y.edges[se.ey].add(se.rev());
				}
			}
			if (s.size() < 2) {
				continue;
			}
			for (Edge e : s.last().edges) {
				if (e.v1.info.id > e.v2.info.id || e.v1.s == e.v2.s) {
					continue;
				}
				if (e.v2.isFirst() || e.v2.isSecondToFirst()
						&& e.v2.s.canSkipFirst()) {
					ScafEdge se = new ScafEdge();
					se.edge = e;
					se.x = s;
					se.y = e.v2.s;
					se.ex = 1;
					se.ey = 0;
					s.edges[se.ex].add(se);
					se.y.edges[se.ey].add(se.rev());
				}
				if ((e.v2.isLast() || e.v2.isSecondToLast()
						&& e.v2.s.canSkipLast())
						&& e.v2.s.size() > 1) {
					ScafEdge se = new ScafEdge();
					se.edge = e;
					se.x = s;
					se.y = e.v2.s;
					se.ex = 1;
					se.ey = 1;
					s.edges[se.ex].add(se);
					se.y.edges[se.ey].add(se.rev());
				}
			}
			for (Edge e : s.secondToFirst().edges) {
				if (e.v1.info.id > e.v2.info.id || !s.canSkipFirst()
						|| e.v1.s == e.v2.s) {
					continue;
				}
				if (e.v2.isFirst() || e.v2.isSecondToFirst()
						&& e.v2.s.canSkipFirst()) {
					ScafEdge se = new ScafEdge();
					se.edge = e;
					se.x = s;
					se.y = e.v2.s;
					se.ex = 0;
					se.ey = 0;
					s.edges[se.ex].add(se);
					se.y.edges[se.ey].add(se.rev());
				}
				if ((e.v2.isLast() || e.v2.isSecondToLast()
						&& e.v2.s.canSkipLast())
						&& e.v2.s.size() > 1) {
					ScafEdge se = new ScafEdge();
					se.edge = e;
					se.x = s;
					se.y = e.v2.s;
					se.ex = 0;
					se.ey = 1;
					s.edges[se.ex].add(se);
					se.y.edges[se.ey].add(se.rev());
				}
			}
			for (Edge e : s.secondToLast().edges) {
				if (e.v1.info.id > e.v2.info.id || !s.canSkipLast()
						|| e.v1.s == e.v2.s) {
					continue;
				}
				if (e.v2.isFirst() || e.v2.isSecondToFirst()
						&& e.v2.s.canSkipFirst()) {
					ScafEdge se = new ScafEdge();
					se.edge = e;
					se.x = s;
					se.y = e.v2.s;
					se.ex = 1;
					se.ey = 0;
					s.edges[se.ex].add(se);
					se.y.edges[se.ey].add(se.rev());
				}
				if ((e.v2.isLast() || e.v2.isSecondToLast()
						&& e.v2.s.canSkipLast())
						&& e.v2.s.size() > 1) {
					ScafEdge se = new ScafEdge();
					se.edge = e;
					se.x = s;
					se.y = e.v2.s;
					se.ex = 1;
					se.ey = 1;
					s.edges[se.ex].add(se);
					se.y.edges[se.ey].add(se.rev());
				}
			}
		}
		int[][] inDeg = new int[ans.size()][2];
		for (Scaffold s : ans) {
			if (s.vertecies.size() > 1) {
				continue;
			}
			if (s.edges[0].size() == 1) {
				for (ScafEdge se : s.edges[0]) {
					inDeg[se.y.id][se.ey]++;
				}
			}
			if (s.edges[0].size() == 2) {
				for (ScafEdge se : s.edges[0]) {
					inDeg[se.y.id][se.ey]++;
				}
			}
		}

		int good = 0;
		int bad = 0;
		int good2 = 0;
		int bad2 = 0;
		for (Scaffold s : ans) {
			if (s.vertecies.size() > 1) {
				continue;
			}
			if (s.edges[0].size() == 1) {
				for (ScafEdge se : s.edges[0]) {
					if (inDeg[se.y.id][se.ey] > 1 || se.y.vertecies.size() < 2) {
						continue;
					}
					if (se.edge.realDist() > Data.getMaxInsertSize() + 3
							* Data.getMaxDeviation()) {
						bad++;
						ScaffoldBuilder.win.println("bad:\t"
								+ se.edge.v1.info.id + "\t"
								+ se.edge.v2.info.id + "\t"
								+ se.edge.v1.info.realPos + "\t"
								+ se.edge.v2.info.realPos + "\t"
								+ se.edge.v1.info.len + "\t"
								+ se.edge.v2.info.len);
						ScaffoldBuilder.win.println("\t" + se.edge.len + "\t"
								+ se.edge.realDist() + "\t"
								+ se.edge.pairs.length);
						ScaffoldBuilder.win.println("\t" + se.x.id + "\t"
								+ se.y.id + "\t" + se.x.vertecies.size() + "\t"
								+ se.y.vertecies.size());
					} else {
						good++;
					}
				}
			}
			if (s.edges[0].size() == 2) {
				for (ScafEdge se : s.edges[0]) {
					if (inDeg[se.y.id][se.ey] > 1 || se.y.vertecies.size() < 2) {
						continue;
					}
					if (se.edge.realDist() > Data.getMaxInsertSize() + 3
							* Data.getMaxDeviation()) {
						bad2++;
						ScaffoldBuilder.win.println("bad2:\t"
								+ se.edge.v1.info.id + "\t"
								+ se.edge.v2.info.id + "\t"
								+ se.edge.v1.info.realPos + "\t"
								+ se.edge.v2.info.realPos + "\t"
								+ se.edge.v1.info.len + "\t"
								+ se.edge.v2.info.len);
						ScaffoldBuilder.win.println("\t" + se.edge.len + "\t"
								+ se.edge.realDist() + "\t"
								+ se.edge.pairs.length);
						ScaffoldBuilder.win.println("\t" + se.x.id + "\t"
								+ se.y.id + "\t" + se.x.vertecies.size() + "\t"
								+ se.y.vertecies.size());
					} else {
						good2++;
					}
				}
			}
		}
		System.err.println("good/bad: " + good + "/" + bad);
		System.err.println("good2/bad2: " + good2 + "/" + bad2);
		PrintWriter scafEdges = new PrintWriter("scafEdges");
		for (Scaffold s : ans) {
			for (int i = 0; i < s.edges.length; i++) {
				for (ScafEdge se : s.edges[i]) {
					int isGood = Math.abs(se.edge.v1.info.id
							- se.edge.v2.info.id) == 1 ? 1 : 0;
					scafEdges.println(se.x.vertecies.size() + "\t"
							+ se.y.vertecies.size() + "\t"
							+ se.x.edges[se.ex].size() + "\t"
							+ se.y.edges[se.ey].size() + "\t"
							+ se.edge.v1.info.len + "\t" + se.edge.v2.info.len
							+ "\t" + se.edge.len + "\t" + se.edge.pairs.length
							+ "\t" + isGood);
				}
			}
		}
		scafEdges.close();
	}

	static void rescale(List<Scaffold> ans) {
		for (ListIterator<Scaffold> it = ans.listIterator(); it.hasNext();) {
			Scaffold s = it.next();
			if (s.vertecies.isEmpty()) {
				it.remove();
			}
		}
		Collections.sort(ans, new Comparator<Scaffold>() {

			@Override
			public int compare(Scaffold s1, Scaffold s2) {
				return s1.getSum() - s2.getSum();
			}

		});
		for (int i = 0; i < ans.size(); i++) {
			ans.get(i).id = i;
		}

		for (Scaffold s : ans) {
			s.bindVertecies();
		}

	}

}
