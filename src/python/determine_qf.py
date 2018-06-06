#!/usr/bin/python

from fastq import *
import sys
from itertools import *


min_phred = None
max_phred = None

for phred in chain(*imap(lambda (seq, phreds): phreds, iter_fastq_stream(sys.stdin))):
    if not min_phred:
        min_phred = phred
        max_phred = phred

    min_phred = min(phred, min_phred)
    max_phred = max(phred, max_phred)

if 33 <= ord(min_phred) < 64:
    print "sanger"
else:
    print "illumina"


