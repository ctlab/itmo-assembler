#!/usr/bin/python

import sys

from fasta import *

if __name__ == "__main__":
    for (i, s) in iter_fasta_stream_items(sys.stdin):
        sys.stdout.write(">%s\n%s\n" % (i, s))


