package ru.ifmo.genetics.tools.scaffolder;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.math.MathException;

public class ScaffoldBuilder {

	static PrintWriter unions;

	public static Scaffold[] buildScaffolds(Vertex[] g) throws MathException,
			FileNotFoundException {
		unions = new PrintWriter("unions");

		List<Scaffold> ans = SmallScaffoldBuilder.buildSmallScaffolds(g);

		win = new PrintWriter("win");

//		boolean change = true;
//		while (change) {
//			// splitSmallTwos(ans);
//			convertScaffolds(ans);
//			GraphBuilder.restoreEdges(g, false);
//			GraphFilter.removeOvercovered(g, 9);
//			// GraphFilter.removeShortContigs(g, 500);
//			// GraphFilter.removePopular(g, i);
//			ScaffoldGraphBuilder.buildScafEdges(ans);
//			change = complexUnion(ans, 3) // || simpleUnion(ans, false)
//					|| insertsmall(ans, 2000)// || otherUnion(ans)
//			// || oneUnion(ans)
//			;
//			ScaffoldGraphBuilder.rescale(ans);
//		}
//		//
//		change = true;
//		while (change) {
//			// splitSmallTwos(ans);
//			convertScaffolds(ans);
//			GraphBuilder.restoreEdges(g, false);
//			GraphFilter.removeOvercovered(g, 30);
//			// GraphFilter.removeShortContigs(g, 500);
//			// GraphFilter.removePopular(g, i);
//			ScaffoldGraphBuilder.buildScafEdges(ans);
//			change = complexUnion(ans, 3) || insertsmall(ans, 2000)
//			// || simpleUnion(ans, false)
//			// || otherUnion(ans)
//			// || oneUnion(ans)
//			;
//			ScaffoldGraphBuilder.rescale(ans);
//		}

		// change = true;
		// while (change) {
		// // splitSmallTwos(ans);
		// convertScaffolds(ans);
		// GraphBuilder.restoreEdges(g, false);
		// GraphFilter.removeOvercovered(g, 27);
		// // GraphFilter.removeShortContigs(g, 500);
		// // GraphFilter.removePopular(g, i);
		// ScaffoldGraphBuilder.buildScafEdges(ans);
		// change = complexUnion(ans, 4) || insertsmall(ans, 2000)
		// // || one2Big(ans) // || oneVSBig(ans) || bigAndBig(ans)
		// // || simpleUnion(ans, false)
		// // || otherUnion(ans)
		// // || oneUnion(ans)
		// ;
		// ScaffoldGraphBuilder.rescale(ans);
		// }

		// change = true;
		// while (change) {
		// // splitSmallTwos(ans);
		// convertScaffolds(ans);
		// GraphBuilder.restoreEdges(g, false);
		// GraphFilter.removeOvercovered(g, 18);
		// // GraphFilter.removePopular(g, i);
		// buildScafEdges(ans);
		// change = oneUnion(ans) // || complexUnion(ans)
		// // || simpleUnion(ans, false)
		// // || otherUnion(ans)
		// ;
		// rescale(ans);
		// }

		GraphBuilder.restoreEdges(g, false);
		GraphFilter.removeOvercovered(g, 27);
		ScaffoldGraphBuilder.buildScafEdges(ans);

		// change = true;
		// while (change) {
		// // splitSmallTwos(ans);
		// convertScaffolds(ans);
		// GraphBuilder.restoreEdges(g, false);
		// // GraphFilter.removeOvercovered(g, 100);
		// // GraphFilter.removeLong(g, Data.NORMAL_DISTRIBUTION_CENTER /
		// // 2);
		// // GraphFilter.removeShort(g,
		// // -Data.NORMAL_DISTRIBUTION_DEVIATION);
		// // GraphFilter.removePopular(g, i);
		// buildScafEdges(ans);
		// change = complexUnion(ans)// || simpleUnion(ans, false)
		// // || otherUnion(ans)
		// ;
		// }

		for (Scaffold s : ans) {
			if (s.size() > 1) {
				continue;
			}
			if (s.edges[0].size() == 2 || s.edges[0].size() == 4) {
				win.println("Scaffold: " + s.id + "\t" + s.first().getCover()
						+ "\t" + s.getSum());
				win.println(s);
				for (ScafEdge se : s.edges[0]) {
					Edge e = se.edge;
					win.println("\t" + e.v2.info.id + "\t" + e.v2.getCover()
							+ "\t" + e.v2.s.id + "\t" + e.v2.s.vertecies.size()
							+ "\t" + e.v2.s.getSum() + "\t" + e + "\t"
							+ e.v2.info.realPos);
				}
				Vertex[] neigh = new Vertex[s.edges[0].size()];
				for (int i = 0; i < neigh.length; i++) {
					neigh[i] = s.edges[0].get(i).edge.v2;
				}
				for (int i = 0; i < neigh.length; i++) {
					for (int j = 0; j < i; j++) {
						win.println(neigh[i].info.id + "\t" + neigh[j].info.id
								+ "\t" + neigh[i].realDistTo(neigh[j]));
					}
				}
				if (s.edges[0].size() > 2) {
					int edges = 0;
					for (Vertex v : neigh) {
						for (Edge e : v.edges) {
							for (int i = 0; i < neigh.length; i++) {
								if (neigh[i] == e.v2) {
									win.println("edge: " + e.v1.info.id + "\t"
											+ e.v2.info.id + "\t" + e);
									edges++;
								}
							}
						}
					}
					if (edges == 0 && neigh.length == 4) {
						Vertex best0 = null;
						double qSum = Double.POSITIVE_INFINITY;
						Vertex v1 = neigh[0];
						for (int i = 1; i < neigh.length; i++) {
							Vertex v2 = neigh[i];
							Vertex v3 = null;
							Vertex v4 = null;
							for (int j = 1; j < neigh.length; j++) {
								if (i == j) {
									continue;
								}
								if (v3 != null) {
									v4 = neigh[j];
								} else {
									v3 = neigh[j];
								}
							}
							int d12 = s.first().info.len;
							int d34 = d12;
							for (Edge e : v1.edges) {
								if (e.v2 == s.first()) {
									d12 += e.len;
								}
							}
							for (Edge e : v2.edges) {
								if (e.v2 == s.first()) {
									d12 += e.len;
								}
							}
							for (Edge e : v3.edges) {
								if (e.v2 == s.first()) {
									d34 += e.len;
								}
							}
							for (Edge e : v4.edges) {
								if (e.v2 == s.first()) {
									d34 += e.len;
								}
							}
							double q12 = DistanceFinder
									.getProbabilityThatAtLeastOneMatepairMatches(
											d12, v1.info.len, v2.info.len);
							double q34 = DistanceFinder
									.getProbabilityThatAtLeastOneMatepairMatches(
											d34, v3.info.len, v4.info.len);
							if (qSum > q12 + q34) {
								qSum = q12 + q34;
								best0 = v2;
							}
						}
						Vertex v3 = null;
						Vertex v4 = null;
						for (int j = 1; j < neigh.length; j++) {
							if (neigh[j] == best0) {
								continue;
							}
							if (v3 != null) {
								v4 = neigh[j];
							} else {
								v3 = neigh[j];
							}
						}
						if (v3 != null && v4 != null && best0 != null) {
							win.println("best: " + neigh[0].info.id + "\t"
									+ best0.info.id + "\t"
									+ neigh[0].realDistTo(best0));
							win.println("best: " + v3.info.id + "\t"
									+ v4.info.id + "\t" + v3.realDistTo(v4));
						}
					}
				}
			}
		}

		ScaffoldGraphBuilder.rescale(ans);

		for (Scaffold s : ans) {
			win.println("Scaffold#" + s.id);
			for (int i = 0; i < s.edges.length; i++) {
				win.println("side " + i);
				for (ScafEdge se : s.edges[i]) {
					Edge e = se.edge;
					win.println("\t" + e.v1.info.id + "\t" + e.v2.info.id
							+ "\t" + e.v2.s.id + "\t" + e.v2.s.vertecies.size()
							+ "\t" + e);
				}
			}
		}

		for (Vertex v : g) {
			if (v.isOnBorder() || v.isSecondToBorder()) {
				win.println("Vertex#" + v.info.id + ":\t" + v.getCover() + "\t"
						+ v.s.id + "\t" + v.s.size() + "\t" + v.info.len);
				for (Edge e : v.edges) {
					if (e.v2.isOnBorder() || e.v2.isSecondToBorder()) {
						win.println("\t" + e.v2.info.id + "\t"
								+ e.v2.getCover() + "\t" + e.v2.s.id + "\t"
								+ e.v2.s.vertecies.size() + "\t" + e);
					}
				}
			}
		}

		win.close();

		Scaffold[] ss = convertScaffolds(ans);

		for (Scaffold s : ss) {
			assignOrientation(s);
		}

		for (Scaffold s : ss) {
			assignPosition(s);
		}

		return ss;
	}

