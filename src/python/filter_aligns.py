#!/usr/bin/python

from sys import stdin

from blast import *

def contig_hit_sets(aligns):
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


for (contig_id, hs) in contig_hit_sets(stdin):
    if hs[0].positive == hs[0].qlen:
        continue

    if hs[0].pmatch >= 0.9:
        threshold = 0.9
        continue
    else:
        threshold = 0.3
    threshold = 0.3

    print "# Contig:", contig_id
    for hit in hs:
        if hit.pmatch < 0.3:
            continue
        #if hit.length <= hit.qlen - 3:
        #    continue
        print "%s\t%s" % (hit.pmatch, hit)
