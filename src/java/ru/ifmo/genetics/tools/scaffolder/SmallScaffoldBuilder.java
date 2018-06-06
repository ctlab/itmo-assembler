package ru.ifmo.genetics.tools.scaffolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.MathException;

public class SmallScaffoldBuilder {

	static final int COLONY_SIZE = 20;
	static final double rho = 0.3;
	static final double tauMin = 0.05;
	static final Random rand = new Random(255);
	static final int maxSteps = 10000;

	public static List<Scaffold> buildSmallScaffolds(Vertex[] g)
			throws MathException {
		for (Vertex v : g) {
			v.color = -1;
			v.u = false;
		}
		int color = 0;
		for (Vertex v : g) {
			if (v.color < 0) {
				color(v, color++);
			}
		}
		int contLen = 0;
		@SuppressWarnings("unchecked")
		ArrayList<Vertex>[] comps = new ArrayList[color];
		for (Vertex v : g) {
			if (comps[v.color] == null) {
				ArrayList<Vertex> al = new ArrayList<Vertex>();
				fillComponent(v, al);
				comps[v.color] = al;
				if (al.size() > 1) {
					for (Vertex v2 : al) {
						contLen += v2.info.len;
					}
				}
			}
		}
		System.err.println("contigs length sum: " + contLen);
		System.err.println("components found: " + color);
		ArrayList<Scaffold> ans = new ArrayList<Scaffold>();
		for (ArrayList<Vertex> c : comps) {
			// System.err.println("processing component#" + c.get(0).color);
			Scaffold[] scaf = getScaffolds(c);
			for (Scaffold s : scaf) {
				ans.add(s);
			}
		}
		return ans;
	}

	private static void fillComponent(Vertex v, ArrayList<Vertex> al) {
		v.u = true;
		al.add(v);
		for (Edge e : v.edges) {
			if (!e.v2.u) {
				fillComponent(e.v2, al);
			}
		}
	}

	private static void color(Vertex v, int c) {
		v.color = c;
		for (Edge e : v.edges) {
			if (e.v2.color < 0) {
				color(e.v2, c);
			}
		}
	}

	private static Scaffold[] getScaffolds(ArrayList<Vertex> ver)
			throws MathException {
		boolean big = ver.size() > 1;
		ArrayList<Scaffold> ans = new ArrayList<Scaffold>();
		int newSing = 0;
		while (ver.size() > 0) {
			for (Vertex v : ver) {
				int deg = 0;
				int ghost = 0;
				for (Edge e : v.edges) {
					if (!ver.contains(e.v2)) {
						e.tau = 0;
						continue;
					}
					if (e.ghost) {
						ghost++;
					} else {
						deg++;
					}
					e.tau = tauMin;
				}
				v.tau = 100 - deg
						+ (deg == 0 ? -100 + tauMin : -1.0 * ghost / deg);
			}
			// System.err.println("Component: ");
			// for (Vertex v : ver) {
			// for (Edge e : v.edges) {
			// if (!ver.contains(e.v2)) {
			// continue;
			// }
			// System.err.println("\t" + e.v1.info.id + "\t"
			// + e.v2.info.id + "\t" + e.tau);
			// }
			// } // System.err.println("start: " + start.info.id);
			@SuppressWarnings("unchecked")
			ArrayList<Edge>[] path = new ArrayList[COLONY_SIZE];
			Vertex[] start = new Vertex[COLONY_SIZE];
			ArrayList<Edge> bestPath = null;
			Vertex bestStart = null;
			double bestFitness = Double.NEGATIVE_INFINITY;
			for (Vertex v : ver) {
				v.u = false;
			}
			for (int steps = 0; steps < maxSteps; steps++) {
				double tSum = 0;
				for (Vertex v : ver) {
					if (v.tau < tauMin) {
						v.tau = tauMin;
					}
					tSum += v.tau;
				}
				for (int i = 0; i < COLONY_SIZE; i++) {
					start[i] = null;
					double p = rand.nextDouble();
					for (Vertex v : ver) {
						if (p < v.tau / tSum) {
							start[i] = v;
							break;
						}
						p -= v.tau / tSum;
					}
					path[i] = getPath(start[i]);
				}
				for (int i = 0; i < path.length; i++) {
					double fitness = getFitness(path[i]);
					if (fitness < 0 && !Double.isInfinite(fitness)) {
						System.err.println("error: " + fitness);
					}
					if (bestFitness < fitness || bestStart == null) {
						bestFitness = fitness;
						bestPath = path[i];
						bestStart = start[i];
					}
					for (Edge e : path[i]) {
						e.tau = Math.max(tauMin, e.tau * rho + fitness);
						e.rev().tau = e.tau;
					}
					// start[i].tau = Math.max(tauMin, start[i].tau * rho
					// + fitness);
					if (path[i].size() > 0) {
						path[i].get(path[i].size() - 1).v2.tau = Math.max(
								tauMin, path[i].get(path[i].size() - 1).v2.tau
										* rho + fitness);
					}
					// if (p.size() > 0) {
					// p.get(p.size() - 1).v2.tau = p.get(p.size() - 1).v2.tau
					// * rho + fitness;
					// }
				}
				if (allSame(path)) {
					break;
				}
			}

			if (bestStart == null) {
				System.err.println(Arrays.toString(path));
				System.err.println(Arrays.toString(start));
				System.err.println(bestFitness);
				System.err.println(getFitness(path[0]));
			}

			// System.err.println("scaffold found: " + (path[0].size() + 1));
			Scaffold s = new Scaffold();
			bestStart.info.pos = 0;
			if (bestPath.size() == 0) {
				s.vertecies.add(bestStart);
				if (big) {
					newSing++;
				}
			} else {
				s.vertecies.add(bestPath.get(0).v1);
				for (Edge e : bestPath) {
					e.v2.info.pos = e.v1.info.pos + e.len
							+ (e.v1.info.len + e.v2.info.len) / 2;
					s.vertecies.add(e.v2);
				}
			}
			// System.err.println(s);
			ans.add(s);
			for (Vertex v : s.vertecies) {
				ver.remove(v);
			}
		}
		Scaffold[] ss = new Scaffold[ans.size()];
		for (int i = 0; i < ss.length; i++) {
			ss[i] = ans.get(i);
		}
		return ss;
	}

