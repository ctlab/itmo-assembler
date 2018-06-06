#!/usr/bin/python

import dna
import sys


for l in sys.stdin:
    l = dna.reverse_complemented(l.strip())
    print l
