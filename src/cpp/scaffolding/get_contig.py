#!/usr/bin/python
import sys

if len(sys.argv) != 2:
    print "usage: ./gen_contig.py <id> < contigs_file > contig_file"
    sys.exit(1)

def reverse_complement(s):
    rc_dict = { 'A': 'T', 'T' : 'A', 'G' : 'C', 'C' : 'G' }
    return "".join(map(lambda x: rc_dict[x], reversed(s)))

printing = False
i = int(sys.argv[1])
n = (i / 2) + 1
rc = ((i % 2) == 1)

result = ""


for l in sys.stdin.readlines():
    l = l[:-1]
    if l.startswith(">"):
        n -= 1
        if n == 0:
            sys.stderr.write(l + "\n")
        continue

    if n < 0:
        break

    if n > 0:
        continue

    result += l

if rc:
    result = reverse_complement(result)

print result

