package ru.ifmo.genetics.tools.scaffolder;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sound.midi.MidiDevice.Info;

import org.apache.commons.math.MathException;

public class GraphBuilder {
	static List<Edge> allEdges;

	public static Vertex[] buildGraph(Library[] libraries)
			throws FileNotFoundException, MathException {
		System.err.println("building graph");
		allEdges = new ArrayList<Edge>();
		Vertex[] g = new Vertex[Contig.getInfo().size()];

		Map<InfoPair, Collection<MatePair>> map = new TreeMap<InfoPair, Collection<MatePair>>();

		for (Library lib : libraries) {
			for (Map.Entry<InfoPair, Collection<MatePair>> en : lib.map
					.entrySet()) {
				if (!map.containsKey(en.getKey())) {
					map.put(en.getKey(), new ArrayList<MatePair>());
				}
				map.get(en.getKey()).addAll(en.getValue());
			}
		}

		System.err.println("libraries remapped");

		loop: for (InfoPair pair : map.keySet()) {
			if (pair.s1.id < pair.s2.id) {
				Edge e = new Edge(pair.s1.v, pair.s2.v, 0, 0);
				if (map.get(pair).size() < 3) {
					continue;
				}
				int cnt = 0;
				cnt += map.get(pair).size();
				MatePair[] pp = new MatePair[cnt];
				cnt = 0;
				for (MatePair p : map.get(pair)) {
					pp[cnt++] = p;
					if (p.getD1() > p.lib.insertSize + 3 * p.lib.deviation
							|| p.getD2() > p.lib.insertSize + 3
									* p.lib.deviation) {
						continue loop;
					}
				}
				e.setReads(pp);
				// e.setReverse(rev);

				e.v1.edges.add(e);
				e.rev().v1.edges.add(e.rev());
				if (e.rev().rev() != e) {
					System.err.println("Oops!");
					throw new Error();
				}
			}
		}
		for (Contig ci : Contig.getInfo().values()) {
			g[ci.id] = ci.v;
		}

		for (Vertex v : g) {
			for (Edge e : v.edges) {
				if (e.v1 != v) {
					System.err.println("Oops!");
					throw new Error();
				}
			}
		}

		for (Vertex v : g) {
			for (Edge e : v.edges) {
				if (e.v1 != v) {
					System.err.println("Oops!");
					throw new Error();
				}
			}
		}

		// flipContigs(g);

		// for (Vertex v : g) {
		// for (Edge e : v.edges) {
		// e.len = DistanceFinder.getMostProbableDistance(e.v1.info.len,
		// e.v2.info.len, e.getD1(), e.getD2(), dnaLength,
		// allReads);
		// double sq = 0;
		// int[] d1 = e.getD1();
		// int[] d2 = e.getD2();
		// for (int i = 0; i < d1.length; i++) {
		// int len = DistanceFinder.getMostProbableDistance(
		// e.v1.info.len, e.v2.info.len, new int[] { d1[i] },
		// new int[] { d2[i] }, dnaLength, allReads
		// - d1.length + 1);
		// sq += (len - e.len) * (len - e.len);
		// }
		// e.err = (int) Math.round(Math.sqrt(sq / (d1.length - 1)) * 2.1);
		// }
		// }

		for (Vertex v : g) {
			for (ListIterator<Edge> li = v.edges.listIterator(); li.hasNext();) {
				Edge e = li.next();
				if (e.v1 == e.v2) {
					li.remove();
				}
			}
		}

		for (Vertex v : g) {
			for (int i = 0; i < v.edges.size(); i++) {
				for (int j = 0; j < v.edges.size() && i < v.edges.size(); j++) {
					if (i == j) {
						continue;
					}
					if (v.edges.get(i) == v.edges.get(j)
							|| v.edges.get(i).v2 == v.edges.get(j).v2
							&& v.edges.get(i).err <= v.edges.get(j).err) {
						v.edges.remove(j);
						j--;
					}
				}
			}
		}
		for (Vertex v : g) {
			for (Edge e : v.edges) {
				if (e.v1 != v) {
					System.err.println("Oops!");
					throw new Error();
				}
				if (!e.v2.edges.contains(e.rev())) {
					e.v2.edges.add(e.rev());
				}
				if (e.rev().rev() != e) {
					System.err.println("Oops!");
					throw new Error();
				}
			}
		}

		for (Vertex v : g) {
			for (Edge e : v.edges) {
				DistanceFinder.setMostProbableDistance(e);
				// e.len = e.getAvLen();
				// e.err = 0;
				// for (Pair p : e.pairs) {
				// e.err += (Data.NORMAL_DISTRIBUTION_CENTER - p.getD1()
				// - p.getD2() - 2 * Data.READ_LENGTH - e.len)
				// * (Data.NORMAL_DISTRIBUTION_CENTER - p.getD1()
				// - p.getD2() - 2 * Data.READ_LENGTH - e.len);
				// }
				// e.err = (int) Math.round(Math.sqrt(e.err / e.pairs.length));
			}
		}

		int reads = 0;
		for (Vertex v : g) {
			for (Edge e : v.edges) {
				reads += e.pairs.length;
			}
		}

		System.err.println("tmp reads1: " + reads / 2);

		// addGhostEdges(g);

		// System.err.println("good/bad str: " + Edge.goodStrTrue + "/"
		// + Edge.goodStrFalse + "/" + Edge.badStrTrue + "/"
		// + Edge.badStrFalse);

		reads = 0;
		for (Vertex v : g) {
			for (Edge e : v.edges) {
				reads += e.pairs.length;
			}
		}

		System.err.println("tmp reads2: " + reads / 2);

		System.err.println("Libraries:");

		for (int i = 0; i < libraries.length; i++) {
			System.err.println("\t" + libraries[i].size() + ":\t"
					+ libraries[i].insertSize + "\t" + libraries[i].deviation
					+ "\t" + libraries[i].readLength);
		}

		for (Vertex v : g) {
			for (Edge e : v.edges) {
				e.setGood(false);
			}
		}

		// for (Vertex v : g) {
		// for (Edge e1 : v.edges) {
		// if (e1.v1.info.id > e1.v2.info.id) {
		// continue;
		// }
		// for (Edge e2 : v.edges) {
		// if (e2 == e1) {
		// break;
		// }
		// for (Edge e3 : e1.v2.edges) {
		// if (e3.v2 != e2.v2) {
		// continue;
		// }
		// Edge max = e1;
		// Edge o1 = e2;
		// Edge o2 = e3;
		// if (max.len < o1.len) {
		// Edge tmp = max;
		// max = o1;
		// o1 = tmp;
		// }
		// if (max.len < o2.len) {
		// Edge tmp = max;
		// max = o2;
		// o2 = tmp;
		// }
		// Vertex mid = null;
		// if (o1.v1 == o2.v1 || o1.v1 == o2.v2) {
		// mid = o1.v1;
		// } else {
		// mid = o2.v2;
		// }
		// if (Math.abs(max.len - (o1.len + mid.info.len + o2.len)) <
		// Data.getMaxDeviation()) {
		// max.setGood(true);
		// o1.setGood(true);
		// o2.setGood(true);
		// }
		// }
		// }
		// }
		// }

//		for (Vertex v : g) {
//			for (ListIterator<Edge> li = v.edges.listIterator(); li.hasNext();) {
//				Edge e = li.next();
//				double expect = 0;
//				for (Library lib : Data.libraries) {
//					expect += DistanceFinder
//							.getProbabilityThatAtLeastOneMatepairMatches(e.len,
//									lib, e.v1.info.len, e.v2.info.len)
//							* lib.size();
//				}
//				double ratio = e.pairs.length / expect;
//				if (!e.good && !e.ghost && (ratio < 0.6 || ratio > 0.9)) {
//					System.err.println("filter: " + e.pairs.length + "\t"
//							+ expect + "\t" + ratio + "\t" + e);
//					li.remove();
//					e.v2.edges.remove(e.rev());
//					continue;
//				}
//				for (MatePair mp : e.pairs) {
//					if (e.len > mp.lib.insertSize - mp.lib.deviation
//							|| e.len < -mp.lib.deviation) {
//						li.remove();
//						break;
//					}
//				}
//			}
//		}
		reads = 0;
		for (Vertex v : g) {
			for (Edge e : v.edges) {
				reads += e.pairs.length;
			}
		}

		System.err.println("tmp reads3: " + reads / 2);

		int edgecnt = 0;
		DistanceFinder.nonconvex = null;
		for (Vertex v : g) {
			for (Edge e : v.edges) {
				if (e.v2.info.id < e.v1.info.id
				/*
				 * || e.realDist() > Data.NORMAL_DISTRIBUTION_CENTER + 3
				 * Data.NORMAL_DISTRIBUTION_DEVIATION
				 */) {
					continue;
				}
				DistanceFinder.setMostProbableDistance(e);
				edgecnt++;
			}
		}

		System.err.println("edgecnt: " + edgecnt);

		for (Vertex v : g) {
			for (Edge e : v.edges) {

				allEdges.add(e);
			}
		}
		// GraphFilter.filter(g);
		double minInsertSize = Integer.MAX_VALUE;
		double maxDeviation = Integer.MIN_VALUE;
		for (Library lib : libraries) {
			minInsertSize = Math.min(minInsertSize, lib.insertSize);
			maxDeviation = Math.max(maxDeviation, lib.deviation);
		}
		// GraphFilter.removeShortContigs(g, Data.getMaxInsertSize() / 3);
		GraphFilter.removeOvercovered(g, Data.getOneCover());
		GraphFilter.removePopular(g, 3);
		// GraphFilter.removeShortEdges(g, Data.getMinInsertSize() + 3 *
		// Data.getMaxDeviation());

		reads = 0;
		for (Vertex v : g) {
			for (Edge e : v.edges) {
				reads += e.pairs.length;
			}
		}

		System.err.println("tmp reads: " + reads / 2);

		printGraph(g);

		printEdgesInfo(g, minInsertSize, maxDeviation);

		// printTripletInfo(g);

		return g;

	}

