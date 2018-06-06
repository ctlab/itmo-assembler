package ru.ifmo.genetics.tools.scaffolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Library {
	Collection<MatePair> reads;
	Map<InfoPair, Collection<MatePair>> map;
	double insertSize;
	double deviation;
	int readLength;
	int id;
	int same;

	public int size() {
		return same + reads.size();
	}

	public Library(File reads1, File reads2, int id) throws IOException {
		this.id = id;
		Map<String, ArrayList<SAMAlignment>> mapa = null;
		Map<String, ArrayList<SAMAlignment>> mapb = null;
		mapa = toMap(SAMParser.parse(reads1));
		mapb = toMap(SAMParser.parse(reads2));
		{
			Set<String> hs = new HashSet<String>();
			hs.addAll(mapa.keySet());
			hs.addAll(mapb.keySet());
		}

		map = new TreeMap<InfoPair, Collection<MatePair>>();
		reads = new ArrayList<MatePair>();
		int miss = 0;
		int match = 0;
		same = 0;
		List<Integer> sameList = new ArrayList<Integer>();
		for (String s1 : mapa.keySet()) {
			char last = s1.charAt(s1.length() - 1);
			String s2 = s1.substring(0, s1.length() - 1)
					+ (last == '1' ? "2" : (last == '2' ? "1"
							: (last == 'r' ? "f" : "r")));
			if (!mapb.containsKey(s2)/*
									 * || mapa.get(s1).size() > 10 ||
									 * mapb.get(s2).size() > 10
									 */) {
				if (!mapb.containsKey(s2)) {
					miss++;
				} else {
					match++;
				}
				// System.err.println(s1 + "\t" + s2);
				continue;
			}
			for (SAMAlignment a : mapa.get(s1)) {
				for (SAMAlignment b : mapb.get(s2)) {
					if (readLength == 0) {
						readLength = a.seq.length();
						System.err.println("read length: " + readLength);
					}
					Contig.getInfo(a.rname).cover += readLength;
					Contig.getInfo(b.rname).cover += readLength;
					if (a.rname.equals(b.rname)) {
						int len = Math.abs(a.pos - b.pos) + readLength;
						sameList.add(len);
						same++;
						continue;
					}
					InfoPair sp = new InfoPair(a.rname, b.rname);
					if (!map.containsKey(sp)) {
						InfoPair rev = sp.reverse();
						map.put(sp, new ArrayList<MatePair>());
						map.put(rev, new ArrayList<MatePair>());
					}
					map.get(sp).add(new MatePair(a, b, this));
					map.get(sp.reverse()).add(new MatePair(b, a, this));
				}
			}
		}

		System.err.println("library: " + map.size());

		for (Collection<MatePair> col : map.values()) {
			reads.addAll(col);
		}

		{
			double sameSum = 0;
			for (int len : sameList) {
				sameSum += len;
			}
			sameSum /= sameList.size();
			System.err.println("mean insert size: " + sameSum);
			double d = 0;
			for (int len : sameList) {
				d += (len - sameSum) * (len - sameSum);
			}
			d /= sameList.size();
			d = Math.sqrt(d);
			System.err.println("standard diviation: " + d);
			// System.out.println("mean graph:");
			// insertSize = 3000;
			// deviation = 300;
			// double max = Double.NEGATIVE_INFINITY;
			// int best = -1;
			// for (int i = 2700; i <= 3500; i++) {
			// insertSize = i;
			// deviation = 0.1 * i;
			// double q = DistanceFinder
			// .getProbabilityThatMatchesInsideContig(Contig.getInfo()
			// .values(), insertSize, deviation);
			// double p = 0;
			// for (int len : sameList) {
			// p += -(len - insertSize) * (len - insertSize) / 2
			// / deviation / deviation
			// - Math.log(DistanceFinder.sqrt2Pi * deviation);
			// }
			// if (max < Math.log(1 - q) * (Data.allReads - same) + p) {
			// max = Math.log(1 - q) * (Data.allReads - same) + p;
			// best = i;
			// }
			// System.out.println(i + "\t" + Math.log(1 - q)
			// * (Data.allReads - same) + "\t" + p);
			// }
			// System.err.println("mean insert size (fixed): " + best);
			//
			// System.out.println("mean graph2:");
			// insertSize = 3000;
			// deviation = 300;
			// for (int i = 200; i <= 500; i++) {
			// deviation = i;
			//
			// double q = DistanceFinder
			// .getProbabilityThatMatchesInsideContig(Contig.getInfo()
			// .values(), insertSize, deviation);
			// double p = 0;
			// for (int len : sameList) {
			// p += -(len - insertSize) * (len - insertSize) / 2
			// / deviation / deviation
			// - Math.log(DistanceFinder.sqrt2Pi * deviation);
			// }
			// System.out.println(i + "\t" + Math.log(1 - q)
			// * (Data.allReads - same) + "\t" + p);
			// }

			insertSize = sameSum;
			d = 0;
			for (int len : sameList) {
				d += (len - insertSize) * (len - insertSize);
			}
			d /= sameList.size();
			d = Math.sqrt(d);
			deviation = d;
		}

		// insertSize = 3000;
		// deviation = 0.1 * insertSize;
	}

	private static Map<String, ArrayList<SAMAlignment>> toMap(SAMAlignment[] a) {
		Map<String, ArrayList<SAMAlignment>> map = new TreeMap<String, ArrayList<SAMAlignment>>();
		for (SAMAlignment sa : a) {
			if (!map.containsKey(sa.qname)) {
				map.put(sa.qname, new ArrayList<SAMAlignment>());
			}
			map.get(sa.qname).add(sa);
		}
		return map;
	}

}
