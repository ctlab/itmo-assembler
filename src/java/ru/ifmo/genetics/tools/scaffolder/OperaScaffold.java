package ru.ifmo.genetics.tools.scaffolder;
import java.util.ArrayList;

public class OperaScaffold {
	ArrayList<OperaLine> contig = new ArrayList<OperaLine>();
	int length;
	int number;

	public int getSum() {
		int sum = 0;
		for (OperaLine op : contig) {
			sum += op.len;
		}
		return sum;
	}
}
