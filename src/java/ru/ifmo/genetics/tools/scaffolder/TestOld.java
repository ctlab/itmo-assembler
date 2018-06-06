/*package ru.ifmo.genetics.tools.scaffolder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.apache.commons.math.MathException;


public class TestOld {

	public static void main(String[] args) throws IOException, MathException {
		Locale.setDefault(Locale.US);
		HashMap<String, ArrayList<SAMAlignment>> mapa = toMap(SAMParser
				.parse(args[0]));
		HashMap<String, ArrayList<SAMAlignment>> mapb = toMap(SAMParser
				.parse(args[1]));
		Data.dnaLength = Integer.parseInt(args[2]);
		loadContigs(args[3]);
		if (args.length > 4) {
			System.out.println("loading positions:");
			HashMap<String, ArrayList<BlastAlignment>> contigMap = toMap(BlastParser
					.parse(args[4]));
			for (String s : contigMap.keySet()) {
				String name = transform(s);
				ContigInfo ci = getInfo(name);
				BlastAlignment al = contigMap.get(s).get(0);
				ci.realPos = (al.sstart + al.send) / 2;
				ci.realRev = al.sstart > al.send;
				System.out.println(s + "\t" + name + "\t" + al.sstart + "\t"
						+ ci.len);
			}
			ArrayList<ContigInfo> list = new ArrayList<ContigInfo>();
			list.addAll(contigInfo.values());
			Collections.sort(list, new Comparator<ContigInfo>() {

				@Override
				public int compare(ContigInfo o1, ContigInfo o2) {
					return o1.realPos - o2.realPos;
				}
			});
			for (int i = 0; i < list.size(); i++) {
				list.get(i).id = i;
			}
		}
		{
			int all = 0;
			int ok = 0;
			for (ContigInfo ci : contigInfo.values()) {
				if (ci.realPos >= 0) {
					ok++;
				} else {
					System.out.println(ci.name);
				}
				all++;
			}
			System.out.println(ok + " out of " + all
					+ " contigs matched reference");
		}
		Data.allReads = Math.max(mapa.size(), mapb.size());
		HashMap<InfoPair, List<Pair>> map = new HashMap<InfoPair, List<Pair>>();
		for (String s1 : mapa.keySet()) {
			String s2 = s1.substring(0, s1.length() - 1) + "2";
			if (!mapb.containsKey(s2) || mapa.get(s1).size() > 1
					|| mapb.get(s2).size() > 1) {
				continue;
			}
			for (SAMAlignment a : mapa.get(s1)) {
				for (SAMAlignment b : mapb.get(s2)) {
					if (a.rname.equals(b.rname)) {
						continue;
					}
					InfoPair sp = new InfoPair(a.rname, b.rname);
					if (!map.containsKey(sp)) {
						InfoPair rev = sp.reverse();
						map.put(sp, new ArrayList<Pair>());
						map.put(rev, new ArrayList<Pair>());
					}
					map.get(sp).add(new Pair(a, b));
					map.get(sp.reverse()).add(new Pair(b, a));
				}
			}
		}

		Set<InfoPair> pairs = map.keySet();

		Vertex[] g = new Vertex[contigInfo.size()];
		for (InfoPair pair : pairs) {
			if (pair.s1.id < pair.s2.id) {
				Edge e = new Edge(pair.s1.v, pair.s2.v, 0, 0);
				if (map.get(pair).size() < 3) {
					continue;
				}
				int cnt = 0;
				int rev = 0;
				for (Pair p : map.get(pair)) {
					cnt++;
					if (p.isReverse()) {
						rev--;
					} else {
						rev++;
					}
				}
				Pair[] pp = new Pair[cnt];
				cnt = 0;
				for (Pair p : map.get(pair)) {
					pp[cnt++] = p;
				}
				e.setReads(pp);
				e.setReverse(rev);

				e.v1.edges.add(e);
				e.rev().v1.edges.add(e.rev());
				if (e.rev().rev() != e) {
					System.err.println("Oops!");
					throw new Error();
				}
			}
		}

		for (ContigInfo ci : contigInfo.values()) {
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
			if (v.edges.size() > 4) {
				remove(v);
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
				int mpd = DistanceFinder.getMostProbableDistance(e.v1.info.len,
						e.v2.info.len, e.getD1(), e.getD2());
				e.setLen(mpd);
			}
		}

		for (Vertex v : g) {
			for (ListIterator<Edge> li = v.edges.listIterator(); li.hasNext();) {
				if (li.next().len > Data.NORMAL_DISTRIBUTION_CENTER + 5
						* Data.NORMAL_DISTRIBUTION_DEVIATION) {
					li.remove();
				}
			}
		}

		Data.allReads = 0;
		for (Vertex v : g) {
			for (Edge e : v.edges) {
				Data.allReads += e.pairs.length;
			}
		}

		{
			int[] cnt = new int[500];
			int[] cntAv = new int[500];
			int[] cntOp = new int[500];
			int[] percent = new int[500];
			int[] percentAv = new int[500];
			int all = 0;
			int ok2 = 0;
			int ok3 = 0;
			boolean printedGraph = false;
			HashMap<String, HashSet<String>> set = new HashMap<String, HashSet<String>>();
			PrintWriter allEdges = new PrintWriter("all_edges");
			PrintWriter badEdges = new PrintWriter("bad_edges");
			PrintWriter goodEdges = new PrintWriter("good_edges");
			PrintWriter qValues = new PrintWriter("q_values");
			// for (OperaLine ol : opera) {
			for (Vertex v : g) {
				for (Edge e :  getInfo(transform(ol.name1)). v.edges) {
					if (e.v1.info.id >= e.v2.info.id
													 * !e.v2.info.name.equals(
													 * transform(ol.name2))
													 
					) {
						continue;
					}
					if (!set.containsKey(e.v1.info.name)) {
						set.put(e.v1.info.name, new HashSet<String>());
					}
					if (set.get(e.v1.info.name).contains(e.v2.info.name)) {
						continue;
					}
					set.get(e.v1.info.name).add(e.v2.info.name);
					int realDist = Math.abs(e.v1.info.realPos
							- e.v2.info.realPos)
							- (e.v1.info.len + e.v2.info.len) / 2;
					if (realDist > 10000) {
						continue;
					}
					qValues.print(e.v1.info.name + "\t" + e.v2.info.name + "\t"
							+ e.v1.info.id + "\t" + e.v2.info.id + "\t"
							+ e.pairs.length);
					for (int i = -1000; i <= 1000; i += 50) {
						double q = DistanceFinder
								.getProbabilityThatAtLeastOneMatepairMatches(
										e.len + i, e.v1.info.len, e.v2.info.len);
						qValues.print("\t" + q);
					}
					qValues.println();
					if (e.v1.info.name.equals("contig437")
							&& e.v2.info.name.equals("contig16")) {
						System.out.println("contig437	contig16");
						for (Pair p : e.pairs) {
							System.out.println(p.getD1() + "\t" + p.getD2());
						}
					}
					double sigma = 0;
					e.av_len = 0;
					for (Pair p : e.pairs) {
						e.av_len += Data.NORMAL_DISTRIBUTION_CENTER - 2
								* Data.READ_LENGTH - p.getD1() - p.getD2();
					}
					e.av_len /= e.pairs.length;
					for (Pair p : e.pairs) {
						sigma += (Data.NORMAL_DISTRIBUTION_CENTER - 2
								* Data.READ_LENGTH - p.getD1() - p.getD2() - e.av_len)
								* (Data.NORMAL_DISTRIBUTION_CENTER - 2
										* Data.READ_LENGTH - p.getD1()
										- p.getD2() - e.av_len);
					}
					sigma /= e.pairs.length - 1;
					allEdges.print(e.v1.info.name + "\t" + e.v2.info.name
							+ "\t" + realDist + "\t" + e.len + "\t" 
																	 * + ol.len
																	 * + "\t" +
																	 * ol.err +
																	 * "\t"
																	 
							+ e.av_len);
					allEdges.print("\t");
					allEdges.print(DistanceFinder
							.getProbabilityThatAllMatepairsMatch(e.len,
									e.v1.info.len, e.v2.info.len,
									e.pairs.length, e.dSq, e.dLin, e.dCon)
							+ "\t");
					allEdges.print(e.pairs.length + "\t");
					allEdges.print(DistanceFinder
							.getProbabilityThatAtLeastOneMatepairMatches(
									realDist, e.v1.info.len, e.v2.info.len)
							* Data.allReads + "\t");
					// for (OperaLine ol : opera) {
					// if (ol.name1.equals(e.v1.info.name)
					// && ol.name2.equals(e.v2.info.name)
					// || ol.name2.equals(e.v1.info.name)
					// && ol.name1.equals(e.v2.info.name)) {
					// int opDist = ol.len - getInfo(ol.name2).len;
					// allEdges.print(opDist);
					// if (Math.abs(realDist - opDist) < cntOp.length) {
					// cntOp[Math.abs(realDist - opDist)]++;
					// }
					// break;
					// }
					// }
					all++;
					
					 * if (Math.abs(ol.len - Math.abs(e.v1.info.realPos -
					 * e.v2.info.realPos)) < ol.err) { allEdges.print("\tok");
					 * ok2++; } else { allEdges.print("\t"); }
					 
					
					 * if (Math.abs(ol.len - Math.abs(e.v1.info.realPos -
					 * e.v2.info.realPos)) > Math .abs(e.len -
					 * Math.abs(e.v1.info.realPos - e.v2.info.realPos))) {
					 * allEdges.print("\tok"); ok++; } else {
					 * allEdges.print("\t"); }
					 
					if (Math.abs(e.av_len - realDist) >= Math.abs(e.len
							- realDist)) {
						goodEdges.println(e.v1.info.name + "\t"
								+ e.v2.info.name + "\t" + realDist + "\t"
								+ e.len + "\t" + e.av_len + "\t"
								+ e.pairs.length + "\t" + sigma);
						allEdges.print("\tok");
						ok3++;
						if (Math.abs(e.av_len - realDist) > Math.abs(e.len
								- realDist)) {
							ok2++;
						}
					} else {
						allEdges.print("\t");
						badEdges.println(e.v1.info.name + "\t" + e.v2.info.name
								+ "\t" + realDist + "\t" + e.len + "\t"
								+ e.av_len + "\t" + e.pairs.length + "\t"
								+ sigma);
					}
					allEdges.println("\t" + e.pairs.length);
					int diff = Math.abs(realDist - e.len);
					if (0 <= diff && diff < cnt.length) {
						cnt[diff]++;
					}
					int perc = Math.abs((int) Math.round(Math.ceil(100.0 * diff
							/ realDist)));
					if (perc < percent.length) {
						percent[perc]++;
					}
					int diffAv = Math.abs(realDist - e.av_len);
					if (0 <= diffAv && diffAv < cntAv.length) {
						cntAv[diffAv]++;
					}
					int percAv = Math.abs((int) Math.round(Math.ceil(100.0
							* diffAv / realDist)));
					if (percAv < percentAv.length) {
						percentAv[percAv]++;
					}

					if (e.av_len != e.len
							&& Math.abs(e.av_len - realDist) > Math.abs(e.len
									- realDist) && realDist < 3000
							&& realDist > 0 && !printedGraph) {
						printedGraph = true;
						PrintWriter graph = new PrintWriter("loglike");
						for (int i = -1000; i <= 5000; i++) {
							double d = DistanceFinder
									.getProbabilityThatAllMatepairsMatch(i,
											e.v1.info.len, e.v2.info.len,
											e.pairs.length, e.dSq, e.dLin,
											e.dCon);
							graph.printf("%d\t%.2f\n", i, d);
						}
						graph.close();
					}
				}
			}
			allEdges.println(ok2 + ", " + ok3 + " / " + all);
			int sum = 0;
			int sumAv = 0;
			int sumP = 0;
			int sumPAv = 0;
			int sumOp = 0;
			for (int i = 0; i < cnt.length; i++) {
				sum += cnt[i];
				sumAv += cntAv[i];
				sumP += percent[i];
				sumPAv += percentAv[i];
				sumOp += cntOp[i];
				allEdges.println(i + "\t" + sum + "\t" + sumAv + "\t" + sumP
						+ "\t" + sumPAv + "\t" + sumOp);
			}
			qValues.close();
			allEdges.close();
			badEdges.close();
			goodEdges.close();

		}
		printGraph(g);
		// filterError(g, 5, true);
		filterLen(g, 4, true);

		Data.allReads = 0;
		for (Vertex v : g) {
			for (Edge e : v.edges) {
				Data.allReads += e.pairs.length;
			}
		}

		Data.allReads /= 2;

		// printGraph(g);
		PrintWriter scaf = new PrintWriter("scaffolds");

		int cnt = 0;
		int skipped = 0;
		int maxlen = 0;
		int all = 0;
		int wrong = 0;
		int sumlen = 0;
		int sca = 0;

		while (true) {
			boolean change = false;
			for (Vertex start : g) {
				if (start.u) {
					continue;
				}
				change = true;
				cnt++;
				System.err.println("Component #" + cnt);
				ArrayList<Vertex> component = new ArrayList<Vertex>();
				getComponent(start, component);
				System.err.println("size = " + component.size());
				for (Vertex v : component) {
					v.d = Integer.MAX_VALUE / 2;
				}
				start.d = 0;
				Queue<Vertex> q = new ArrayDeque<Vertex>();
				q.add(start);
				bfs(q);
				Vertex best = start;
				for (Vertex v : component) {
					if (v.d > best.d) {
						best = v;
					}
				}

				for (Vertex v : component) {
					v.d = Integer.MAX_VALUE / 2;
					v.info.placed = false;
				}
				best.d = 0;
				q = new ArrayDeque<Vertex>();
				q.add(best);
				bfs(q);
				best = start;
				for (Vertex v : component) {
					if (v.d > best.d) {
						best = v;
					}
				}

				System.err.println("diam = " + best.d);

				best.info.pos = 0;
				best.info.placed = true;
				if (best.prev != null) {
					if (best.prev.endSecond < 0) {
						best.info.reversed = true;
					}
				}
				while (best.d > 0) {
					best.prev.v1.info.pos = best.info.pos + best.prev.len
							+ (best.info.len + best.prev.v1.info.len) / 2;
					if (best.prev.endFirst > 0) {
						best.prev.v1.info.reversed = true;
					}
					best = best.prev.v1;
					best.info.placed = true;
				}

				for (Vertex v : component) {
					v.u = false;
					if (v.info.placed) {
						v.d = 0;
						q.add(v);
					} else {
						v.d = Integer.MAX_VALUE / 2;
					}
				}

				bfs(q);

				for (Vertex v : component) {
					if (v.d == 2) {
						remove(v);
					}
				}

				setLastPostitions(best, component);

				for (Vertex v : component) {
					v.u = true;
				}

				Collections.sort(component, new Comparator<Vertex>() {

					@Override
					public int compare(Vertex o1, Vertex o2) {
						return o1.info.pos - o2.info.pos;
					}
				});

				if (component.size() > 1) {
					int len = 0;
					scaf.println("scaffold #" + cnt + " (" + component.size()
							+ ")");
					Vertex p = null;
					int wr = 0;
					for (Vertex v : component) {
						if (p != null) {
							scaf.println(Math.abs(v.info.pos - p.info.pos)
									- (v.info.len + p.info.len)
									/ 2
									+ "\t"
									+ (Math.abs(v.info.realPos - p.info.realPos) - (v.info.len + p.info.len) / 2));
							len += Math.abs(v.info.pos - p.info.pos)
									- (v.info.len + p.info.len) / 2;
							System.out.println("len = " + len);
							if (Math.abs(v.info.id - p.info.id) > 10) {
								wr++;
							}
						}
						len += v.info.len;
						scaf.println(v.info.name + "\t(" + v.info.id + ", "
								+ v.info.pos + ", " + v.info.len + ", "
								+ v.info.realPos + ", " + v.info.reversed
								+ ", " + v.info.realRev + ")");
						p = v;
					}
					scaf.println("length = " + len);
					scaf.println(wr + "/" + (component.size() - 1));
					wrong += wr;
					all += component.size() - 1;
					maxlen = Math.max(maxlen, len);
					if (len > 80000) {
						sumlen += len;
						sca++;
					}
				} else {
					skipped++;
				}

			}
			if (!change) {
				break;
			}
		}

		System.out.println(skipped + "/" + g.length + " contigs skipped");
		System.out.println("max length = " + maxlen);
		System.out.println("avg length = " + (1.0 * sumlen / sca));
		System.out.println(wrong + "/" + all + " connections wrong");

		scaf.close();

	}

	private static void bfs(Queue<Vertex> q) {
		while (!q.isEmpty()) {
			Vertex v = q.poll();
			for (Edge e : v.edges) {
				if (e.v2.d > e.v1.d + 1) {
					e.v2.d = e.v1.d + 1;
					e.v2.prev = e;
					q.add(e.v2);
				}
			}
		}
	}

	private static void remove(Vertex v) {
		while (!v.edges.isEmpty()) {
			Edge e = v.edges.remove(v.edges.size() - 1);
			e.v2.edges.remove(e.rev());
		}
	}

	private static void setLastPostitions(Vertex v, ArrayList<Vertex> component)
			throws MathException {
		v.u = true;
		if (!v.info.placed) {
			int rev = 0;
			int str = 0;
			for (Edge e : v.edges) {
				if (!e.v2.info.reversed) {
					if (e.endFirst < 0) {
						if (e.endSecond < 0) {
							rev++;
						} else {
							str++;
						}
					} else {
						if (e.endSecond < 0) {
							str++;
						} else {
							rev++;
						}
					}
				} else {
					if (e.endFirst < 0) {
						if (e.endSecond < 0) {
							str++;
						} else {
							rev++;
						}
					} else {
						if (e.endSecond < 0) {
							rev++;
						} else {
							str++;
						}
					}
				}
			}
			if (rev > str) {
				v.info.reversed = true;
			}
			int l = -Data.dnaLength;
			int r = Data.dnaLength;
			while (r - l > 2) {
				int ml = (2 * l + r) / 3;
				int mr = (l + 2 * r) / 3;
				double vl = countValue(v, ml, component);
				double vr = countValue(v, mr, component);
				if (vl < vr) {
					l = ml;
				} else {
					r = mr;
				}
			}
			v.info.pos = (l + r) / 2;

			boolean bad = false;
			for (Edge e : v.edges) {
				if (e.v2.info.placed
						&& Math.abs(e.v1.info.pos - e.v2.info.pos) > Data.NORMAL_DISTRIBUTION_CENTER
								+ 5 * Data.NORMAL_DISTRIBUTION_DEVIATION) {
					bad = true;
					break;
				}
			}

			if (bad) {
				int sum = 0;
				int cnt = 0;
				for (Edge e : v.edges) {
					if (e.v2.info.placed) {
						sum += e.v2.info.pos;
						cnt++;
					}
				}
				v.info.pos = sum / cnt;
			}

			System.out
					.println("last pos " + v.info.pos + "\t" + v.info.realPos);
			for (Edge e : v.edges) {
				if (e.v2.info.placed) {
					System.out
							.println("\t"
									+ e.v2.info.pos
									+ "\t"
									+ e.v2.info.realPos
									+ "\t"
									+ e.len
									+ "\t"
									+ (Math.abs(v.info.pos - e.v2.info.pos) - (v.info.len + e.v2.info.len) / 2)
									+ "\t"
									+ (Math.abs(v.info.realPos
											- e.v2.info.realPos) - (v.info.len + e.v2.info.len) / 2));
				}
			}
			System.out.println();
		}
		v.info.placed = true;
		for (Edge e : v.edges) {
			if (!e.v2.u) {
				setLastPostitions(e.v2, component);
			}
		}
	}

	private static double countValue(Vertex x, int position,
			ArrayList<Vertex> component) throws MathException {
		for (Vertex v : component) {
			v.check = false;
		}
		double ans = 0;
		for (Edge e : x.edges) {
			if (!e.v2.info.placed) {
				continue;
			}
			e.v2.check = true;
			int d = Math.abs(position - e.v2.info.pos)
					- (x.info.len + e.v2.info.len) / 2;
			ans += DistanceFinder.getProbabilityThatAllMatepairsMatch(d,
					e.v2.info.len, x.info.len, e.pairs.length, e.dSq, e.dLin,
					e.dCon);
		}
		for (Vertex v : component) {
			if (v.check || !v.info.placed) {
				continue;
			}
			int d = Math.abs(position - v.info.pos) - (x.info.len + v.info.len)
					/ 2;
			ans += DistanceFinder.getProbabilityThatAllMatepairsMatch(d,
					v.info.len, x.info.len, 0, 0, 0, 0);
		}
		if (Double.isNaN(ans)) {
			ans = Double.NEGATIVE_INFINITY;
		}
		return ans;
	}

	private static void flipContigs(Vertex[] g) {
		for (Vertex v : g) {
			v.u = false;
		}
		for (Vertex v : g) {
			if (!v.u) {
				ft = 0;
				tf = 0;
				tt = 0;
				ff = 0;
				dfsFlip(v, false);
				System.out.println(ff + " " + tt + " " + ft + " " + tf);
			}
		}
	}

	static int ft, tf, tt, ff;

	private static void dfsFlip(Vertex v, boolean flip) {
		v.u = true;
		v.info.reversed = flip;
		if (v.info.reversed) {
			if (v.info.realRev) {
				tt++;
			} else {
				tf++;
			}
		} else {
			if (v.info.realRev) {
				ft++;
			} else {
				ff++;
			}
		}
		for (Edge e : v.edges) {
			if (!e.v2.u) {
				dfsFlip(e.v2, flip ^ (e.reverse < 0));
			}
		}
	}

	private static void printGraph(Vertex[] g) throws FileNotFoundException {
		PrintWriter graph = new PrintWriter("graph");

		graph.println("graph G {");
		graph.println("\tnode [shape=circle];");
		for (ContigInfo ci : contigInfo.values()) {
			if (ci.v.edges.size() == 0) {
				continue;
			}
			graph.println("\t" + ci.name + " [label = <" + ci.id + ", "
					+ ci.len + ">];");
		}
		HashMap<Integer, HashSet<Integer>> hs = new HashMap<Integer, HashSet<Integer>>();
		for (Vertex v : g) {
			for (Edge e : v.edges) {
				if (e.v1.info.id < e.v2.info.id
						&& (!hs.containsKey(e.v1.info.id) || !hs.get(
								e.v1.info.id).contains(e.v2.info.id))) {
					String name1 = e.v1.info.name;
					String name2 = e.v2.info.name;
					graph.println("\t"
							+ name1
							+ " -- "
							+ name2
							+ " [label = <"
							// + e.len
							// + ", "
							+ (Math.abs(e.v1.info.realPos - e.v2.info.realPos) - (e.v1.info.len + e.v2.info.len) / 2)
							+ ">"
							+ (Math.abs(e.v1.info.id - e.v2.info.id) == 1 ? ", style = bold, color = red"
									: "") + "];");
					if (!hs.containsKey(e.v1.info.id)) {
						hs.put(e.v1.info.id, new HashSet<Integer>());
					}
					hs.get(e.v1.info.id).add(e.v2.info.id);
				}
			}
		}
		graph.println("}");
		graph.close();

		System.err.println("Graph printed");
	}

	private static void optimize(Vertex[] scaffold) throws MathException {
		setLentgth(scaffold);

		double current = getProbability(scaffold);

		Vertex[] s = scaffold.clone();

		boolean change = true;

		int cnt = 50;

		while (cnt-- > 0 && change) {
			change = false;

			for (int i = 0; i < s.length; i++) {
				for (int j = -2; j <= 2; j++) {
					if (i + j < 0 || i + j >= s.length || j == 0) {
						continue;
					}
					Vertex v = s[i];
					if (j < 0) {
						for (int k = 0; k > j; k--) {
							s[i + k] = s[i + k - 1];
						}
					} else {
						for (int k = 0; k < j; k++) {
							s[i + k] = s[i + k + 1];
						}
					}
					s[i + j] = v;
					setLentgth(s);
					double p = getProbability(s);
					if (p < current) {
						for (int k = Math.max(0, i - 2); k < Math.min(s.length,
								i + 3); k++) {
							s[k] = scaffold[k];
						}
					} else {
						change = true;
						System.arraycopy(s, 0, scaffold, 0, s.length);
					}
				}
			}
		}

		setLentgth(scaffold);

	}

	private static void setLentgth(Vertex[] scaffold) throws MathException {
		scaffold[0].info.pos = 0;
		for (int i = 0; i + 1 < scaffold.length; i++) {
			int[] d1 = new int[0];
			int[] d2 = new int[0];
			for (Edge e : scaffold[i].edges) {
				if (e.v2 != scaffold[i + 1]) {
					continue;
				}
				d1 = e.getD1();
				d2 = e.getD2();
				break;
			}
			scaffold[i + 1].info.pos = scaffold[i].info.pos
					+ DistanceFinder.getMostProbableDistance(
							scaffold[i].info.len, scaffold[i + 1].info.len, d1,
							d2)
					+ (scaffold[i].info.len + scaffold[i + 1].info.len) / 2;
		}

	}

	private static double getProbability(Vertex[] scaffold)
			throws MathException {

		double p_sum = 0;

		for (Vertex v : scaffold) {
			for (Edge e : v.edges) {
				p_sum += DistanceFinder.getProbabilityThatAllMatepairsMatch(
						len(e.v1.info, e.v2.info), e.v1.info.len,
						e.v2.info.len, e.pairs.length, e.dSq, e.dLin, e.dCon);
			}
		}

		return p_sum / 2;
	}

	private static int len(ContigInfo ci1, ContigInfo ci2) {
		return Math.abs(ci1.pos - ci2.pos) - (ci1.len + ci2.len) / 2;
	}

	private static void getComponent(Vertex v, ArrayList<Vertex> al) {
		v.u = true;
		al.add(v);
		for (Edge e : v.edges) {
			if (!e.v2.u) {
				getComponent(e.v2, al);
			}
		}
	}

	private static HashMap<String, ArrayList<BlastAlignment>> toMap(
			BlastAlignment[] a) {
		HashMap<String, ArrayList<BlastAlignment>> map = new HashMap<String, ArrayList<BlastAlignment>>();
		for (BlastAlignment ba : a) {
			if (!map.containsKey(ba.qseqid)) {
				map.put(ba.qseqid, new ArrayList<BlastAlignment>());
			}
			map.get(ba.qseqid).add(ba);
		}
		return map;
	}

	private static void filterLen(Vertex[] g, int size, boolean strict) {
		int[] best = new int[size];
		for (Vertex v : g) {
			Arrays.fill(best, Integer.MAX_VALUE / 2);
			for (Edge e : v.edges) {
				for (int i = 0; i < best.length; i++) {
					if (e.len < best[i]) {
						for (int j = best.length - 1; j > i; j--) {
							best[j] = best[j - 1];
						}
						best[i] = e.len;
						break;
					}
				}
			}
			for (ListIterator<Edge> li = v.edges.listIterator(); li.hasNext();) {
				Edge e = li.next();
				e.good = e.len <= best[best.length - 1];
			}
		}

		filter(g, strict);
	}

	private static void filterError(Vertex[] g, int size, boolean strict) {
		int[] best = new int[size];
		for (Vertex v : g) {
			Arrays.fill(best, Integer.MAX_VALUE / 2);
			for (Edge e : v.edges) {
				for (int i = 0; i < best.length; i++) {
					if (e.err < best[i]) {
						for (int j = best.length - 1; j > i; j--) {
							best[j] = best[j - 1];
						}
						best[i] = e.err;
						break;
					}
				}
			}
			for (ListIterator<Edge> li = v.edges.listIterator(); li.hasNext();) {
				Edge e = li.next();
				e.good = e.err <= best[best.length - 1];
			}
		}

		filter(g, strict);
	}

	private static void filter(Vertex[] g, boolean strict) {
		for (Vertex v : g) {
			for (ListIterator<Edge> li = v.edges.listIterator(); li.hasNext();) {
				Edge e = li.next();
				if (!strict) {
					if (!e.good && !e.r.good) {
						li.remove();
					}
				} else {
					if (!e.good || !e.r.good) {
						li.remove();
					}
				}
			}
		}
	}

	private static void setPositions(Vertex v) {
		v.u = true;
		int l1 = Integer.MIN_VALUE / 2;
		int l2 = Integer.MIN_VALUE / 2;
		int r1 = Integer.MAX_VALUE / 2;
		int r2 = Integer.MAX_VALUE / 2;
		for (Edge e : v.edges) {
			if (e.v2.u) {
				l1 = Math.max(l1, e.v2.info.pos - e.len - e.err);
				l2 = Math.max(l2, e.v2.info.pos + e.len - e.err);
				r1 = Math.min(r1, e.v2.info.pos - e.len + e.err);
				r2 = Math.min(r2, e.v2.info.pos + e.len + e.err);
			}
		}
		if (l1 <= r1) {
			v.info.pos = (l1 + r1) / 2;
		} else {
			v.info.pos = (l2 + r2) / 2;
		}
		for (Edge e : v.edges) {
			if (!e.v2.u) {
				e.v2.info.pos = v.info.pos + e.len;
				setPositions(e.v2);
			}
		}
	}

	private static void loadContigs(String string) throws FileNotFoundException {
		File f = new File(string);
		Scanner in = new Scanner(f);
		String next = in.nextLine();
		while (next != null) {
			String name = next.substring(1);
			String contig = "";
			next = in.nextLine();
			while (next != null && !next.startsWith(">")) {
				contig += next;
				if (in.hasNext()) {
					next = in.nextLine();
				} else {
					next = null;
				}
			}
			ContigInfo ci = getInfo(name);
			ci.len = contig.length();
			ci.seq = contig;
		}
		in.close();
	}

	public static class Pair {
		public Pair(SAMAlignment a, SAMAlignment b) {
			d1 = a;
			d2 = b;
		}

		public boolean isReverse() {
			return d1.isReverseComplimented() == d2.isReverseComplimented();
		}

		public int getD1() {
			return d1.isReverseComplimented() ? d1.pos : getInfo(d1.rname).len
					- d1.pos - Data.READ_LENGTH;
		}

		public int getD2() {
			return d2.isReverseComplimented() ? d2.pos : getInfo(d2.rname).len
					- d2.pos - Data.READ_LENGTH;
		}

		SAMAlignment d1, d2;
	}

	public static class InfoPair {
		ContigInfo s1, s2;

		public InfoPair(String s1, String s2) {
			this.s1 = getInfo(s1);
			this.s2 = getInfo(s2);
		}

		public InfoPair(ContigInfo s1, ContigInfo s2) {
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
	}

	private static HashMap<String, ArrayList<SAMAlignment>> toMap(
			SAMAlignment[] a) {
		HashMap<String, ArrayList<SAMAlignment>> map = new HashMap<String, ArrayList<SAMAlignment>>();
		for (SAMAlignment sa : a) {
			if (!map.containsKey(sa.qname)) {
				map.put(sa.qname, new ArrayList<SAMAlignment>());
			}
			map.get(sa.qname).add(sa);
		}
		return map;
	}

	public static class Edge {

		public Edge(Vertex v1, Vertex v2, int len, int err) {
			this.v1 = v1;
			this.v2 = v2;
			this.len = len;
			this.err = err;
			av_len = Integer.MAX_VALUE;
		}

		public int[] getD1() {
			int[] d1 = new int[pairs.length];
			for (int i = 0; i < d1.length; i++) {
				d1[i] = pairs[i].getD1();
			}
			return d1;
		}

		public int[] getD2() {
			int[] d2 = new int[pairs.length];
			for (int i = 0; i < d2.length; i++) {
				d2[i] = pairs[i].getD2();
			}
			return d2;
		}

		public void setReverse(int rev) {
			reverse = rev;
			rev().reverse = rev;
		}

		public void setReads(Pair[] pairs) {
			this.pairs = pairs.clone();
			rev();
			int[] d1 = getD1();
			int[] d2 = getD2();
			dSq = DistanceFinder.dSq(d1, d2);
			dLin = DistanceFinder.dLin(d1, d2);
			dCon = DistanceFinder.dCon(d1, d2);
			r.dSq = dSq;
			r.dLin = dLin;
			r.dCon = dCon;

			int end1 = 0;
			int end2 = 0;
			for (Pair p : pairs) {
				if (p.d1.pos > v1.info.len / 2) {
					end1++;
				}
				if (p.d2.pos > v2.info.len / 2) {
					end2++;
				}
			}
			endFirst = end1 > pairs.length / 2 ? 1 : -1;
			endSecond = end2 > pairs.length / 2 ? 1 : -1;
		}

		public void setAvLen(long l) {
			av_len = (int) l;
			rev().av_len = (int) l;
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
			r.pairs = new Pair[pairs.length];
			for (int i = 0; i < r.pairs.length; i++) {
				r.pairs[i] = new Pair(pairs[i].d2, pairs[i].d1);
			}
			return e;
		}

		Vertex v1, v2;
		int len;
		int av_len;
		int err;
		Edge r;
		int reverse;
		boolean good;
		Pair[] pairs;
		double dSq, dLin, dCon;
		int endFirst, endSecond;
	}

	static class Vertex {
		public Vertex(ContigInfo contigInfo) {
			info = contigInfo;
			edges = new ArrayList<Edge>();
		}

		ArrayList<Edge> edges;
		boolean u;
		int d;
		ContigInfo info;
		Edge prev;
		boolean check;
	}

	static private String transform(String name) {
		return name.replace(' ', '_');
	}

	static class ContigInfo {

		public ContigInfo(int id, String name) {
			this.id = id;
			this.name = transform(name);
			v = new Vertex(this);
			realPos = -1;
		}

		public boolean isReversed() {
			return reversed;
		}

		int len;
		int id;
		int pos;
		boolean placed;
		int realPos;
		boolean realRev;
		String name;
		String seq;
		Vertex v;
		boolean reversed;
	}

	static private HashMap<String, ContigInfo> contigInfo = new HashMap<String, ContigInfo>();

	static public ContigInfo getInfo(String s) {
		if (!contigInfo.containsKey(s)) {
			contigInfo.put(s, new ContigInfo(contigInfo.size(), s));
		}
		return contigInfo.get(s);
	}

}
*/