	private static void addGhostEdges(Vertex[] g) throws MathException {
		int ghost = 0;
		for (Vertex v : g) {
			// System.err.println("Vertex #" + v.info.id);
			for (int i = 0; i < v.edges.size(); i++) {
				Edge e1 = v.edges.get(i);
				if (e1.ghost) {
					continue;
				}
				for (int j = 0; j < i; j++) {
					Edge e2 = v.edges.get(j);
					if (e2.ghost) {
						continue;
					}
					Edge e3 = null;
					for (Edge e : e1.v2.edges) {
						if (e.v2 == e2.v2) {
							e3 = e;
						}
					}
					if (v.info.id == 300
							&& (e1.v2.info.id == 301 || e1.v2.info.id == 302)) {
						System.err.println(e1.v1.info.id + "\t" + e1.v2.info.id
								+ "\t" + e1);
						System.err.println(e2.v1.info.id + "\t" + e2.v2.info.id
								+ "\t" + e2);
						System.err.println(e3);
					}
					if (e3 != null) {
						continue;
					}
					e3 = new Edge(e1.v2, e2.v2, e1.len > e2.len ? e1.len
							- e2.len - e2.v2.info.len : e2.len - e1.len
							- e1.v2.info.len, 0);
					e3.ghost = true;
					e3.pairs = new MatePair[0];

					double e = 0;

					for (Library lib : Data.libraries) {
						e += DistanceFinder
								.getProbabilityThatAtLeastOneMatepairMatches(
										e3.len, lib, e3.v1.info.len,
										e3.v2.info.len)
								* lib.size();
					}

					if (e > 3) {
						continue;
					}

					// if (-2 * e3.len < Math.min(e3.v1.info.len,
					// e3.v2.info.len)) {
					e3.v1.edges.add(e3);
					e3.v2.edges.add(e3.rev());
					ghost++;
					// }
					// if (-2 * e4.len < Math.min(e4.v1.info.len,
					// e4.v2.info.len)) {
					// }
					// if (ghost % 100 == 0) {
					// System.err.println(ghost + " edges added");
					// }
				}
			}
		}
		System.err.println("ghost: " + ghost);
	}