	private static boolean one2Big(List<Scaffold> ans) {
		int[] cnt = new int[ans.size()];
		for (Scaffold s : ans) {
			if (s.size() > 1) {
				continue;
			}
			Arrays.fill(cnt, 0);
			for (ScafEdge se : s.edges[0]) {
				cnt[se.y.id]++;
			}
			int big = 0;
			for (int i = 0; i < cnt.length; i++) {
				if (cnt[i] > 1) {
					big++;
				}
			}
			if (big == 1) {
				Scaffold b = null;
				for (int i = 0; i < cnt.length; i++) {
					if (cnt[i] > 1) {
						b = ans.get(i);
					}
				}
				ScafEdge best = null;
				for (ScafEdge se : s.edges[0]) {
					if (se.y == b) {
						if (best == null || best.edge.len > se.edge.len) {
							best = se;
						}
					}
				}
				if (best != null) {
					unions.println("one = big");
					merge(best);
					return true;
				}
			}
			if (big == 2) {

			}
		}
		return false;
	}

	private static boolean bigAndBig(List<Scaffold> ans) {
		ScafEdge best = null;
		for (Scaffold s : ans) {
			if (s.size() < 2) {
				continue;
			}
			for (int i = 0; i < s.edges.length; i++) {
				int big = 0;
				ScafEdge bs = null;
				for (ScafEdge se : s.edges[i]) {
					if (se.y.size() < 2) {
						continue;
					}
					big++;
					if (bs == null || bs.edge.len > se.edge.len) {
						bs = se;
					}
				}
				if (big == 1) {
					if (best == null || best.edge.len > bs.edge.len) {
						best = bs;
					}
				}
			}
		}
		if (best != null) {
			unions.println("big & big");
			merge(best);
			return true;
		}
		return false;
	}

	private static boolean oneVSBig(List<Scaffold> ans) {
		Scaffold best = null;
		int bestDist = Integer.MAX_VALUE / 2;
		for (Scaffold s : ans) {
			if (s.size() > 1 || s.edges[0].size() > 3) {
				continue;
			}
			int big = 0;
			int dist = 0;
			ScafEdge se1 = null;
			ScafEdge se2 = null;
			for (ScafEdge se : s.edges[0]) {
				if (se.y.size() > 1) {
					big++;
					dist += se.edge.len;
					if (se1 == null) {
						se1 = se;
					} else {
						se2 = se;
					}
				}
			}
			if (big == 2 && bestDist > dist && se1.y != se2.y) {
				bestDist = dist;
				best = s;
			}
		}
		if (best != null) {
			ScafEdge se1 = null;
			ScafEdge se2 = null;
			for (ScafEdge se : best.edges[0]) {
				if (se.y.size() > 1) {
					if (se1 == null) {
						se1 = se;
					} else {
						se2 = se;
					}
				}
			}
			unions.println("one vs big");
			merge(se1.rev(), se2);
			return true;
		}
		return false;
	}

	private static void assignPosition(Scaffold s) {
		for (Vertex v : s.vertecies) {
			v.pos = -1;
		}
		posDfs(s.first());
	}

