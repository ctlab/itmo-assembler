package ru.ifmo.genetics.tools.scaffolder;
import java.util.StringTokenizer;

public class SAMAlignment {

	@Override
	public String toString() {
		return qname + "\t" + rname + "\t" + pos;
	}

	public SAMAlignment(String s) {
		StringTokenizer st = new StringTokenizer(s, "\t");
		qname = st.nextToken().trim();
		if (qname.contains(" ")) {
			qname = qname.substring(0, qname.indexOf(' '));
		}
		flag = Integer.parseInt(st.nextToken());
		rname = st.nextToken();
		pos = Integer.parseInt(st.nextToken());
		mapq = Integer.parseInt(st.nextToken());
		cigar = st.nextToken();
		rnext = st.nextToken();
		pnext = Integer.parseInt(st.nextToken());
		tlen = Integer.parseInt(st.nextToken());
		seq = st.nextToken();
		qual = st.nextToken();
	}

	boolean isReverseComplimented() {
		return (flag & 0x10) != 0;
	}

	/**
	 * query name
	 */
	public String qname;
	/**
	 * bitfalg 0x1 template has multiple segments in sequencing 0x2 each segment
	 * properly aligned according to aligner 0x4 segment unmapped 0x8 next
	 * segment in the template unmapped 0x10 SEQ being reverse complemented 0x20
	 * SEQ of the next segment in the template being reversed 0x40 the first
	 * segment in the template 0x80 the last segment in the template 0x100
	 * secondary alignment 0x200 not passing quality controls 0x400 PRC or
	 * optical duplicate
	 */
	int flag;
	/**
	 * reference sequence name
	 */
	public String rname;
	/**
	 * 1-based leftmost mapping position 0 for unmapped
	 */
	public int pos;
	/**
	 * mapping quality, -10 log_10 Pr{mapping position is wrong} 255 - mapping
	 * quality is not available
	 */
	int mapq;
	/**
	 * CIGAR string
	 */
	String cigar;
	/**
	 * reference sequence name of the next segment in the template
	 */
	String rnext;
	/**
	 * position of the next segment in the template
	 */
	int pnext;
	/**
	 * template length
	 */
	int tlen;
	/**
	 * sequence
	 */
	public String seq;
	/**
	 * quality (same as fastq)
	 */
	String qual;
}