	/*
	 * private static void printTripletInfo(Vertex[] g) throws
	 * FileNotFoundException, MathException {
	 * 
	 * PrintWriter triplets = new PrintWriter("triplets"); int maxD = 1000; int
	 * dStep = 10; int cnt = 0; loop: for (Vertex v : g) { for (int i = 0; i <
	 * v.edges.size(); i++) { Edge e1 = v.edges.get(i); for (int j = 0; j < i;
	 * j++) { Edge e2 = v.edges.get(j); Edge e3 = null; for (Edge e :
	 * e1.v2.edges) { if (e.v2 == e2.v2) { e3 = e; } } cnt++; if (cnt >= 10) {
	 * break loop; } triplets.print(e1.v2.info.name + "\t" + v.info.name + "\t"
	 * + e2.v2.info.name); triplets.println("\t" + (e3 == null ? "no" : "yes"));
	 * for (int d1 = 0; d1 <= maxD; d1 += dStep) { for (int d2 = 0; d2 <= maxD;
	 * d2 += dStep) { double p = 0; p += (e1.dSq * d1 + e1.dLin) * d1 + e1.dCon;
	 * p += (e2.dSq * d2 + e2.dLin) * d2 + e2.dCon; if (e3 != null) { p +=
	 * (e3.dSq * (d1 + v.info.len + d2) + e3.dLin) (d1 + v.info.len + d2) +
	 * e3.dCon; } double q = 0; q += DistanceFinder
	 * .getProbabilityThatAtLeastOneMatepairMatches( d1, e1.v2.info.len,
	 * v.info.len); q += DistanceFinder
	 * .getProbabilityThatAtLeastOneMatepairMatches( d2, e2.v2.info.len,
	 * v.info.len); q += DistanceFinder
	 * .getProbabilityThatAtLeastOneMatepairMatches( d1 + v.info.len + d2,
	 * e2.v2.info.len, e1.v2.info.len); int reads = 0; reads += e1.pairs.length;
	 * reads += e2.pairs.length; if (e3 != null) { reads += e3.pairs.length; }
	 * double ans = p + (Data.allReads - reads) Math.log(1 - q);
	 * triplets.print((ans + "\t").replace('.', ',')); } triplets.println(); } }
	 * } }
	 * 
	 * triplets.close(); }
	 */