	private static void posDfs(Vertex v) {
		if (v.pos < 0) {
			v.pos = 0;
		}
		for (Edge e : v.edges) {
			if (e.v2.s != v.s || e.v2.pos >= 0) {
				continue;
			}
			e.v2.pos = v.pos + e.len + (v.info.len + e.v2.info.len) / 2;
			posDfs(e.v2);
		}
	}

	private static void assignOrientation(Scaffold s) {
		orientDfs(s.first());
	}

	private static void orientDfs(Vertex v) {
		if (v.or == 0) {
			v.or = 1;
		}
		// System.err.println("Vertex#" + v.info.id + ":\t" + v.getOrientation()
		// + "\t" + v.or);
		for (Edge e : v.edges) {
			if (e.v2.s != v.s || e.v2.or != 0) {
				continue;
			}
			if (e.isReverse()) {
				e.v2.or = -v.or;
			} else {
				e.v2.or = v.or;
			}
			orientDfs(e.v2);
		}
	}

	private static boolean insertsmall(List<Scaffold> ans, int len) {
		for (Scaffold s : ans) {
			if (s.size() > 1 || s.first().info.len > len
					|| s.first().info.getCover() > Data.getOneCover()) {
				continue;
			}
			if (s.first().edges.size() == 2) {
				Vertex v1 = s.first().edges.get(0).v2;
				Vertex v2 = s.first().edges.get(1).v2;
				int ind1 = v1.getScaffoldIndex();
				int ind2 = v2.getScaffoldIndex();
				if (v1.s == v2.s && Math.abs(ind1 - ind2) == 1) {
					unions.println("insert small");
					unions.println("was");
					unions.println(v1.s);
					unions.println(s);
					v1.s.insertContig(Math.max(ind1, ind2), s.first());
					s.first().s = v1.s;
					s.clear();
					unions.println("now");
					unions.println(v1.s);
					unions.flush();
					return true;
				}
			}
		}
		return false;
	}

	private static boolean oneUnion(ArrayList<Scaffold> ans)
			throws MathException {
		for (Scaffold s : ans) {
			if (s.size() > 1
					|| s.first().info.len < 2 * Data.getMaxInsertSize()) {
				continue;
			}
			if (s.edges[0].size() == 2) {
				ScafEdge s1 = s.edges[0].get(0);
				ScafEdge s2 = s.edges[0].get(1);
				if (s1.y != s2.y) {
					if (s1.y.size() + s2.y.size() < 3) {
						continue;
					}
					unions.println("one union");
					merge(s1.rev(), s2);
				} else {
					unions.println("one union");
					if (s1.edge.v2.isOnBorder()) {
						merge(s1);
					} else {
						merge(s2);
					}
					if (s.secondToFirst().info.len > Data.getMaxInsertSize()) {
						s.swapFirst();
					}
				}
				return true;
			}
			if (s.edges[0].size() == 4) {
				Vertex[] neigh = new Vertex[s.edges[0].size()];
				for (int i = 0; i < neigh.length; i++) {
					neigh[i] = s.edges[0].get(i).edge.v2;
				}
				Edge[][] a = new Edge[neigh.length][neigh.length];
				int edges = 0;
				for (int j = 0; j < neigh.length; j++) {
					Vertex v = neigh[j];
					for (Edge e : v.edges) {
						for (int i = 0; i < neigh.length; i++) {
							if (neigh[i] == e.v2) {
								a[j][i] = e;
								edges++;
							}
						}
					}
				}
				edges /= 2;
				if (edges == 0) {
					Vertex best0 = null;
					double qSum = Double.POSITIVE_INFINITY;
					Vertex v1 = neigh[0];
					for (int i = 1; i < neigh.length; i++) {
						Vertex v2 = neigh[i];
						Vertex v3 = null;
						Vertex v4 = null;
						for (int j = 1; j < neigh.length; j++) {
							if (i == j) {
								continue;
							}
							if (v3 != null) {
								v4 = neigh[j];
							} else {
								v3 = neigh[j];
							}
						}
						int d12 = s.first().info.len;
						int d34 = d12;
						for (Edge e : v1.edges) {
							if (e.v2 == s.first()) {
								d12 += e.len;
							}
						}
						for (Edge e : v2.edges) {
							if (e.v2 == s.first()) {
								d12 += e.len;
							}
						}
						for (Edge e : v3.edges) {
							if (e.v2 == s.first()) {
								d34 += e.len;
							}
						}
						for (Edge e : v4.edges) {
							if (e.v2 == s.first()) {
								d34 += e.len;
							}
						}
						double q12 = DistanceFinder
								.getProbabilityThatAtLeastOneMatepairMatches(
										d12, v1.info.len, v2.info.len);
						double q34 = DistanceFinder
								.getProbabilityThatAtLeastOneMatepairMatches(
										d34, v3.info.len, v4.info.len);
						if (qSum > q12 + q34) {
							qSum = q12 + q34;
							best0 = v2;
						}
					}
					Vertex v3 = null;
					Vertex v4 = null;
					for (int j = 1; j < neigh.length; j++) {
						if (neigh[j] == best0) {
							continue;
						}
						if (v3 != null) {
							v4 = neigh[j];
						} else {
							v3 = neigh[j];
						}
					}
					if (neigh[0].s == best0.s || v3.s == v4.s) {
						continue;
					}
					unions.println("edge0");
					merge(neigh[0], s.first().copy(), best0);
					merge(v3, s.first(), v4);
					return true;
				} else if (edges == 1) {
					for (int i = 0; i < neigh.length; i++) {
						for (int j = 0; j < neigh.length; j++) {
							if (a[i][j] != null) {
								int k = 0;
								while (k == i || k == j) {
									k++;
								}
								int l = 0;
								while (l == i || l == j || l == k) {
									l++;
								}
								if (neigh[i].s == neigh[j].s
										|| neigh[k].s == neigh[l].s) {
									continue;
								}
								unions.println("edge1");
								merge(neigh[i], s.first().copy(), neigh[j]);
								merge(neigh[k], s.first(), neigh[l]);
								return true;
							}
						}
					}
				} else if (edges == 2) {
					int[] deg = new int[neigh.length];
					for (int i = 0; i < a.length; i++) {
						for (int j = 0; j < a[i].length; j++) {
							if (a[i][j] != null) {
								deg[i]++;
							}
						}
					}
					for (int i = 0; i < deg.length; i++) {
						if (deg[i] == 0 && neigh[i].s != s) {
							unions.println("edge 1-2");
							merge(neigh[i], s.first());
							return true;
						}
					}
					for (int i = 0; i < neigh.length; i++) {
						for (int j = 0; j < neigh.length; j++) {
							if (a[i][j] != null) {
								int k = 0;
								while (k == i || k == j) {
									k++;
								}
								int l = 0;
								while (l == i || l == j || l == k) {
									l++;
								}
								if (neigh[i].s == neigh[j].s
										|| neigh[k].s == neigh[l].s) {
									continue;
								}
								unions.println("edge 1-1 1-1");
								merge(neigh[i], s.first().copy(), neigh[j]);
								merge(neigh[k], s.first(), neigh[l]);
								return true;
							}
						}
					}
				} else if (edges >= 3) {
					int dist = Integer.MAX_VALUE / 2;
					Vertex best = null;
					for (ScafEdge se : s.edges[0]) {
						if (se.edge.len < dist) {
							dist = se.edge.len;
							best = se.edge.v2;
						}
					}
					if (best != null) {
						unions.println("edge3");
						merge(best, s.first());
						return true;
					}
				}
			}
		}
		return false;
	}

