package ru.ifmo.genetics.tools.scaffolder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class BlastParser {
	public static BlastAlignment[] parse(String fname) throws IOException {
		return parse(new File(fname));
	}

	public static BlastAlignment[] parse(File file) throws IOException {
		System.out.println("Parsing blast");
		// int line = 0;
		BufferedReader br = new BufferedReader(new FileReader(file));
		ArrayList<BlastAlignment> ans = new ArrayList<BlastAlignment>();
		while (true) {
			String s = br.readLine();
			// line++;
			// System.out.println(line);
			if (s == null) {
				br.close();
				break;
			}
			if (s.startsWith("#")
					|| new StringTokenizer(s, "\t").countTokens() != 15) {
				continue;
			}
			ans.add(new BlastAlignment(s));
		}
		BlastAlignment[] res = new BlastAlignment[ans.size()];
		for (int i = 0; i < res.length; i++) {
			res[i] = ans.get(i);
		}
		return res;
	}
}
