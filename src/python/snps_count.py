#!/usr/bin/python

import sys
from itertools import *

s1 = sys.stdin.readline()
s2 = sys.stdin.readline()

total = 0
for (i, c1, c2) in izip(count(), s1, s2):
    if c1 != c2:
        print i
        total += 1

print "total: %d" % total