	private static void merge(Vertex main, Vertex addition) {
		Scaffold s1 = main.s;
		if (main.isFirst() || (!main.isLast() && main.isSecondToFirst())) {
			s1.reverse();
		}
		unions.println("before");
		unions.println(s1);
		Scaffold s2 = new Scaffold();
		s2.vertecies.add(addition);
		unions.println(s2);
		unions.flush();
		s1.vertecies.add(addition);
		addition.s.vertecies.clear();
		unions.println("after");
		unions.println(s1);
		unions.flush();
	}

	private static void merge(Vertex v1, Vertex v2, Vertex v3) {
		Scaffold s1 = v1.s;
		if (v1.isFirst() || (!v1.isLast() && v1.isSecondToFirst())) {
			s1.reverse();
		}
		Scaffold s3 = v3.s;
		if (v3.isLast() || (!v3.isFirst() && v3.isSecondToLast())) {
			s3.reverse();
		}
		unions.println("before");
		unions.println(s1);
		Scaffold s2 = new Scaffold();
		s2.vertecies.add(v2);
		unions.println(s2);
		unions.println(s3);
		unions.flush();
		s1.vertecies.add(v2);
		s1.vertecies.addAll(s3.vertecies);
		s3.vertecies.clear();
		s3.clear();
		unions.println("after");
		unions.println(s1);
		unions.flush();
	}

	private static boolean otherUnion(ArrayList<Scaffold> ans) {
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

		for (Scaffold s : ans) {
			if (s.vertecies.size() > 1) {
				continue;
			}
			if (s.edges[0].size() == 1) {
				for (ScafEdge se : s.edges[0]) {
					if (inDeg[se.y.id][se.ey] > 1 || se.y.vertecies.size() < 2) {
						continue;
					}
					System.err.println("good " + se.edge.v1.info.id + " "
							+ se.edge.v2.info.id + " " + se.x.vertecies.size()
							+ " " + se.y.vertecies.size());
					unions.println("other union");
					return merge(se.rev());
				}
			}
		}
		for (Scaffold s : ans) {
			if (s.vertecies.size() > 1) {
				continue;
			}
			if (s.edges[0].size() == 2) {
				for (ScafEdge se : s.edges[0]) {
					if (inDeg[se.y.id][se.ey] > 1 || se.y.vertecies.size() < 2) {
						continue;
					}
					System.err.println("good2 " + se.edge.v1.info.id + " "
							+ se.edge.v2.info.id + " " + se.x.vertecies.size()
							+ " " + se.y.vertecies.size());
					return merge(se.rev());
				}
			}
		}

		return false;
	}

	private static void splitSmallTwos(ArrayList<Scaffold> ans) {
		for (ListIterator<Scaffold> it = ans.listIterator(); it.hasNext();) {
			Scaffold s = it.next();
			if (s.size() == 2
					&& Math.min(s.first().info.len, s.last().info.len) < Data
							.getMaxInsertSize()) {
				Scaffold s2 = new Scaffold();
				s2.vertecies.add(s.pollFirst());
				it.add(s2);
				s.bindVertecies();
				s2.bindVertecies();
			}
		}
		ScaffoldGraphBuilder.rescale(ans);
	}

	static PrintWriter paths;
	static PrintWriter badPaths;

	private static boolean complexUnion(List<Scaffold> ans, int bound)
			throws FileNotFoundException {
		ScaffoldGraphBuilder.rescale(ans);
		System.err.println("complex");
		if (paths == null) {
			paths = new PrintWriter("paths");
			badPaths = new PrintWriter("bad_paths");
		}
		Scaffold start = null;
		Scaffold finish = null;
		int startDir = 0;
		int minDist = Integer.MAX_VALUE / 2;
		for (Scaffold s : ans) {
			if (s.vertecies.size() < 2) {
				continue;
			}
			for (int i = 0; i < s.edges.length; i++) {
				bfs(s, i, ans, bound);
				for (Scaffold s2 : ans) {
					if (s == s2 || s2.vertecies.size() < 2) {
						continue;
					}
					if (minDist > s2.d && s2.prev != null) {
						boolean good = true;
						ScafEdge e = s2.prev;
						while (e != null) {
							good &= e.x.edges[e.ex].size() < bound
									|| e.y.edges[e.ey].size() < bound;
							e = e.x.prev;
						}
						if (good) {
							minDist = s2.d;
							start = s;
							finish = s2;
							startDir = i;
						} else {
							badPaths.println("path:");
							e = s2.prev;
							while (e != null) {
								Vertex v = e.edge.v2;
								badPaths.println(v.info.id + "\t"
										+ e.y.edges[e.ey].size() + "\t"
										+ v.getCover() + "\t" + v.info.realPos);
								badPaths.println(e.edge.v1.info.id + "\t"
										+ e.edge.v2.info.id + "\t" + e.edge);
								if (e.x.prev == null) {
									v = e.edge.v1;
									badPaths.println(v.info.id + "\t"
											+ e.x.edges[e.ex].size() + "\t"
											+ v.getCover() + "\t"
											+ v.info.realPos);
								}
								e = e.x.prev;
							}
							badPaths.flush();
						}
					}
				}
			}
		}
		if (start == null) {
			return false;
		}
		bfs(start, startDir, ans, bound);
		if (finish.vertecies.isEmpty() || finish.prev == null) {
			System.err.println("error: " + finish.id);
		}
		unions.println("complex union");
		unionAll(finish);
		ScaffoldGraphBuilder.rescale(ans);
		paths.flush();
		return true;
	}

