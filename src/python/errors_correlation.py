#!/usr/bin/python

import sys
from math import log
from itertools import *

total = {}
bad = {}

k = 2
eps = 1e-100

n2m = {'A' : 0, 'T' : 1, 'G' : 2, 'C' : 3}

key_expr = sys.argv[1]

bad = {}
total = {}

for l in sys.stdin:
    if not l:
        break
    l = l[:-1]
    x = l.split("\t")
    bc = int(x[1])
    tc = int(x[2])
    context = x[3:]
    if len(context) != 2 * k:
        continue

    n1 = context[0]
    q1 = context[1]
    m1 = n2m[n1]
    n2 = context[2]
    q2 = context[3]
    m2 = n2m[n2]

    key = eval(key_expr)
    if not key in bads:
        bad[key] = 0
        total[key] = 0

    bad[key] += bc
    total[key] += tc
    

