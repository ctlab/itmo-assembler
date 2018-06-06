package ru.ifmo.genetics.tools.scaffolder;
import java.util.Arrays;
import java.util.ListIterator;

public class GraphFilter {

	public static void filter(Vertex[] g) {
		// int[] deg = new int[1000];
		// for (Vertex v : g) {
		// if (v.edges.size() < deg.length) {
		// deg[v.edges.size()]++;
		// }
		// }
		// for (int i = 0; i < deg.length; i++) {
		// if (deg[i] > 0) {
		// System.out.println(i + "\t" + deg[i]);
		// }
		// }
		// removeShort(g);
		// removePopular(g, 4);
		//
		// for (Vertex v : g) {
		// for (ListIterator<Edge> li = v.edges.listIterator(); li.hasNext();) {
		// Edge e = li.next();
		// if (e.v1.info.len + e.v2.info.len < 5000) {
		// li.remove();
		// e.v2.edges.remove(e.rev());
		// }
		// }
		// }
		//
		// deg = new int[1000];
		// for (Vertex v : g) {
		// if (v.edges.size() < deg.length) {
		// deg[v.edges.size()]++;
		// }
		// }
		// for (int i = 0; i < deg.length; i++) {
		// if (deg[i] > 0) {
		// System.out.println(i + "\t" + deg[i]);
		// }
		// }

		// filterLen(g, 5, true);
		// filterError(g, 4, false);

		// for (Vertex v : g) {
		// if (v.info.id < 230 || v.info.id > 250) {
		// cut(v);
		// }
		// }

	}

	public static void removeShortContigs(Vertex[] g, double d) {
		for (Vertex v : g) {
			if (v.info.len < d) {
				cut(v);
			}
		}
	}

	public static void filter(Vertex[] g, boolean strict) {
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

	public static void filterLen(Vertex[] g, int size, boolean strict) {
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

	public static void filterError(Vertex[] g, int size, boolean strict) {
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

	public static void cut(Vertex v) {
		while (!v.edges.isEmpty()) {
			Edge e = v.edges.remove(v.edges.size() - 1);
			e.v2.edges.remove(e.rev());
		}
	}

	public static void removePopular(Vertex[] g, int x) {
		int removed = 0;
		boolean[] bad = new boolean[g.length];
		for (int i = 0; i < bad.length; i++) {
			if (g[i].edges.size() > x) {
				bad[i] = true;
			}
		}
		for (int i = 0; i < bad.length; i++) {
			if (bad[i]) {
				cut(g[i]);
				removed++;
			}
		}
		System.err.println("Removed popular: " + removed);
	}

	public static void removeOvercovered(Vertex[] g, double v) {
		int removed = 0;
		boolean[] bad = new boolean[g.length];
		for (int i = 0; i < bad.length; i++) {
			if (g[i].getCover() > v) {
				bad[i] = true;
			}
		}
		for (int i = 0; i < bad.length; i++) {
			if (bad[i]) {
				cut(g[i]);
				removed++;
			}
		}
		System.err.println("Removed overcovered: " + removed);

	}

	public static void removeLongEdges(Vertex[] g, double d) {
		for (Vertex v : g) {
			for (ListIterator<Edge> it = v.edges.listIterator(); it.hasNext();) {
				Edge e = it.next();
				if (e.len > d) {
					it.remove();
				}
			}
		}
	}

	public static void removeShortEdges(Vertex[] g, double d) {
		for (Vertex v : g) {
			for (ListIterator<Edge> it = v.edges.listIterator(); it.hasNext();) {
				Edge e = it.next();
				if (e.len < d) {
					it.remove();
				}
			}
		}
	}

}
