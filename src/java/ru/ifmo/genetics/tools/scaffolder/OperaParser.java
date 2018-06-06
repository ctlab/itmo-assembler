package ru.ifmo.genetics.tools.scaffolder;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class OperaParser {

	public static ArrayList<OperaScaffold> parse(String s)
			throws FileNotFoundException {
		return parse(new File(s));
	}

	private static ArrayList<OperaScaffold> parse(File file)
			throws FileNotFoundException {
		return parse(new Scanner(file));
	}

	private static ArrayList<OperaScaffold> parse(Scanner in) {
		ArrayList<OperaScaffold> ans = new ArrayList<OperaScaffold>();
		in.next();
		int cnt = 1;
		while (in.hasNext()) {
			in.next();
			OperaScaffold os = new OperaScaffold();
			os.length = in.nextInt();
			os.number = cnt++;
			in.next();
			in.next();
			while (in.hasNext()) {
				String s = in.next();
				if (s.startsWith(">")) {
					break;
				}
				OperaLine ol = new OperaLine();
				ol.name = s;
				ol.or = in.next();
				ol.len = in.nextInt();
				ol.dist = in.nextInt();
				in.nextLine();
				os.contig.add(ol);
			}
			ans.add(os);
		}
		return ans;
	}

}
