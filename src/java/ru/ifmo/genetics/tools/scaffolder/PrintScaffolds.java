package ru.ifmo.genetics.tools.scaffolder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;

public class PrintScaffolds {
	public static void main(String[] args) throws FileNotFoundException {
		Locale.setDefault(Locale.US);
		loadContigs(args[0]);

		System.err.println("Loading scaffolds info...");
		ArrayList<OperaScaffold> opera = OperaParser.parse(args[1]);
		System.err.println();

		System.err.println("Generating output...");
		PrintWriter out = new PrintWriter(args[2]);
		for (OperaScaffold s : opera) {
			out.println(">scaffold" + s.number);
			StringBuilder sb = new StringBuilder();
			for (OperaLine ol : s.contig) {
				String seq = getInfo(ol.name).seq;
				sb.append(seq);
				if (ol.dist < 0) {
					sb.delete(sb.length() + ol.dist, sb.length());
				} else if (ol.dist > 0) {
					for (int i = 0; i < ol.dist; i++) {
						sb.append("N");
					}
				}
			}
			System.err.println("Scaffold " + s.number + ": " + sb.length() + " nucs"); 
			out.println(sb.toString());
		}
		out.close();
	}

	private static void loadContigs(String string) throws FileNotFoundException {
		System.err.println("Loading contigs...");
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
			ci.seq = contig;
			System.err.println("Loaded contig with " + ci.seq.length() + " nucs"); 
		}
		in.close();
		System.err.println();
	}

	static private HashMap<String, Contig> Contig = new HashMap<String, Contig>();

	static public Contig getInfo(String s) {
		if (!Contig.containsKey(s)) {
			Contig.put(s, new Contig(Contig.size(), s));
		}
		return Contig.get(s);
	}
}
