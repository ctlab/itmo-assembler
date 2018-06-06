#!/usr/bin/python

from dna import *
from fasta import *

import sys
from itertools import *

nucs 

def average(l):
    res = 0
    for x in l:
        res += x
    return res / len(l)


def stat(s):
    res = {}
    for x in s:
        if not x in res:
            res[x] = 0
        res[x] += 1
    return res 

def dict_apply(f, d):
    for v in d.itervalues():
        f(v)

def endeep_dict(d):
    res = {}
    for (k, v) in d.iteritems():
        cur = res
        for k1 in k[:-1]:
            if not k1 in cur:
                cur[k1] = {}
            cur = cur[k1]
        cur[k[-1]] = v
    return res



def norm(d):
    sum = 0
    for k in d.keys():
        if not k in nucs:
            del d[k]

    for v in d.itervalues():
        sum += v

    for k in nucs:
        if not k in d:
            d[k] = 0
        d[k] = d[k] / float(sum)

def average_dict_diff(d1, d2):
    res = 0
    for k in d1.iterkeys():
        res += abs(d1[k] - d2[k])
    return res / float(len(d1))

def dep(g, s0, d):
    s1 = stat(izip(g, g[d:]))
    s1 = endeep_dict(s1)
    dict_apply(norm, s1)
    return average([average_dict_diff(s, s0) for (k, s) in s1.iteritems() if k in nucs])

if __name__ == "__main__":
    g = iter_fasta_file(sys.argv[1]).next()
    s0 = stat(g)
    norm(s0)
    for i in range(0, 23):
        x = 2 ** i
        print x, dep(g, s0, x)

