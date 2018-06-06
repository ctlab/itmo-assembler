#!/usr/bin/python

import sys

from fasta import *
from dna import *

if __name__ == "__main__":
    for (name, s) in iter_fasta_stream_items(sys.stdin):
        sys.stdout.write(">%s\n%s\n" % (name, s)) 
        sys.stdout.write(">%src\n%s\n" % (name, reverse_complemented(s))) 