	private static double getFitness(ArrayList<Edge> p) throws MathException {
		if (p.isEmpty()) {
			return Double.NEGATIVE_INFINITY;
		}
		Scaffold s = new Scaffold();
		s.vertecies.add(p.get(0).v1);
		p.get(0).v1.info.pos = 0;
		int ghost = 0;
		int sum = 0;
		for (Edge e : p) {
			if (e.ghost) {
				ghost++;
			}
			sum += e.len;
			s.vertecies.add(e.v2);
			e.v2.info.pos = e.v1.info.pos + (e.v1.info.len + e.v2.info.len) / 2;
		}
		// double fine = 0;
		// for (Vertex v : s.vertecies) {
		// fine += v.edges.size();
		// }
		int d = s.vertecies.size();
		// System.err
		// .println(d
		// + "\t"
		// + ghost
		// + "\t"
		// + sum
		// + "\t"
		// + (d - (1.0 * ghost / d) - (1.0 * sum / d / d /
		// Data.NORMAL_DISTRIBUTION_CENTER)));
		return Math.max(
				0,
				d - (0.5 * ghost / d)
						- (0.5 * sum / d / Data.getMaxInsertSize())/*
																	 * - 0.1
																	 * fine / d
																	 */);
	}

	private static boolean allSame(ArrayList<Edge>[] path) {
		boolean same = true;
		for (int i = 1; same && i < path.length; i++) {
			same &= path[0].size() == path[i].size();
			for (int j = 0; same && j < path[i].size(); j++) {
				same &= path[0].get(j) == path[i].get(j);
			}
		}
		return same;
	}

	private static ArrayList<Edge> getPath(Vertex v) {
		ArrayList<Edge> al = new ArrayList<Edge>();
		Vertex start = v;
		// System.err.println("start: " + start.info.id);
		loop: while (true) {
			v.u = true;
			double tSum = 0;
			for (Edge e : v.edges) {
				if (!e.v2.u) {
					tSum += e.tau;
				}
			}
			double p = rand.nextDouble();
			// if (p < v.tau / tSum) {
			// break;
			// }
			// p -= v.tau / tSum;
			// System.err.println("p: " + p + "\ttSum: " + tSum);
			for (Edge e : v.edges) {
				if (e.v2.u) {
					continue;
				}
				// System.err.println("edge: " + e.tau + "\tto: " +
				// e.v2.info.id);
				if (p < e.tau / tSum) {
					v = e.v2;
					// System.err.println("next: " + v.info.id);
					al.add(e);
					continue loop;
				}
				p -= e.tau / tSum;
			}
			break;
		}
		start.u = false;
		for (Edge e : al) {
			e.v1.u = false;
			e.v2.u = false;
		}
		return al;
	}

}
