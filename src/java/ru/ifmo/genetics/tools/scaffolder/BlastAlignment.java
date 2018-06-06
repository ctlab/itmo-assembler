package ru.ifmo.genetics.tools.scaffolder;

import java.util.StringTokenizer;

public class BlastAlignment {
	public BlastAlignment(String s) {
		StringTokenizer st = new StringTokenizer(s, "\t");
		qseqid = st.nextToken();
		sseqid = st.nextToken();
		// pident =
		Double.parseDouble(st.nextToken());
		// qlen =
		Integer.parseInt(st.nextToken());
		// length =
		Integer.parseInt(st.nextToken());
		// positive =
		Integer.parseInt(st.nextToken());
		// mismatch =
		Integer.parseInt(st.nextToken());
		// gapopen =
		Integer.parseInt(st.nextToken());
		qstart = Integer.parseInt(st.nextToken());
		qend = Integer.parseInt(st.nextToken());
		sstart = Integer.parseInt(st.nextToken());
		send = Integer.parseInt(st.nextToken());
		// bitscore =
		Double.parseDouble(st.nextToken());
		// btop =
		st.nextToken();
		slen = Integer.parseInt(st.nextToken());
	}

	public String qseqid;
	public String sseqid;
	// double pident;
	// int qlen;
	// int length;
	// int positive;
	// int mismatch;
	// int gapopen;
	public int qstart;
	public int qend;
	public int sstart;
	public int send;
	// double bitscore;
	// String btop;
	public int slen;

}