	private static void unionAll(Scaffold finish) {
		ArrayList<ScafEdge> al = new ArrayList<ScafEdge>();
		ScafEdge e = finish.prev;
		while (e != null) {
			al.add(e);
			e = e.x.prev;
		}
		Collections.reverse(al);
		paths.println("new path");
		// System.err.println(finish.id + "\t" + finish.prev);
		ScafEdge[] all = new ScafEdge[al.size()];
		int cnt = 0;
		for (ScafEdge se : al) {
			paths.println(se.edge.v1.info.id + "\t" + se.edge.v2.info.id + "\t"
					+ se.edge);
			all[cnt++] = se;
		}
		merge(all);
		paths.flush();
	}

	private static void bfs(Scaffold s, int i, List<Scaffold> ans, int bound) {
		for (Scaffold ss : ans) {
			ss.d = Integer.MAX_VALUE / 2;
			ss.u = false;
			ss.prev = null;
		}
		s.d = 0;
		ArrayDeque<ScafEdge> deque = new ArrayDeque<ScafEdge>();
		for (ScafEdge se : s.edges[i]) {
			if (se.x.edges[se.ex].size() >= bound
					&& se.y.edges[se.ey].size() >= bound
					|| se.edge.len > Data.getMaxInsertSize()) {
				continue;
			}
			int w = se.edge.len + se.edge.v2.info.len;
			// if (se.edge.len + se.edge.v2.info.len < 0) {
			// continue;
			// }
			w = Math.max(0, w);
			deque.addLast(se);
			se.y.u = true;
			se.y.d = w;
			se.y.prev = se;
		}
		while (!deque.isEmpty()) {
			ScafEdge se = deque.pollFirst();
			se.y.u = false;
			// System.err.println(se.y.id + "\t" + se.y.d);
			if (se.y.vertecies.size() > 1) {
				continue;
			}
			for (ScafEdge e : se.y.edges[0]) {
				if (e.x.edges[e.ex].size() >= bound
						&& e.y.edges[e.ey].size() >= bound
						|| se.edge.len > Data.getMaxInsertSize()) {
					continue;
				}
				int w = e.edge.len + e.edge.v2.info.len;
				// if (e.edge.len + e.edge.v2.info.len < 0) {
				// continue;
				// }
				w = Math.max(0, w);
				if (e.y.d > e.x.d + w) {
					e.y.d = e.x.d + w;
					e.y.prev = e;
					if (!e.y.u) {
						e.y.u = true;
						deque.addLast(e);
					}
				}
			}
		}
	}

	private static int dfs(ScafEdge e) {
		e.y.u = true;
		if (e.y.vertecies.size() > 2) {
			return 1;
		}
		int sum = 0;
		for (ScafEdge se : e.y.edges[e.y.vertecies.size() == 1 ? 0 : (1 - e.ey)]) {
			if (!se.y.u) {
				int v = dfs(se);
				sum += v;
				if (v > 0) {
					e.y.prev = se;
				}
			}
		}
		return sum;
	}

