from itertools import *
import re

btop_re = re.compile("(\d+|[ATGCN-]{2})")
n_snp_re = re.compile("N[ATGCN]")

class blast_hit:
    fields = [ ("qseqid", str),
               ("sseqid", str),
               ("pident", float),
               ("qlen", int),
               ("length", int),
               ("positive", int),
               ("mismatch", int),
               ("gapopen", int),
               ("qstart", int),
               ("qend", int),
               ("sstart", int),
               ("send", int),
               ("bitscore", float),
               ("btop", str)]


    def __init__(self, s):
        s = s.strip()
        def _init_field((field, value)):
            (field_name, field_type) = field
            setattr(self, field_name, field_type(value))


        map(_init_field, izip(blast_hit.fields, s.split("\t")))
        self.btop_parsed = btop_re.findall(self.btop)
        self.positive += len(filter(n_snp_re.match, self.btop_parsed))
        self.negative = self.qlen - self.positive
        self.pmatch = self.positive/float(self.qlen)

    def __str__(self):
        return "\t".join([str(getattr(self, field_name)) for (field_name, _) in blast_hit.fields])

