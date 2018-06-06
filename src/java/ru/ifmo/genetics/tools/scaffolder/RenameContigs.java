package ru.ifmo.genetics.tools.scaffolder;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Scanner;

public class RenameContigs {
	public static void main(String[] args) throws FileNotFoundException {
		Scanner in = new Scanner(new FileReader(args[0]));
		PrintWriter out = new PrintWriter(args[1]);
		int cnt = 0;
		String next = in.nextLine();
		while (next != null) {
			if (next.startsWith(">")) {
				StringBuilder sb = new StringBuilder();
				next = in.nextLine();
				while (next != null && !next.startsWith(">")) {
					sb.append(next);
					next = in.hasNext() ? in.nextLine() : null;
				}
				out.println(">contig" + (++cnt));
				out.println(sb);
			}
		}
		in.close();
		out.close();
	}
}