	private static void fixOrder(ArrayList<Scaffold> ans) throws MathException {
		int good213 = 0;
		int bad213 = 0;
		int good132 = 0;
		int bad132 = 0;
		for (Scaffold s : ans) {
			ArrayList<Vertex> al = new ArrayList<Vertex>(s.vertecies);
			for (int i = 0; i + 2 < al.size(); i++) {
				Vertex v1 = al.get(i);
				Vertex v2 = al.get(i + 1);
				Vertex v3 = al.get(i + 2);
				Edge e12 = null;
				Edge e23 = null;
				Edge e13 = null;
				for (Edge e : v1.edges) {
					if (e.ghost) {
						continue;
					}
					if (e.v2 == v2) {
						e12 = e;
					}
					if (e.v2 == v3) {
						e13 = e;
					}
				}
				for (Edge e : v2.edges) {
					if (e.ghost) {
						continue;
					}
					if (e.v2 == v3) {
						e23 = e;
					}
				}
				int bad = 0;
				bad += e12 == null ? 1 : 0;
				bad += e13 == null ? 1 : 0;
				bad += e23 == null ? 1 : 0;
				if (Math.abs(v2.info.id - v1.info.id) == 1
						&& Math.abs(v1.info.id - v3.info.id) == 1) {
					win.println("should:\t" + v1.info.id + "\t" + v2.info.id
							+ "\t" + v3.info.id + "\t" + bad);
					win.println(v1.info.len + "\t" + v2.info.len + "\t"
							+ v3.info.len);
					win.println(e12 + "\t" + e23 + "\t" + e13);
					win.println(v1.realDistTo(v2) + "\t" + v2.realDistTo(v3)
							+ "\t" + v1.realDistTo(v3));
				}
				if (Math.abs(v1.info.id - v3.info.id) == 1
						&& Math.abs(v3.info.id - v2.info.id) == 1) {
					win.println("should:\t" + v1.info.id + "\t" + v2.info.id
							+ "\t" + v3.info.id + "\t" + bad);
					win.println(v1.info.len + "\t" + v2.info.len + "\t"
							+ v3.info.len);
					win.println(e12 + "\t" + e23 + "\t" + e13);
					win.println(v1.realDistTo(v2) + "\t" + v2.realDistTo(v3)
							+ "\t" + v1.realDistTo(v3));
				}
				if (e12 != null && e13 != null && e23 != null) {
					if (e12.len > e13.len && e12.len > e23.len) {
						win.println("swap was:\t" + v1.info.id + "\t"
								+ v2.info.id + "\t" + v3.info.id);
						al.set(i + 1, v3);
						al.set(i + 2, v2);
						win.println("swap now:\t" + v1.info.id + "\t"
								+ v3.info.id + "\t" + v2.info.id);
						continue;
					} else if (e23.len > e12.len && e23.len > e13.len) {
						win.println("swap was:\t" + v1.info.id + "\t"
								+ v2.info.id + "\t" + v3.info.id);
						al.set(i, v2);
						al.set(i + 1, v1);
						win.println("swap now:\t" + v2.info.id + "\t"
								+ v1.info.id + "\t" + v3.info.id);
						continue;
					}
				}
				if (bad > 1) {
					continue;
				}
				if (e13 == null && v1.info.len < Data.getMaxInsertSize()
						&& e23.len > e12.len + v1.info.len
				/*
				 * && e23.len - e12.len + v3.info.len <
				 * Data.NORMAL_DISTRIBUTION_CENTER
				 */) {
					if (Math.abs(v2.info.id - v1.info.id) == 1
							&& Math.abs(v1.info.id - v3.info.id) == 1) {
						good213++;
					} else {
						bad213++;
					}
					// System.err.println(v1.info.id + "\t" + v2.info.id + "\t"
					// + v3.info.id + "\t" + v1.info.len + "\t"
					// + v2.info.len + "\t" + v3.info.len + "\t" + e12.len
					// + "\t" + e23.len + "\t" + v1.realDistTo(v2) + "\t"
					// + v2.realDistTo(v3) + "\t" + v1.realDistTo(v3));
				}
				if (e13 == null && v3.info.len < Data.getMaxInsertSize()
						&& e12.len > e23.len + v3.info.len
				/*
				 * && e12.len - e23.len + v1.info.len <
				 * Data.NORMAL_DISTRIBUTION_CENTER
				 */) {
					if (Math.abs(v1.info.id - v3.info.id) == 1
							&& Math.abs(v3.info.id - v2.info.id) == 1) {
						good132++;
					} else {
						bad132++;
					}
					// System.err.println(v1.info.id + "\t" + v2.info.id + "\t"
					// + v3.info.id + "\t" + v1.info.len + "\t"
					// + v2.info.len + "\t" + v3.info.len + "\t" + e12.len
					// + "\t" + e23.len + "\t" + v1.realDistTo(v2) + "\t"
					// + v2.realDistTo(v3) + "\t" + v1.realDistTo(v3));
				}
				int reads = 0;
				reads += e12 == null ? 0 : e12.pairs.length;
				reads += e13 == null ? 0 : e13.pairs.length;
				reads += e23 == null ? 0 : e23.pairs.length;
				double q123 = 0;
				int d12 = e12 == null ? e13.len - e23.len - v2.info.len
						: e12.len;
				int d23 = e23 == null ? e13.len - e12.len - v2.info.len
						: e23.len;
				int d13 = d12 + v2.info.len + d23;
				q123 += DistanceFinder
						.getProbabilityThatAtLeastOneMatepairMatches(d12,
								v1.info.len, v2.info.len);
				q123 += DistanceFinder
						.getProbabilityThatAtLeastOneMatepairMatches(d13,
								v1.info.len, v3.info.len);
				q123 += DistanceFinder
						.getProbabilityThatAtLeastOneMatepairMatches(d23,
								v2.info.len, v3.info.len);
				double q213 = 0;
				int d21 = e12 == null ? e23.len - e13.len - v1.info.len
						: e12.len;
				d13 = e13 == null ? e23.len - e12.len - v1.info.len : e13.len;
				d23 = d21 + v1.info.len + d13;
				q213 += DistanceFinder
						.getProbabilityThatAtLeastOneMatepairMatches(d21,
								v2.info.len, v1.info.len);
				q213 += DistanceFinder
						.getProbabilityThatAtLeastOneMatepairMatches(d13,
								v1.info.len, v3.info.len);
				q213 += DistanceFinder
						.getProbabilityThatAtLeastOneMatepairMatches(d23,
								v2.info.len, v3.info.len);
				double q132 = 0;
				d13 = e13 == null ? e12.len - e23.len - v3.info.len : e13.len;
				int d32 = e23 == null ? e13.len - e13.len - v3.info.len
						: e23.len;
				d12 = d13 + v3.info.len + d32;
				q132 += DistanceFinder
						.getProbabilityThatAtLeastOneMatepairMatches(d13,
								v1.info.len, v3.info.len);
				q132 += DistanceFinder
						.getProbabilityThatAtLeastOneMatepairMatches(d32,
								v3.info.len, v2.info.len);
				q132 += DistanceFinder
						.getProbabilityThatAtLeastOneMatepairMatches(d12,
								v1.info.len, v1.info.len);
				int r123 = (int) Math.round(q123 * Data.allReads());
				int r213 = (int) Math.round(q213 * Data.allReads());
				int r132 = (int) Math.round(q132 * Data.allReads());
				if (Math.abs(reads - r123) <= Math.abs(reads - r213) + 70
						&& Math.abs(reads - r123) <= Math.abs(reads - r132) + 70) {

				} else if (Math.abs(reads - r213) <= Math.abs(reads - r123)
						&& Math.abs(reads - r213) <= Math.abs(reads - r132)) {
					win.println("swap was:\t" + v1.info.id + "\t" + v2.info.id
							+ "\t" + v3.info.id + "\t" + reads + "\t" + r123
							+ "\t" + r213 + "\t" + r132);
					al.set(i, v2);
					al.set(i + 1, v1);
					win.println("swap now:\t" + v2.info.id + "\t" + v1.info.id
							+ "\t" + v3.info.id);
				} else if (Math.abs(reads - r132) <= Math.abs(reads - r123)
						&& Math.abs(reads - r132) <= Math.abs(reads - r213)) {
					win.println("swap was:\t" + v1.info.id + "\t" + v2.info.id
							+ "\t" + v3.info.id + "\t" + reads + "\t" + r123
							+ "\t" + r213 + "\t" + r132);
					al.set(i + 1, v3);
					al.set(i + 2, v2);
					win.println("swap now:\t" + v1.info.id + "\t" + v3.info.id
							+ "\t" + v2.info.id);
				}
			}
			s.vertecies = new ArrayList<Vertex>(al);
		}
		System.err.println("\t" + good213 + "/" + bad213 + "\t" + good132 + "/"
				+ bad132);
	}

