#!/usr/bin/python
import sys

if len(sys.argv) != 1:
    print "usage: ./edges_to_dotfile.py < edges_file > dot_file"
    sys.exit(1)

print "digraph g {"

for l in sys.stdin.readlines():
    l = l[:-1]
    u, v = l.split();
    print '"%s" -> "%s";' % (u, v)

print "}"

