#!/usr/bin/python
from bowtie import *

import sys
from math import log
from itertools import *

total = {}
bad = {}

k = 2
eps = 1e-100

for l in sys.stdin:
    if not l:
        break
    l = l[:-1]
    hit = bowtie_hit(l)
    for i in xrange(0, len(hit.original_read)):
        context_nuc = hit.original_read[max(0, i + 1 - k):i + 1]
        context_qual = hit.original_qual[max(0, i + 1 - k):i + 1]

        p = (context_nuc, context_qual)
        if not p in total:
            total[p] = 0
            bad[p] = 0

        total[p] += 1
        if i in hit.parsed_mismatches:
            bad[p] += 1

for ((nucs, quals), total_count) in total.iteritems():
    bad_count = bad[(nucs, quals)]
    ratio = log(float(bad_count)/total_count + eps, 10)
    context = "\t".join(reversed(["%s\t%d" % (n, ord(q) - 33) for  (n, q) in izip(nucs, quals)]))
    print "%.2f\t%d\t%d\t%s" % (ratio, bad_count, total_count, context)
