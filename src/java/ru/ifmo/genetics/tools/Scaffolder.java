package ru.ifmo.genetics.tools;

import java.io.*;
import java.util.*;

import ru.ifmo.genetics.tools.scaffolder.*;
import ru.ifmo.genetics.utils.tool.*;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.*;

public class Scaffolder extends Tool {

	public Scaffolder() {
		super(NAME, DESCRIPTION);
	}

	public static final String NAME = "scaffolder";
	public static final String DESCRIPTION = "make scaffolds from contigs using magic";

	public final Parameter<Integer> dnaLength = addParameter(new IntParameterBuilder(
			"genome-length").mandatory().withShortOpt("g")
			.withDescription("the estimated length of the genome sequence")
			.create());

	public final Parameter<File> contigFile = addParameter(new FileParameterBuilder(
			"contigs").mandatory().withShortOpt("i")
			.withDescription("contigs in fasta format").create());

	public final Parameter<File[]> mp1File = addParameter(new FileMVParameterBuilder(
			"mate-pair1").mandatory().withShortOpt("1")
			.withDescription("mate paired reads 1 bowtie alignment").create());

	public final Parameter<File[]> mp2File = addParameter(new FileMVParameterBuilder(
			"mate-pair2").mandatory().withShortOpt("2")
			.withDescription("mate paired reads 2 bowtie alignment").create());

	public final Parameter<File> refMap = addParameter(new FileParameterBuilder(
			"ref-map").optional().withShortOpt("rm")

	.withDescription("BLAST results of contig alignment to reference genome")
			.create());

