#!/usr/bin/python

import sys

from fasta import *

if __name__ == "__main__":
    sys.stdout.write("$")
    for s in iter_fasta_stream(sys.stdin):
        sys.stdout.write(s + "$")