	static PrintWriter win;

	private static Scaffold[] convertScaffolds(List<Scaffold> ans)
			throws MathException {
		ScaffoldGraphBuilder.rescale(ans);
		Scaffold[] ss = new Scaffold[ans.size()];
		for (int i = 0; i < ss.length; i++) {
			ss[i] = ans.get(i);
		}

		int singleton = 0;
		int sumLen = 0;
		int full = 0;
		int connections = 0;
		int errors = 0;
		for (Scaffold s : ss) {
			full += s.getSum();
			if (s.vertecies.size() == 1) {
				singleton++;
			} else {
				sumLen += s.getSum();
				Vertex prev = null;
				Vertex prev2 = null;
				boolean pb = false;
				for (Vertex v : s.vertecies) {
					if (prev != null) {
						if (v.realDistTo(prev) > Data.getMaxInsertSize() + 3
								* Data.getMaxDeviation()) {
							errors++;
							pb = true;
							Edge e = null;
							for (Edge e2 : prev.edges) {
								if (e2.v2 == v) {
									e = e2;
								}
							}
							System.err.println("error connection: "
									+ prev.info.id + " " + v.info.id + "\t"
									+ prev.realDistTo(v) + "\t" + e);
						} else if (!pb
								&& prev2 != null
								&& Math.abs(prev2.realDistTo(prev)
										+ prev.info.len + prev.realDistTo(v)
										- prev2.realDistTo(v)) > Data
											.getMaxDeviation()
								&& prev2.realDistTo(prev) < Data
										.getMaxInsertSize()
										+ 3
										* Data.getMaxDeviation()) {
							pb = true;
							errors++;
							System.err.println("error connection: "
									+ prev2.info.id
									+ " "
									+ prev.info.id
									+ " "
									+ v.info.id
									+ "\t"
									+ prev2.realDistTo(prev)
									+ " "
									+ prev.info.len
									+ " "
									+ prev.realDistTo(v)
									+ "\t"
									+ prev2.realDistTo(v)
									+ "\t"
									+ Math.abs(prev2.realDistTo(prev)
											+ prev.info.len
											+ prev.realDistTo(v)
											- prev2.realDistTo(v)));
						} else if (Math.abs(v.info.id - prev.info.id) > 1) {
							// System.err.println("messed up connection: "
							// + prev.info.id + " " + v.info.id + "\t"
							// + prev.realDistTo(v));
							pb = false;
						} else {
							pb = false;
						}
						connections++;
					}
					prev2 = prev;
					prev = v;
				}
			}
		}

		int s2 = 0;
		for (int i = ss.length - 1; i >= 0; i--) {
			s2 += ss[i].getSum();
			if (2 * s2 >= sumLen) {
				System.err.println("N50: " + ss[i].getSum());
				if (paths != null) {
					paths.println("N50: " + ss[i].getSum());
				}
				break;
			}
		}
		s2 = 0;
		for (int i = ss.length - 1; i >= 0; i--) {
			s2 += ss[i].getSum();
			if (2 * s2 >= full) {
				System.err.println("N50(2): " + ss[i].getSum());
				if (paths != null) {
					paths.println("N50(2): " + ss[i].getSum());
				}
				break;
			}
		}

		int single = 0;
		for (Scaffold s : ss) {
			if (s.vertecies.size() == 1) {
				single++;
			}
		}

		System.err.println("scaffolds: " + (ss.length - singleton));
		System.err.println("len: " + sumLen);
		System.err.println("full: " + full);
		System.err.println("singleton: " + single);
		System.err.println("connections: " + connections);
		if (paths != null) {
			paths.println("errors: " + errors);
		}
		System.err.println("errors: " + errors);
		System.err.println("newSing: " + newSing);
		System.err.println("win: " + win);

		ArrayList<Integer> realLengths = new ArrayList<Integer>();
		for (Scaffold s : ss) {
			realLengths.add(s.getSum());
		}
		Collections.sort(realLengths);
		System.err.println(realLengths.size());
		realLengths.clear();

		for (Scaffold s : ss) {
			Vertex prev = null;
			Vertex prev2 = null;
			boolean pb = false;
			int len = 0;
			for (Vertex v : s.vertecies) {
				if (prev != null) {
					if (v.realDistTo(prev) > Data.getMaxInsertSize() + 3
							* Data.getMaxDeviation()) {
						realLengths.add(len);
						len = 0;
						pb = true;
					} else if (!pb
							&& prev2 != null
							&& Math.abs(prev2.realDistTo(prev) + prev.info.len
									+ prev.realDistTo(v) - prev2.realDistTo(v)) > Data
										.getMaxDeviation()
							&& prev2.realDistTo(prev) < Data.getMaxInsertSize()
									+ 3 * Data.getMaxDeviation()) {
						realLengths.add(len);
						len = 0;
						pb = true;
					} else if (Math.abs(v.info.id - prev.info.id) > 1) {
						pb = false;
					} else {
						pb = false;
					}
				}
				len += v.info.len;
				prev2 = prev;
				prev = v;
			}
			realLengths.add(len);
		}

		Collections.sort(realLengths);
		System.err.println(realLengths.size());
		s2 = 0;
		for (int i = realLengths.size() - 1; i >= 0; i--) {
			int x = realLengths.get(i);
			s2 += x;
			if (2 * s2 >= full) {
				if (paths != null) {
					paths.println("N50(cut): " + x);
				}
				System.err.println("N50(cut): " + x);
				break;
			}
		}

		for (Scaffold s : ss) {
			s.bindVertecies();
		}

		return ss;
	}

