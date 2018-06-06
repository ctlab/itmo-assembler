package ru.ifmo.genetics.tools.scaffolder;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class RecreateContigs {
	public static void main(String[] args) throws IOException {
		System.out.println("loading positions:");
		HashMap<String, ArrayList<BlastAlignment>> contigMap = toMap(BlastParser
				.parse(args[1]));
		Scanner in = new Scanner(new File(args[0]));
		StringBuilder ref = new StringBuilder();
		in.nextLine();
		while (in.hasNext()) {
			ref.append(in.next());
		}
		char[] r = ref.toString().toCharArray();
		PrintWriter out = new PrintWriter(args[2]);
		for (String s : contigMap.keySet()) {
			BlastAlignment al = contigMap.get(s).get(0);
			out.println(">" + al.qseqid);
			for (int i = Math.min(al.sstart, al.send); i < Math.min(r.length,
					Math.max(al.sstart, al.send)); i++) {
				out.print(r[i]);
			}
			out.println();
		}
		in.close();
		out.close();
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
}
