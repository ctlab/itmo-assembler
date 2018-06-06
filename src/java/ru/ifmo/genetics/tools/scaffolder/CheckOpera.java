/*package ru.ifmo.genetics.tools.scaffolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.apache.commons.math.MathException;

public class CheckOpera {

	public static final int[] other = new int[] { 0, 2, 1, 3 };

	public static void main(String[] args) throws IOException, MathException {
		Data.NORMAL_DISTRIBUTION_CENTER = 3000;
		Data.NORMAL_DISTRIBUTION_DEVIATION = 0.1 * Data.NORMAL_DISTRIBUTION_CENTER;
		Data.NORMAL_DISTRIBUTION_DEVIATION_SQUARED = Data.NORMAL_DISTRIBUTION_DEVIATION
				* Data.NORMAL_DISTRIBUTION_DEVIATION;
		Data.READ_LENGTH = 36;
		Locale.setDefault(Locale.US);
		loadContigs(args[0]);
		int size = Contig.size();
		int full = 0;
		{
			System.out.println("loading positions:");
			HashMap<String, ArrayList<BlastAlignment>> contigMap = toMap(BlastParser
					.parse(args[1]));
			for (String s : contigMap.keySet()) {
				String name = transform(s);
				Contig ci = getInfo(name);
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
			list.addAll(Contig.values());

			for (Contig c : list) {
				Collections.sort(c.realPos);
			}

			Collections.sort(list, new Comparator<Contig>() {

				@Override
				public int compare(Contig o1, Contig o2) {
					return (o1.realPos.isEmpty() ? -1 : o1.realPos.get(0))
							- (o2.realPos.isEmpty() ? -1 : o2.realPos.get(0));
				}
			});
			for (int i = 0; i < list.size(); i++) {
				list.get(i).id = i;
				full += list.get(i).len;
			}
		}
		{
			int all = 0;
			int ok = 0;
			for (Contig ci : Contig.values()) {
				if (!ci.realPos.isEmpty()) {
					ok++;
				} else {
					System.out.println(ci.name);
				}
				all++;
			}
			System.out.println(ok + " out of " + all
					+ " contigs matched reference");
		}
		ArrayList<OperaScaffold> opera = OperaParser.parse(args[2]);
		System.err.println(opera.size() + " scaffolds read");
		PrintWriter out = new PrintWriter(args[2] + "_new");

		int used = 0;
		int maxlen = 0;
		int cnt = 0;
		int wrong = 0;
		int all = 0;
		int sum = 0;

		int[] u = new int[Contig.size()];

		PrintWriter operaDist = new PrintWriter("opera_dist");

		for (OperaScaffold s : opera) {
			for (OperaLine ol : s.contig) {
				u[getInfo(ol.name).id]++;
			}
			if (s.contig.size() < 2) {
				continue;
			}
			sum += s.getSum();
			out.println(">opera_scaffold_" + s.number + "\tlength:\t"
					+ s.length + "\tcov: 9.1");
			cnt++;
			Contig prev = null;
			Contig prev2 = null;
			for (OperaLine ol : s.contig) {
				Contig v = getInfo(ol.name);
				out.println(ol.name + "\t" + ol.or + "\t" + ol.len + "\t"
						+ ol.dist + "\t" + v.id + "\t" + v.realPos + "\t"
						+ v.realRev);
				if (prev != null) {
					if (realDist(getInfo(ol.name), prev) > Data.NORMAL_DISTRIBUTION_CENTER
							+ 3 * Data.NORMAL_DISTRIBUTION_DEVIATION) {
						System.err.println("error connection: " + prev.id + " "
								+ v.id + "\t" + realDist(prev, v));
						wrong++;
					} else if (prev2 != null
							&& Math.abs(realDist(prev, prev2) + prev.len
									+ realDist(prev, v) - realDist(prev2, v)) > Data.NORMAL_DISTRIBUTION_DEVIATION
							&& realDist(prev2, prev) < Data.NORMAL_DISTRIBUTION_CENTER
									+ 3 * Data.NORMAL_DISTRIBUTION_DEVIATION) {
						wrong++;
						System.err.println("error connection: "
								+ prev2.id
								+ " "
								+ prev.id
								+ " "
								+ v.id
								+ "\t"
								+ realDist(prev2, prev)
								+ " "
								+ prev.len
								+ " "
								+ realDist(prev, v)
								+ "\t"
								+ realDist(prev2, v)
								+ "\t"
								+ Math.abs(realDist(prev2, prev) + prev.len
										+ realDist(prev, v)
										- realDist(prev2, v)));
					}
					all++;
				}
				prev2 = prev;
				prev = getInfo(ol.name);
				used++;
			}
			maxlen = Math.max(maxlen, s.getSum());
		}

		Collections.sort(opera, new Comparator<OperaScaffold>() {

			@Override
			public int compare(OperaScaffold o1, OperaScaffold o2) {
				return o2.getSum() - o1.getSum();
			}
		});

		System.err.println("sum: " + sum);
		System.err.println("full: " + full);

		int s2 = 0;
		for (OperaScaffold s : opera) {
			s2 += s.getSum();
			if (2 * s2 >= sum) {
				System.err.println("N50: " + s.getSum());
				break;
			}
		}
		s2 = 0;
		for (OperaScaffold s : opera) {
			s2 += s.getSum();
			if (2 * s2 >= full) {
				System.err.println("N50(2): " + s.getSum());
				break;
			}
		}

		System.err.println((size - used) + "/" + size + " contigs skipped");
		System.err.println("max length = " + maxlen);
		System.err.println("avg length = " + (1.0 * sum / cnt));
		System.err.println(wrong + "/" + all + " connections wrong");

		for (int i = 0; i < u.length; i++) {
			if (u[i] > 1) {
				System.err.println("\tused too much: contig " + i + ": " + u[i]
						+ " times");
			}
		}

		out.close();

		HashMap<Integer, HashMap<Integer, Integer>> gamlet = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> avrg = new HashMap<Integer, HashMap<Integer, Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> real = new HashMap<Integer, HashMap<Integer, Integer>>();

		Scanner allEdges = new Scanner(new File("all_edges"));

		while (allEdges.hasNext()) {
			StringTokenizer st = new StringTokenizer(allEdges.nextLine());
			String name1 = st.nextToken();
			String name2 = st.nextToken();
			if (!name1.startsWith("c")) {
				break;
			}
			int id1 = Integer.parseInt(st.nextToken());
			int id2 = Integer.parseInt(st.nextToken());
			if (id2 < id1) {
				int tmp = id1;
				id1 = id2;
				id2 = tmp;
			}
			int reald = Integer.parseInt(st.nextToken());
			int gamletd = Integer.parseInt(st.nextToken());
			int avrgd = Integer.parseInt(st.nextToken());
			if (!gamlet.containsKey(id1)) {
				gamlet.put(id1, new HashMap<Integer, Integer>());
			}
			if (!avrg.containsKey(id1)) {
				avrg.put(id1, new HashMap<Integer, Integer>());
			}
			if (!real.containsKey(id1)) {
				real.put(id1, new HashMap<Integer, Integer>());
			}
			gamlet.get(id1).put(id2, gamletd);
			avrg.get(id1).put(id2, avrgd);
			real.get(id1).put(id2, reald);

		}

		allEdges.close();

		ArrayList<Integer> realLengths = new ArrayList<Integer>();

		for (OperaScaffold s : opera) {
			Vertex prev = null;
			Vertex prev2 = null;
			OperaLine olp = null;
			boolean pb = false;
			int len = 0;
			for (OperaLine ol : s.contig) {
				Vertex v = getInfo(ol.name).v;
				if (prev != null) {
					if (v.realDistTo(prev) > Data.NORMAL_DISTRIBUTION_CENTER
							+ 3 * Data.NORMAL_DISTRIBUTION_DEVIATION) {
						realLengths.add(len);
						len = 0;
						pb = true;
					} else if (!pb
							&& prev2 != null
							&& Math.abs(prev2.realDistTo(prev) + prev.info.len
									+ prev.realDistTo(v) - prev2.realDistTo(v)) > Data.NORMAL_DISTRIBUTION_DEVIATION
							&& prev2.realDistTo(prev) < Data.NORMAL_DISTRIBUTION_CENTER
									+ 3 * Data.NORMAL_DISTRIBUTION_DEVIATION) {
						realLengths.add(len);
						len = 0;
						pb = true;
					} else {
						int id1 = Math.min(prev.info.id, v.info.id);
						int id2 = Math.max(prev.info.id, v.info.id);
						if (gamlet.containsKey(id1)
								&& gamlet.get(id1).containsKey(id2)) {
							operaDist.println(id1 + "\t" + id2 + "\t"
									+ real.get(id1).get(id2) + "\t"
									+ gamlet.get(id1).get(id2) + "\t"
									+ avrg.get(id1).get(id2) + "\t" + olp.dist);

						}
						pb = false;
					}
				}
				len += v.info.len;
				prev2 = prev;
				prev = v;
				olp = ol;
			}
			realLengths.add(len);
		}

		Collections.sort(realLengths);
		s2 = 0;
		for (int i = realLengths.size() - 1; i >= 0; i--) {
			int x = realLengths.get(i);
			s2 += x;
			if (2 * s2 >= full) {
				System.err.println("N50(cut): " + x);
				break;
			}
		}
		operaDist.close();
	}

	private static int realDist(Contig c1, Contig c2) {
		int ans = Integer.MAX_VALUE / 2;
		for (int x : c1.realPos) {
			for (int y : c2.realPos) {
				ans = Math.min(ans, Math.abs(x - y) - (c1.len + c2.len) / 2);
				ans = Math.min(ans, Math.abs(Data.dnaLength + x - y)
						- (c1.len + c2.len) / 2);
				ans = Math.min(ans, Math.abs(Data.dnaLength - x + y)
						- (c1.len + c2.len) / 2);
			}
		}
		return ans;
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
			Contig ci = getInfo(name);
			ci.len = contig.length();
			// ci.seq = contig;
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
		Contig s1, s2;

		public InfoPair(String s1, String s2) {
			this.s1 = getInfo(s1);
			this.s2 = getInfo(s2);
		}

		public InfoPair(Contig s1, Contig s2) {
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

	static private String transform(String name) {
		return name.replace(' ', '_');
	}

	static private HashMap<String, Contig> Contig = new HashMap<String, Contig>();

	static public Contig getInfo(String s) {
		if (!Contig.containsKey(s)) {
			Contig.put(s, new Contig(Contig.size(), s));
		}
		return Contig.get(s);
	}

}
*/