	private static void printEdgesInfo(Vertex[] g, double minInsertSize,
			double maxDeviation) throws FileNotFoundException, MathException {
		{
			int all = 0;
			int ok1 = 0;
			int ok4 = 0;
			int ok2 = 0;
			int ok3 = 0;
			int better = 0;
			int same = 0;
			int worse = 0;
			boolean printedGraph = false;
			Map<String, TreeSet<String>> set = new TreeMap<String, TreeSet<String>>();
			PrintWriter allEdges = new PrintWriter("all_edges");
			PrintWriter badEdges = new PrintWriter("bad_edges");
			PrintWriter goodEdges = new PrintWriter("good_edges");
			PrintWriter qValues = new PrintWriter("q_values");
			PrintWriter out = new PrintWriter("rand");
			int shiftL = 0;
			int shiftR = 0;
			int superbad = 20;
			int supergood = 13;
			PrintWriter sb = new PrintWriter("superbad");
			for (Edge e : GraphBuilder.allEdges) {
				if (e.v1.info.id >= e.v2.info.id) {
					continue;
				}
				if (!set.containsKey(e.v1.info.name)) {
					set.put(e.v1.info.name, new TreeSet<String>());
				}
				if (set.get(e.v1.info.name).contains(e.v2.info.name)) {
					continue;
				}
				if (e.ghost) {
					continue;
				}
				if (superbad > 0) {
					if (e.realDist() > 10000 && e.pairs.length > 10) {
						superbad--;
						if (superbad >= 0) {
							// System.err.println("SUPER BAD");
							sb.println(e.v1.info.name + "\t" + e.v2.info.name
									+ "\t" + e.v1.info.len + "\t"
									+ e.v2.info.len);
							for (MatePair p : e.pairs) {
								sb.println(p.getD1() + "\t" + p.getD2());
							}
						}
					}
				}
				if (supergood > 0) {
					if (Math.abs(e.realDist() - e.len) < 10
							&& e.pairs.length > 100 && e.pairs.length < 200) {
						supergood--;
						if (supergood == 0) {
							System.err.println("SUPER GOOD");
							PrintWriter sg = new PrintWriter("supergood");
							for (MatePair p : e.pairs) {
								sg.println(p.getD1() + "\t" + p.getD2());
							}
							sg.close();
						}
					}
				}
				set.get(e.v1.info.name).add(e.v2.info.name);
				// if (e.realDist() > minInsertSize + 3 * maxDeviation) {
				// continue;
				// }
				int swDist = SwedenDistanceFinder.getMostProbableDistance(
						e.v1.info.len, e.v2.info.len, e.getD1(), e.getD2(),
						Data.dnaLength, Data.allReads());
				if (Math.abs(swDist - e.realDist()) > Math.abs(e.len
						- e.realDist())) {
					ok1++;
				}
				if (Math.abs(swDist - e.realDist()) >= Math.abs(e.len
						- e.realDist())) {
					ok4++;
				}
				qValues.print(e.v1.info.name + "\t" + e.v2.info.name + "\t"
						+ e.v1.info.id + "\t" + e.v2.info.id + "\t"
						+ e.pairs.length);
				for (int i = -1000; i <= 1000; i += 50) {
					for (Library lib : Data.libraries) {
						double q = DistanceFinder
								.getProbabilityThatAtLeastOneMatepairMatches(
										e.len + i, lib, e.v1.info.len,
										e.v2.info.len);
						qValues.print("\t" + q);
					}
				}
				qValues.println();
				// double sigma = 0;
				// for (MatePair p : e.pairs) {
				// sigma += (Data.NORMAL_DISTRIBUTION_CENTER - 2
				// * Data.READ_LENGTH - p.getD1() - p.getD2() - e
				// .getAvLen())
				// * (Data.NORMAL_DISTRIBUTION_CENTER - 2
				// * Data.READ_LENGTH - p.getD1() - p.getD2() - e
				// .getAvLen());
				// }
				// sigma /= e.pairs.length - 1;
				allEdges.print(e.v1.info.name + "\t" + e.v2.info.name + "\t"
						+ e.v1.info.id + "\t" + e.v2.info.id

						+ "\t" + e.realDist() + "\t" + e.len + "\t" /*
																	 * + ol.len
																	 * + "\t" +
																	 * ol.err +
																	 * "\t"
																	 */
						+ e.getAvLen() + "\t" + swDist + "\t" + e.v1.info.len
						+ "\t" + e.v2.info.len);
				allEdges.print("\t");
				allEdges.print(DistanceFinder
						.getProbabilityThatAllMatepairsMatch(e.len,
								e.v1.info.len, e.v2.info.len, e.pairs.length,
								e.dSq, e.dLin, e.dCon, e.cnt)
						+ "\t");
				allEdges.print(e.pairs.length + "\t");
				double expect = 0;
				for (Library lib : Data.libraries) {
					expect += DistanceFinder
							.getProbabilityThatAtLeastOneMatepairMatches(e.len,
									lib, e.v1.info.len, e.v2.info.len)
							* lib.size();
				}
				double ratio = e.pairs.length / expect;
				allEdges.print(expect + "\t" + ratio + "\t");
				all++;
				if (Math.abs(e.getAvLen() - e.realDist()) >= Math.abs(e.len
						- e.realDist())) {
					goodEdges.println(e.v1.info.name + "\t" + e.v2.info.name
							+ "\t" + e.v1.info.len + "\t" + e.v2.info.len
							+ "\t" + e.realDist() + "\t" + e.len + "\t"
							+ e.getAvLen() + "\t" + e.pairs.length);
					allEdges.print("\tok");
					ok3++;
					if (Math.abs(e.getAvLen() - e.realDist()) > Math.abs(e.len
							- e.realDist())) {
						ok2++;
					}
				} else {
					allEdges.print("\t");
					badEdges.println(e.v1.info.name + "\t" + e.v2.info.name
							+ "\t" + e.v1.info.len + "\t" + e.v2.info.len
							+ "\t" + e.realDist() + "\t" + e.len + "\t"
							+ e.getAvLen() + "\t" + e.pairs.length);
				}
				allEdges.print("\t" + e.pairs.length);
				if (e.getAvLen() != e.len
						&& Math.abs(e.getAvLen() - e.realDist()) > Math
								.abs(e.len - e.realDist())
						&& e.realDist() < 3000 && e.realDist() > 0
						&& !printedGraph) {
					printedGraph = true;
					PrintWriter graph = new PrintWriter("loglike");
					for (int i = -1000; i <= 5000; i++) {
						double d = DistanceFinder
								.getProbabilityThatAllMatepairsMatch(i,
										e.v1.info.len, e.v2.info.len,
										e.pairs.length, e.dSq, e.dLin, e.dCon,
										e.cnt);
						graph.printf("%d\t%.2f\n", i, d);
					}
					graph.close();
				}

				double sumD1 = 0;
				double sumD2 = 0;
				for (MatePair p : e.pairs) {
					sumD1 += p.getD1();
					sumD2 += p.getD2();
				}
				sumD1 /= e.pairs.length;
				sumD2 /= e.pairs.length;
				double disD1 = 0;
				double disD2 = 0;
				for (MatePair p : e.pairs) {
					disD1 += (p.getD1() - sumD1) * (p.getD1() - sumD1);
					disD2 += (p.getD2() - sumD2) * (p.getD2() - sumD2);
				}
				disD1 /= e.pairs.length;
				disD2 /= e.pairs.length;
				allEdges.println("\t" + sumD1 + "\t" + sumD2 + "\t"
						+ Math.sqrt(disD1) + "\t" + Math.sqrt(disD2));
				// System.err.println("\t" + e.err + "\t" + e.rev1 + "\t"
				// + e.rev2 + "\t" + qdist1 + "\t" + qdist2);
				// DistanceFinder.newFix = true;
				// DistanceFinder.setMostProbableDistance(e);
				// int dF = e.len;
				// DistanceFinder.newFix = false;
				// DistanceFinder.setMostProbableDistance(e);
				// if (Math.abs(dF - e.realDist()) < Math
				// .abs(e.len - e.realDist())) {
				// better++;
				// } else if (Math.abs(dF - e.realDist()) < Math.abs(e.len
				// - e.realDist())) {
				// same++;
				// } else {
				// worse++;
				// }
			}
			allEdges.println(ok2 + ", " + ok3 + " / " + all);
			System.err.println(ok2 + ", " + ok3 + " / " + all);
			System.err.println(ok1 + ", " + ok4 + " / " + all);
			// int sum = 0;
			// int sumAv = 0;
			// int sumP = 0;
			// int sumPAv = 0;
			// int sumOp = 0;
			// for (int i = 0; i < cnt.length; i++) {
			// sum += cnt[i];
			// sumAv += cntAv[i];
			// sumP += percent[i];
			// sumPAv += percentAv[i];
			// sumOp += cntOp[i];
			// allEdges.println(i + "\t" + sum + "\t" + sumAv + "\t" + sumP
			// + "\t" + sumPAv + "\t" + sumOp);
			// }

			double sumDiff = 0;
			double sumDiffAv = 0;
			double sumDiffSw = 0;
			double usumDiff = 0;
			double usumDiffAv = 0;
			double usumDiffSw = 0;
			int cnt = 0;

			for (Edge e : GraphBuilder.allEdges) {
				if (e.v1.info.id >= e.v2.info.id) {
					continue;
				}
				if (e.ghost) {
					continue;
				}
				if (e.realDist() > minInsertSize + 3 * maxDeviation) {
					continue;
				}
				sumDiff += e.len - e.realDist();
				usumDiff += Math.abs(e.len - e.realDist());
				sumDiffAv += e.getAvLen() - e.realDist();
				usumDiffAv += Math.abs(e.getAvLen() - e.realDist());
				{
					int swLen = SwedenDistanceFinder.getMostProbableDistance(
							e.v1.info.len, e.v2.info.len, e.getD1(), e.getD2(),
							Data.dnaLength, Data.allReads());
					sumDiffSw += swLen - e.realDist();
					usumDiffSw += Math.abs(swLen - e.realDist());
				}
				cnt++;
			}
			sumDiff /= cnt;
			sumDiffAv /= cnt;
			sumDiffSw /= cnt;
			usumDiff /= cnt;
			usumDiffAv /= cnt;
			usumDiffSw /= cnt;
			double div = 0;
			double divAv = 0;
			double divSw = 0;
			double udiv = 0;
			double udivAv = 0;
			double udivSw = 0;
			for (Edge e : GraphBuilder.allEdges) {
				if (e.v1.info.id >= e.v2.info.id) {
					continue;
				}
				if (e.ghost) {
					continue;
				}
				if (e.realDist() > minInsertSize + 3 * maxDeviation) {
					continue;
				}
				div += Math.abs(e.len - e.realDist() - sumDiff);
				udiv += Math.abs(Math.abs(e.len - e.realDist()) - usumDiff);
				divAv += Math.abs(e.getAvLen() - e.realDist() - sumDiffAv);
				udivAv += Math.abs(Math.abs(e.getAvLen() - e.realDist())
						- usumDiffAv);
				{
					int swLen = SwedenDistanceFinder.getMostProbableDistance(
							e.v1.info.len, e.v2.info.len, e.getD1(), e.getD2(),
							Data.dnaLength, Data.allReads());
					divSw += Math.abs(swLen - e.realDist() - sumDiffSw);
					udivSw += Math.abs(Math.abs(swLen - e.realDist())
							- usumDiffSw);
				}
			}
			allEdges.println("our dif:\t" + sumDiff + "\t"
					+ Math.sqrt(div / cnt) + "\t" + usumDiff + "\t"
					+ Math.sqrt(udiv / cnt));
			allEdges.println("avrg dif:\t" + sumDiffAv + "\t"
					+ Math.sqrt(divAv / cnt) + "\t" + usumDiffAv + "\t"
					+ Math.sqrt(udivAv / cnt));
			allEdges.println("swe dif:\t" + sumDiffSw + "\t"
					+ Math.sqrt(divSw / cnt) + "\t" + usumDiffSw + "\t"
					+ Math.sqrt(udivSw / cnt));
			allEdges.println("better: " + better);
			allEdges.println("same: " + same);
			allEdges.println("worse: " + worse);

			int sumLen = 0;
			List<Vertex> al = new ArrayList<Vertex>();
			for (Vertex v : g) {
				sumLen += v.info.len;
				al.add(v);
			}
			Collections.sort(al, new Comparator<Vertex>() {

				@Override
				public int compare(Vertex o1, Vertex o2) {
					return o1.info.len - o2.info.len;
				}
			});

			allEdges.println("max len:\t" + al.get(al.size() - 1).info.len);
			allEdges.println("mean len:\t" + (1.0 * sumLen / al.size()));

			int sum = 0;
			for (int i = al.size() - 1; i >= 0; i--) {
				sum += al.get(i).info.len;
				if (2 * sum >= sumLen) {
					allEdges.println("N50:\t" + al.get(i).info.len);
					break;
				}
			}

			sb.close();
			qValues.close();
			allEdges.close();
			badEdges.close();
			goodEdges.close();
			out.close();
			System.err.println("shiftL: " + shiftL);
			System.err.println("shiftR: " + shiftR);
		}
	}

