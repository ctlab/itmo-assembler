#!/usr/bin/python

import sys
import re
from itertools import *
from subprocess import *

from fasta import *

from intervals_tree import *
from blast import *

import argparse

def iter_contig_hit_sets(aligns):
    cur_id = None
    cur_list = []
    for l in aligns:
        if l.startswith("#"):
            continue
        hit = blast_hit(l)
        if cur_id != None and hit.qseqid != cur_id:
            yield (cur_id, cur_list)
            cur_list = []

        cur_id = hit.qseqid
        cur_list.append(hit)

    yield (cur_id, cur_list)

def average(l):
    return sum(l)/float(len(l))

def nk(lengths):
    lengths = sorted(lengths)
    res = []
    s = 0
    sl = sum(lengths)
    last_k = -1
    for a in reversed(lengths):
        s += a
        k = s * 100 / sl
        for i in range(last_k + 1, k + 1):
            res.append(a)
        last_k = k
    if len(lengths) > 0:
        m = min(lengths)
    else:
        m = 0;
    for i in range(last_k + 1, 101):
        res.append(m)
    return res

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Computes assembly statistic")

    parser.add_argument(
            '--contigs',
            dest="contigs_file",
            metavar="CONTIGS",
            help="file with assembled contigs")

    parser.add_argument(
            '--reference',
            dest="reference_file",
            metavar="REFERENCE",
            help="file with reference genome")

    parser.add_argument(
            '--alignments',
            dest="alignments_file",
            metavar="ALIGNMENTS",
            help="file with BLAST-alignments of CONTIGS to the REFERENCE")

    parser.add_argument(
            '--nx-plot',
            dest="nx_plot_file",
            metavar="NX_PLOT",
            help="file to store date for Nx plot")


    """
    # :ToDo: return support for it
    parser.add_argument(
            '--visual-alignments',
            dest="visual_alignments_file",
            metavar="VISUAL_ALIGNMENTS",
            help="file to store visualized alignment")
    """

    parser.add_argument(
            '--coverage-stat',
            dest="coverage_stat_file",
            metavar="COVERAGE_STAT",
            help="file to store coverage stat")

    args = parser.parse_args()


    if args.contigs_file:
        lengths = filter(lambda x: x > 0, imap(lambda s: len(s),
            iter_fasta_file(args.contigs_file)))

        if len(lengths) == 0:
            print "Assembly is empty!"
            sys.exit(1)

        print "cnt: ", len(lengths)
        print "sum: ", sum(lengths)
        print "max: ", max(lengths)
        print "min: ", min(lengths)
        print "avg: ", average(lengths)
        nx = nk(lengths)
        print "n50: ", nx[50]
        print "n90: ", nx[90]

        if args.nx_plot_file:
            pf = open(args.nx_plot_file, "w")
            for (x, n) in izip(count(), nx):
                pf.write("%d %d\n" % (x, n))
            pf.close()


    if args.alignments_file:
        if not args.reference_file:
            print "Reference file should be given to get alignment stat"
        genome_length = 0
        coverage = {}

        aligns = {}

        if args.contigs_file:
            for (seq_id, seq) in iter_fasta_file_items(args.contigs_file):
                seq_id = seq_id.split()[0]
                aligns[seq_id] = 0


        shifted_contigs = {}
        inserts = {}

        ref = {}

        for (seq_id, seq) in iter_fasta_file_items(args.reference_file):
            seq_id = seq_id.split()[0]
            seq_len = len(seq)
            ref[seq_id] = seq

            genome_length += seq_len
            coverage[seq_id] = IntervalsTree(0, seq_len)
            shifted_contigs[seq_id] = []
            inserts[seq_id] = {}

        alignments = open(args.alignments_file)

        worst_aligned = []

        eq100 = 0
        less100 = 0
        less99 = 0
        less95 = 0
        less90 = 0
        less50 = 0

        for (contig_id, hits) in iter_contig_hit_sets(alignments):
            worst_aligned = list(sorted(worst_aligned + [(hits[0].pmatch, hits)]))[:5]

            if hits[0].positive != hits[0].qlen:
                less100 += 1
            else:
                eq100 += 1

            if hits[0].pmatch < 0.99:
                less99 += 1

            if hits[0].pmatch < 0.95:
                less95 += 1

            if hits[0].pmatch < 0.90:
                less90 += 1

            if hits[0].pmatch < 0.50:
                less50 += 1

            if hits[0].pmatch >= 0.9:
                threshold = 0.9
            else:
                threshold = 0.3


            for hit in hits:
                seq_id = hit.qseqid

                chromosome = hit.sseqid
                rc = hit.send < hit.sstart

                if hit.pmatch > 0.9:
                    aligns[seq_id] += 1

                if hit.pmatch >= 0.9:
                #if hit.positive == hit.qlen:
                    coverage[chromosome].add(min(hit.sstart, hit.send), max(hit.send, hit.sstart) + 1, 1)
                """
                # :ToDo: parse BTOP string
                begin = pos
                anchored = False
                for (count, operation) in cigar:
                    if operation in "MX=":
                        # good align
                        anchored = True
                        coverage[chromosome].add(pos, pos + count, 1)
                        pos += count
                    elif operation in "I":
                        # insert
                        if not pos in inserts[chromosome]:
                            inserts[chromosome][pos] = 0
                        inserts[chromosome][pos] = max(inserts[chromosome][pos], count)
                    elif operation in "D":
                        # delete
                        pos += count
                    elif operation in "SH":
                        # clip
                        if not anchored:
                            begin -= count
                    else:
                        print "WARNING: CIGAR operation '%s' not supported" % operation


                

                if args.visual_alignments_file:
                    shifted_contigs[chromosome].append((begin, vals[9], cigar, seq_id + "_" + rc_flag))
                """


        print "genome length:", genome_length
        print "eq100:", eq100
        print "less100:", less100
        print "less99:", less99
        print "less95:", less95
        print "less90:", less90
        print "less50:", less50

        holes = 0

        holes_out = open("holes", "w")

        coverage_stat = {0 : 0}
        for (chr_id, t) in coverage.iteritems():
            is_hole = False
            hole_start = -1
            hole_end = -1
            for i in xrange(t.left, t.right):
                x = t.get(i);
                if x == 0:
                    if not is_hole:
                        hole_start = i
                    is_hole = True
                elif is_hole:
                    hole_end = i
                    holes_out.write("%s %d %d %s\n" % (chr_id, hole_start, hole_end, ref[chr_id][hole_start:hole_end]))
                    holes += 1
                    is_hole = False

                if not x in coverage_stat:
                    coverage_stat[x] = 0

                coverage_stat[x] += 1

            if is_hole:
                hole_end = i
                holes_out.write("%s %d %d\n" % (chr_id, hole_start, hole_end))
                holes += 1
        holes_out.close()

        print "coverage by 90-percent ok contigs stat:"
        for (val, cnt) in sorted(coverage_stat.items()):
            if val > 20:
                print "There is coverage greater than 20, interrupting"
                break
            print val, cnt

        if args.coverage_stat_file:
            csf = open(args.coverage_stat_file, "w")
            for (val, cnt) in sorted(coverage_stat.items()):
                csf.write("%d %d" % (val, cnt))
            csf.close()

        print "There are %d holes" % holes
        print "%.2f%% of genome non-covered" % (100. * coverage_stat[0] / genome_length)

        not_aligned = open("not_aligned", "w")
        aligns_stat = {}
        for (contig, t) in aligns.iteritems():
            if t == 0:
                not_aligned.write("%s\n" % contig)
            if not t in aligns_stat:
                aligns_stat[t] = 0
            aligns_stat[t] += 1
        not_aligned.close()

        print "aligns stat:"
        for (val, cnt) in sorted(aligns_stat.items()):
            print val, cnt

        print "worst aligned (pmatch, qseqid, sseqid, qlen, length, positive, qstart, qend, sstart, send):"
        for (q, hits) in worst_aligned:
            print "Contig", hits[0].qseqid
            for hit in hits[:2]:
                print "\t".join(map(str, [hit.pmatch, hit.qseqid, hit.sseqid, hit.qlen, hit.length, hit.positive, hit.qstart, hit.qend, hit.sstart, hit.send]))
        
        max_coverage = reversed(sorted(coverage_stat.keys())).next()
        print "max coverage:", max_coverage

        
        # :ToDo: return
        #if args.visual_alignments_file:
        if False:
            if not args.reference_file:
                print "Reference file should be given to visualize assembly"
            from bisect import bisect_right

            vaf = open(args.visual_alignments_file, "w")


            class LinkedString:
                def __init__(self):
                    self.parts = []
                    self.length = 0
                    self.end = 0
                    self.last_insert = False

                def raw_append(self, s):
                    self.parts.append(s)
                    self.length += len(s)


                def append_insert(self, s):
                    self.raw_append(s)
                    self.last_insert = True

                def append_seq(self, s):
                    self.raw_append(s)
                    self.end += len(s)
                    self.last_insert = False

                def __str__(self):
                    return "".join(self.parts)

            for (seq_id, seq) in iter_fasta_file_items(args.reference_file):
                seq_id = seq_id.split()[0]
                cur_shifted_contigs = sorted(shifted_contigs[seq_id])
                offset = 0
                if len(cur_shifted_contigs) != 0:
                    t = cur_shifted_contigs[0][0]
                    if t < 0:
                        offset = -t


                cur_inserts = inserts[seq_id]
                cur_inserts_positions = sorted(cur_inserts.iterkeys())

                def add_seq(line, seq, is_insert = False):
                    ref_pos = line.end - offset
                    if is_insert:
                        ins_len = cur_inserts[ref_pos]
                        assert ins_len >= len(seq)
                        line.append_insert(seq)
                        line.append_insert("." * (ins_len - len(seq)))
                    else:
                        while seq:
                            ref_pos = line.end - offset
                            if ref_pos in cur_inserts and not line.last_insert:
                                ins_len = cur_inserts[ref_pos]
                                line.append_insert("." * ins_len)

                            cut_length = len(seq)
                            i = bisect_right(cur_inserts_positions, ref_pos)
                            if i < len(cur_inserts_positions):
                                cut_length = cur_inserts_positions[i] - ref_pos

                            line.append_seq(seq[:cut_length])
                            seq = seq[cut_length:]
                    

                pseq = LinkedString()
                add_seq(pseq, " " * offset)
                add_seq(pseq, seq)


                L = pseq.length
                upper_bound = "v" * L
                lower_bound = "^" * L
                vaf.write("%s\n" % seq_id)
                vaf.write("%s\n" % upper_bound)
                vaf.write("%s\n" % pseq)

                lines = []


                for (pos, contig, cigar, seq_id) in cur_shifted_contigs:
                    line_to_put_in = 0
                    while line_to_put_in < len(lines):
                        if pos + offset >= lines[line_to_put_in].end:
                            break
                        line_to_put_in += 1

                    if line_to_put_in == len(lines):
                        lines.append(LinkedString())

                    add_seq(lines[line_to_put_in], " " * (pos + offset - lines[line_to_put_in].end))

                    x = 0
                    for (count, operation) in cigar:
                        if operation in "D":
                            add_seq(lines[line_to_put_in], "x" * count)
                        elif operation in "MX=I":
                            add_seq(lines[line_to_put_in], contig[x:x+count], operation == "I")
                            x += count
                        elif operation in "SH":
                            add_seq(lines[line_to_put_in], contig[x:x+count].lower())
                            x += count
                        else:
                            assert False, operation

                    add_seq(lines[line_to_put_in], "_%s_ " % seq_id)

                for line in lines:
                    s = str(line)
                    vaf.write("%s%s\n" % (s, " " * (L - len(s))))
                vaf.write("%s\n" % lower_bound)

            vaf.close()

