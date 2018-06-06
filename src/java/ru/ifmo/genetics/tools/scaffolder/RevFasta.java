package ru.ifmo.genetics.tools.scaffolder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class RevFasta {
	public static void main(String[] args) throws FileNotFoundException {
		Scanner in = new Scanner(new File(args[0]));
		PrintWriter out = new PrintWriter(args[1]);
		while (in.hasNext()) {
			String comment = in.nextLine();
			String seq = in.nextLine();
			out.println(comment);
			seq = compRev(seq);
			out.println(seq);
		}
		in.close();
		out.close();
	}

	private static String compRev(String seq) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < seq.length(); i++) {
			sb.append(comp(seq.charAt(i)));
		}
		return sb.reverse().toString();
	}

	private static char comp(char c) {
		switch (c) {
		case 'A':
			return 'T';
		case 'T':
			return 'A';
		case 'G':
			return 'C';
		case 'C':
			return 'G';

		default:
			throw new Error("Unknown: " + c);
		}
	}
}