	@Override
	protected void runImpl() throws ExecutionFailedException {
		Locale.setDefault(Locale.US);
		if (mp1File.get() == null || mp2File.get() == null
				|| mp1File.get().length == 0
				|| mp1File.get().length != mp2File.get().length) {
			error("No mate paired files are given! (or number mismatch)", new IllegalArgumentException());
			return;
		}
		Data.libraries = new Library[mp1File.get().length];
		for (int i = 0; i < mp1File.get().length; i++) {
			try {
				Data.libraries[i] = new Library(mp1File.get()[i],
						mp2File.get()[i], i);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		try {
			loadContigs(contigFile.get());
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
			return;
		}
		Data.contigs = Contig.getInfo().size();
		Data.contigSum = 0;
		for (Contig c : Contig.getInfo().values()) {
			Data.contigSum += c.len;
		}
		Data.dnaLength = dnaLength.get();
		if (refMap.get() != null) {
			System.out.println("loading positions:");
			Map<String, ArrayList<BlastAlignment>> contigMap;
			try {
				contigMap = toMap(BlastParser.parse(refMap.get()));
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			for (String s : contigMap.keySet()) {
				String name = Contig.transform(s);
				Contig ci = Contig.getInfo(name);
				for (BlastAlignment al : contigMap.get(s)) {
					if ((al.qend - al.qstart + 0.0) / ci.len > Data.COVER_DEMAND) {
						ci.realPos.add((al.sstart + al.send) / 2);
						ci.realRev.add(al.sstart > al.send);
					}
				}
				if (ci.realPos.isEmpty()) {
					BlastAlignment al = contigMap.get(s).get(0);
					ci.realPos.add((al.sstart + al.send) / 2);
					ci.realRev.add(al.sstart > al.send);
				}
			}
			ArrayList<Contig> list = new ArrayList<Contig>();
			list.addAll(Contig.getInfo().values());
			for (Contig c : list) {
				Collections.sort(c.realPos);
			}
			Collections.sort(list, new Comparator<Contig>() {

				@Override
				public int compare(Contig o1, Contig o2) {
					return (o1.realPos.isEmpty() ? -2 : o1.realPos.get(0))
							- (o2.realPos.isEmpty() ? -1 : o2.realPos.get(0));
				}
			});
			for (int i = 0; i < list.size(); i++) {
				list.get(i).id = i;
			}
		}
		{
			int all = 0;
			int ok = 0;
			for (Contig ci : Contig.getInfo().values()) {
				if (!ci.realPos.isEmpty()) {
					ok++;
				} else {
					// System.out.println(ci.name);
				}
				all++;
			}
			System.out.println(ok + " out of " + all
					+ " contigs matched reference");
		}

		// allReads = Math.min(mapa.size(), mapb.size());

		Vertex[] g;
		try {
			g = GraphBuilder.buildGraph(Data.libraries);
			System.err.println("contig graph built");
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}

		for (Vertex v : g) {
			for (Edge e : v.edges) {
				e.dCon = DistanceFinder.dCon(e.pairs);
				e.dSq = DistanceFinder.dSq(e.pairs);
				e.dLin = DistanceFinder.dLin(e.pairs);
			}
		}

		// double qSum = 0;
		// int reads = 0;
		// int size = 50;
		//
		// for (int i = 0; i < size; i++) {
		// Vertex v = g[i];
		// for (Edge e : v.edges) {
		// if (e.v1.info.id < e.v2.info.id) {
		// continue;
		// }
		// reads += e.pairs.length;
		// }
		// }
		// for (int i = 0; i < size; i++) {
		// for (int j = 0; j < i; j++) {
		// qSum += DistanceFinder
		// .getProbabilityThatAtLeastOneMatepairMatches(
		// g[i].realDistTo(g[j]), g[i].info.len,
		// g[j].info.len);
		// }
		// }
		//
		// System.err.println("real reads: " + reads);
		// System.err.println("expected: " + qSum * Data.allReads);
		//
		// double bestGap = Double.POSITIVE_INFINITY;
		// int bp1 = 0;
		// int bp2 = 0;
		//
		// for (int p1 = 0; p1 < size; p1++) {
		// for (int p2 = 0; p2 < p1; p2++) {
		// qSum = 0;
		// for (int ii = 0; ii < size; ii++) {
		// for (int jj = 0; jj < ii; jj++) {
		// int i = ii;
		// int j = jj;
		// if (i == p1) {
		// i = p2;
		// } else if (i == p2) {
		// i = p1;
		// }
		// if (j == p1) {
		// j = p2;
		// } else if (j == p2) {
		// j = p1;
		// }
		//
		// qSum += DistanceFinder
		// .getProbabilityThatAtLeastOneMatepairMatches(
		// g[i].realDistTo(g[j]), g[ii].info.len,
		// g[jj].info.len);
		// }
		// }
		// if (Math.abs(bestGap) > Math.abs(reads - qSum * Data.allReads)) {
		// bestGap = -reads + qSum * Data.allReads;
		// bp1 = p1;
		// bp2 = p2;
		// }
		// }
		// }
		// System.err.println("not expected: " + (reads + bestGap));
		// System.err.println("best result: " + bp1 + " " + bp2);

		// boolean wrong = false;
		// for (Vertex v : g) {
		// if (v.info.reversed || v.info.realRev) {
		// // System.err.println(v.info.name + "\t" + v.info.reversed +
		// // "\t"
		// // + v.info.realRev);
		// wrong = true;
		// break;
		// }
		// }
		//
		// System.err.println("wrong: " + wrong);

		Scaffold[] scafs;
		try {
			scafs = ScaffoldBuilder.buildScaffolds(g);
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}

		try {
			PrintWriter out;
			out = new PrintWriter("scaffolds");
			for (int i = 0; i < scafs.length; i++) {
				out.println(">scaffold#" + (i + 1) + "\tsum_len:\t"
						+ scafs[i].getSum() + "\tcov:\t9.1");
				// out.println("contig name\tTBD\tlen\tTBD\tid\treference positions, some stuff and contig coverage");
				out.println(scafs[i]);
			}
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			PrintWriter graph;
			graph = new PrintWriter("scaffold_graph");
			graph.println("s1.id\ts1.end\ts2.id\ts2.end\te.len\t(real len)");
			for (Scaffold s : scafs) {
				for (int i = 0; i < s.edges.length; i++) {
					for (ScafEdge se : s.edges[i]) {
						graph.println(se.x.id + "\t" + se.ex + "\t" + se.y.id
								+ "\t" + se.ey + "\t" + se.edge);
					}
				}
			}

			graph.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			printScaffolds(scafs);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	static boolean contains(int[] mask, int value) {
		for (int i = 0; i < mask.length; i++) {
			if (mask[i] - 1 == value) {
				return true;
			}
		}
		return false;
	}

	private static void printScaffolds(Scaffold[] scafs)
			throws FileNotFoundException {
		int[] mask = new int[] { /* 178, 159, 189 */180, 116, 145, 182 };
		int[] deg = new int[scafs.length];
		PrintWriter scafGraph = new PrintWriter("scafgraph");

		for (Scaffold s : scafs) {
			for (int i = 0; i < s.edges.length; i++) {
				for (ScafEdge se : s.edges[i]) {
					Scaffold s1 = se.x;
					Scaffold s2 = se.y;
					if (s1 == null || s2 == null || s1 == s2 || s1.id > s2.id) {
						continue;
					}
					boolean good = contains(mask, s1.id)
							|| contains(mask, s2.id);
					if (good) {
						deg[s1.id]++;
						deg[s2.id]++;
					}
				}
			}
		}

		scafGraph.println("graph G {");
		scafGraph.println("\tnode [shape=circle];");
		for (Scaffold s : scafs) {
			if (deg[s.id] == 0) {
				continue;
			}
			scafGraph.println("\t" + "s" + s.id + " [label = <" + s.id + ", "
					+ s.getSum() + ", " + s.vertecies.size() + ">];");
		}
		for (Scaffold s : scafs) {
			for (int i = 0; i < s.edges.length; i++) {
				for (ScafEdge se : s.edges[i]) {
					Scaffold s1 = se.x;
					Scaffold s2 = se.y;
					if (s1 == null || s2 == null || s1.id > s2.id || s1 == s2) {
						continue;
					}
					boolean good = contains(mask, s1.id)
							|| contains(mask, s2.id);
					if (!good) {
						continue;
					}
					String name1 = "s" + s1.id;
					String name2 = "s" + s2.id;
					scafGraph
							.println("\t"
									+ name1
									+ " -- "
									+ name2
									+ " [label = <"
									+ se.edge.len
									+ ", "
									+ se.edge.realDist()
									+ ", "
									+ se.edge.v1.info.id
									+ " -- "
									+ se.edge.v2.info.id
									+ ">"
									+ (Math.abs(se.edge.v1.info.id
											- se.edge.v2.info.id) == 1 ? ", color = red"
											: "")
									+ (", style = " + se.edge.getStyle())
									+ "];");

				}
			}
		}
		scafGraph.println("}");
		scafGraph.close();

		System.err.println("Scaffold graph printed");
	}

	private static Map<String, ArrayList<BlastAlignment>> toMap(
			BlastAlignment[] a) {
		Map<String, ArrayList<BlastAlignment>> map = new TreeMap<String, ArrayList<BlastAlignment>>();
		for (BlastAlignment ba : a) {
			if (!map.containsKey(ba.qseqid)) {
				map.put(ba.qseqid, new ArrayList<BlastAlignment>());
			}
			map.get(ba.qseqid).add(ba);
		}
		return map;
	}

	private static void loadContigs(File f) throws FileNotFoundException {
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
			Contig ci = Contig.getInfo(name);
			ci.len = contig.length();
			// ci.seq = contig;
		}
		in.close();
	}

	@Override
	protected void cleanImpl() {
		// TODO Auto-generated method stub

	}

	public static void main(String[] args) {
		new Scaffolder().mainImpl(args);
	}

}
