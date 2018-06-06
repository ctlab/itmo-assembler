from itertools import *

def grouper(n, iterable, fillvalue=None):
    "grouper(3, 'ABCDEFG', 'x') --> ABC DEF Gxx"
    args = [iter(iterable)] * n
    return izip_longest(fillvalue=fillvalue, *args)

def iter_fasta_stream_items(stream):
    last_id = None
    last_seq = ""
    for l in stream:
        l = l.strip()
        if l.startswith(">"):
            if last_id:
                yield (last_id, last_seq)
            last_id = l[1:]
            last_seq = ""
        else:
            last_seq += l.upper()

    if last_id:
        yield (last_id, last_seq)

    return

def iter_fasta_stream(stream):
    return imap(lambda (x, y): y, iter_fasta_stream_items(stream))

def iter_fasta_file(filename):
    return iter_fasta_stream(open(filename))

def iter_fasta_file_items(filename):
    return iter_fasta_stream_items(open(filename))
