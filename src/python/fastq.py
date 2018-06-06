from itertools import *

def grouper(n, iterable, fillvalue=None):
    "grouper(3, 'ABCDEFG', 'x') --> ABC DEF Gxx"
    args = [iter(iterable)] * n
    return izip_longest(fillvalue=fillvalue, *args)

def iter_fastq_stream_items(stream):
    last_id = None
    last_seq = ""
    for g in grouper(4, stream):
        g = tuple(imap(lambda s: s.strip(), g))
        (l1, l2, l3, l4) = tuple(g)
        assert l1[0] == "@"
        assert l3[0] == "+"

        yield (l1[1:], l2, l4)
    return

def iter_fastq_stream(stream):
    return imap(lambda (x, y, z): (y, z), iter_fastq_stream_items(stream))

def iter_fastq_file(filename):
    return iter_fastq_stream(open(filename))

def iter_fastq_file_items(filename):
    return iter_fastq_stream_items(open(filename))
