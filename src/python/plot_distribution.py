#!/usr/bin/python

from sys import *

def number(s):
    if s.isdigit():
        return int(s)
    return float(s)

if len(argv) != 3:
    print "usage: plot_distribution.py <min_value> <max_value>"
    exit(1)

min_value = number(argv[1])
max_value = number(argv[2])
length = max_value - min_value

print min_value, max_value
groups = 20

stat = [0] * groups

for l in stdin:
    x = number(l.strip())
    stat[min(int(groups * (x - min_value) / length), groups - 1)] += 1

for i in xrange(groups):
    left = min_value + i * length / groups
    right = min_value + (i + 1) * length / groups
    middle = (left + right) / 2
    print left, middle, right, stat[i]