	private static void printGraph(Vertex[] g) throws FileNotFoundException {
		PrintWriter graph = new PrintWriter("graph");

		graph.println("graph G {");
		graph.println("\tnode [shape=circle];");
		for (Contig ci : Contig.getInfo().values()) {
			if (ci.v.edges.size() == 0/* || ci.id > 250 */) {
				continue;
			}
			graph.println("\t" + ci.name + " [label = <" + ci.id + ", "
					+ ci.len + ">];");
		}
		Map<Integer, TreeSet<Integer>> hs = new TreeMap<Integer, TreeSet<Integer>>();
		for (Vertex v : g) {
			for (Edge e : v.edges) {
				// if (e.v1.info.id > 250 || e.v2.info.id > 250) {
				// continue;
				// }
				if (e.v1.info.id < e.v2.info.id
				/*
				 * && (!hs.containsKey(e.v1.info.id) || !hs.get(
				 * e.v1.info.id).contains(e.v2.info.id))
				 */) {
					String name1 = e.v1.info.name;
					String name2 = e.v2.info.name;
					graph.println("\t"
							+ name1
							+ " -- "
							+ name2
							+ " [label = <"
							+ e.len
							+ ", "
							+ (e.realDist())
							+ ">"
							+ (Math.abs(e.v1.info.id - e.v2.info.id) == 1 ? ", color = red"
									: "") + (", style = " + e.getStyle())
							+ "];");
					if (!hs.containsKey(e.v1.info.id)) {
						hs.put(e.v1.info.id, new TreeSet<Integer>());
					}
					hs.get(e.v1.info.id).add(e.v2.info.id);
				}
			}
		}
		graph.println("}");
		graph.close();

		System.err.println("Graph printed");
	}

	public static void restoreEdges(Vertex[] g, boolean allowGhosts) {
		for (Vertex v : g) {
			v.edges.clear();
		}
		for (Edge e : allEdges) {
			if (!allowGhosts && e.ghost) {
				continue;
			}
			e.v1.edges.add(e);
			// e.v2.edges.add(e.rev());
		}
		System.err.println("edges restored");
	}
}
