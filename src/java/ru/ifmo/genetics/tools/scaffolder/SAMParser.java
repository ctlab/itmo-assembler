package ru.ifmo.genetics.tools.scaffolder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class SAMParser {
	public static SAMAlignment[] parse(String fname) throws IOException {
		return parse(new File(fname));
	}

	public static SAMAlignment[] parse(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		ArrayList<SAMAlignment> ans = new ArrayList<SAMAlignment>();
		while (true) {
			String s = br.readLine();
			if (s == null) {
				br.close();
				break;
			}
			if (s.startsWith("@")) {
				continue;
			}
			SAMAlignment sa = new SAMAlignment(s);
			if (sa.rname.equals("*")) {
				continue;
			}
			ans.add(sa);
		}
		SAMAlignment[] res = new SAMAlignment[ans.size()];
		for (int i = 0; i < res.length; i++) {
			res[i] = ans.get(i);
		}
		System.err.println("SAM Alignments: " + res.length);
		return res;
	}
}
