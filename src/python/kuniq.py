#!/usr/bin/python -u

import sys
import fasta
from math import *

sequences = list(fasta.iter_fasta_stream(sys.stdin))

print "#k\tuniall\tunireps\trepeats\ttotal\trepeats/total ratio"
for k in xrange(1, 200, 4):
    allKmers = set()
    repeatedKmers = set()
    repeats = 0
    total = 0
    for s in sequences:
        for i in xrange(len(s) - k + 1):
            kmer = s[i:i+k]
            if kmer in allKmers:
                repeatedKmers.add(kmer)
                repeats += 1
            allKmers.add(kmer)

    for s in sequences:
        last_unique = False
        last_contig_size = 0
        for i in xrange(len(s) - k + 1):
            kmer = s[i:i+k]
            total += 1
            if kmer in repeatedKmers:



    print "%d\t%d\t%d\t%d\t%d\t%f\t%f" % (k, len(allKmers),
            len(repeatedKmers), repeats, total, repeats / float(total),
            log(repeats / float(total)))
