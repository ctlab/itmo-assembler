#!/usr/bin/python
from bowtie import *

from pysam import *
import sys
from itertools import *
from dna import *

holes_filename = sys.argv[1]
bam_filename = sys.argv[2]
out_prefix = sys.argv[3]

aligns = Samfile(bam_filename, "rb")


rs1 = open(out_prefix+"_1.fq", "w")
rs2 = open(out_prefix+"_2.fq", "w")

def reverse(s):
    return "".join(reversed(s))

for l in open(holes_filename):
    (chr_id, hole_start, hole_end, hole_seq) = l.split()
    hole_start = int(hole_start)
    hole_end = int(hole_end)
    print "hole (%d, %d)" % (hole_start, hole_end)
    printed = set()

    for r in aligns.fetch(chr_id, hole_start, hole_end):
        if not r.is_proper_pair:
            continue
        rr = aligns.mate(r)
        printed.add(r.qname)

        rs1.write(">%s hole %d-%d\n" % (r.qname, hole_start, hole_end))
        rs2.write(">%s hole %d-%d\n" % (r.qname, hole_start, hole_end))

        if r.mate_is_reverse:
            rs1.write("%s\n+\n%s\n" % (r.seq, r.qual))
            rs2.write("%s\n+\n%s\n" % (rc(rr.seq), reverse(rr.qual)))
        else:
            rs2.write("%s\n+\n%s\n" % (rc(r.seq), reverse(r.qual)))
            rs1.write("%s\n+\n%s\n" % (rr.seq, rr.qual))



rs1.close()
rs2.close()