	private static boolean simpleUnion(List<Scaffold> ans, boolean allowBig) {
		for (ListIterator<Scaffold> li = ans.listIterator(); li.hasNext();) {
			if (li.next().size() == 0) {
				li.remove();
			}
		}
		for (Scaffold s : ans) {
			if (s.vertecies.size() < 2) {
				continue;
			}
			for (int i = 0; i < s.edges.length; i++) {
				for (ListIterator<ScafEdge> it = s.edges[i].listIterator(); it
						.hasNext();) {
					if (it.next().y.size() == 0) {
						it.remove();
					}
				}
			}
			if (allowBig) {
				filterOnlyBig(s.edges[0]);
				filterOnlyBig(s.edges[1]);
			}
			if (s.first().info.id == 218 || s.last().info.id == 218) {
				System.err.println("scaffold#" + s.id);
				System.err.println(s);
				for (int i = 0; i < s.edges.length; i++) {
					for (ScafEdge se : s.edges[i]) {
						Edge e = se.edge;
						System.err.println("\t" + e.v1.info.id + "\t"
								+ e.v2.info.id + "\t" + e.v2.getCover() + "\t"
								+ e.v2.s.id + "\t" + e.v2.s.vertecies.size()
								+ "\t" + e);
					}
				}
			}
			if (s.edges[0].size() == 1) {
				ScafEdge e = s.edges[0].get(0);
				if (e.x.vertecies.size() + e.y.vertecies.size() >= 5
						&& e.y.size() > 0) {
					unions.println("simple");
					merge(e);
					return true;
				}
				continue;
			}
			if (s.edges[1].size() == 1) {
				ScafEdge e = s.edges[1].get(0);
				if (e.x.vertecies.size() + e.y.vertecies.size() >= 5
						&& e.y.size() > 0) {
					unions.println("simple");
					merge(e);
					return true;
				}
				continue;
			}
			if (s.vertecies.size() > 1) {
				for (int i = 0; i < s.edges.length; i++) {
					int good = 0;
					ScafEdge ge = null;
					for (ScafEdge e : s.edges[i]) {
						if (e.y.size() == 0) {
							continue;
						}
						Vertex prev = null;
						if (i == 0) {
							prev = s.secondToFirst();
						} else {
							prev = s.secondToLast();
						}
						for (Edge ed : prev.edges) {
							if (ed.v2 == e.edge.v2) {
								good++;
								ge = e;
							}
						}
					}
					if (good == 1) {
						// System.err.println("new merge:\t" +
						// ge.edge.v1.info.id
						// + "\t" + ge.edge.v2.info.id + "\t"
						// + ge.edge.realDist());
						merge(ge);
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean merge(ScafEdge... edges) {
		boolean rev = false;
		for (ScafEdge e : edges) {
			if (e.ex == 0) {
				if (!rev) {
					e.x.reverse();
					e.ex = 1;
				}
			}
			if (e.ey == 1) {
				rev = true;
				e.y.reverse();
				e.ey = 0;
			}
		}
		unions.println("before");
		for (int i = 0; i < edges.length; i++) {
			if (i == 0) {
				unions.println(edges[i].x);
			}
			unions.println("\tunion: " + edges[i].x.id + "\t" + edges[i].y.id
					+ "\t" + edges[i].edge.v1.info.id + "\t"
					+ edges[i].edge.v2.info.id + "\t"
					+ edges[i].x.vertecies.size() + "\t"
					+ edges[i].y.vertecies.size() + "\t" + edges[i].edge + "\t"
					+ edges[i].ex + "\t" + edges[i].ey);
			unions.println();
			unions.println(edges[i].y);
		}
		unions.flush();
		Scaffold s = null;
		for (int i = 0; i < edges.length; i++) {
			ScafEdge e = edges[i];
			if (s == null) {
				s = e.x;
			}
			if (s.id == e.y.id) {
				unions.println("wrong: " + s + "\t" + e.y);
				unions.flush();
				throw new Error();
			}
			if (s.size() > 1) {
				if (e.ex == 0 && e.edge.v1.isSecondToFirst()
						&& e.edge.len < s.first().info.len) {
					s.swapFirst();
				} else if (e.ex == 1 && e.edge.v1.isSecondToLast()
						&& e.edge.len < s.last().info.len) {
					s.swapLast();
				}
			}
			if (e.y.size() > 1) {
				if (e.ey == 0 && e.edge.v2.isSecondToFirst()
						&& e.edge.len < e.y.first().info.len) {
					e.y.swapFirst();
				} else if (e.ey == 1 && e.edge.v2.isSecondToLast()
						&& e.edge.len < e.y.last().info.len) {
					e.y.swapLast();
				}
			}
			while (e.y.size() > 0) {
				s.addLast(e.y.pollFirst());
			}
			e.y.bindVertecies();
			s.bindVertecies();
		}
		unions.println("after:");
		unions.println(s);
		unions.flush();
		return true;
	}

	private static void filterOnlyBig(ArrayList<ScafEdge> arrayList) {
		int big = 0;
		for (ScafEdge e : arrayList) {
			if (e.y.vertecies.size() > 1) {
				big++;
			}
		}
		if (big == 1) {
			for (ListIterator<ScafEdge> it = arrayList.listIterator(); it
					.hasNext();) {
				if (it.next().y.vertecies.size() < 2) {
					it.remove();
				}
			}
		}
	}

	static int newSing = 0;

	// private static void bfs(Vertex start) {
	// Queue<Vertex> q = new ArrayDeque<>();
	// q.add(start);
	// while (!q.isEmpty()) {
	// Vertex v = q.poll();
	// for (Edge e : v.edges) {
	// if (!e.ghost && e.v2.d > e.v1.d + 1) {
	// e.v2.d = e.v1.d + 1;
	// q.add(e.v2);
	// }
	// }
	// }
	// }

}
