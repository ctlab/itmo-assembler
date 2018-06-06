from itertools import *
import re


class bowtie_hit:
    mismatch_re = re.compile("(\d+):([ATGC])>([ATGC])")
    class mismatch:
        def __init__(self, m):
            self.offset = int(m.group(1))
            self.ref_nuc = m.group(2)
            self.read_nuc = m.group(3)

        def __str__(self):
            return "%d:%s->%s" % (self.offset, self.ref_nuc, self.read_nuc)

    fields = [ ("name", str),
               ("strand", str),
               ("refid", str),
               ("offset", int),
               ("oriented_read", str),
               ("oriented_qual", str),
               ("magic", int),
               ("mismatches", str)]


    def __init__(self, s):
        if s.endswith("\n"):
            s = s[:-1]
        def _init_field((field, value)):
            (field_name, field_type) = field
            setattr(self, field_name, field_type(value))


        map(_init_field, izip(bowtie_hit.fields, s.split("\t")))

        if self.strand == "-":
            orientation_func = lambda s: "".join(reversed(s))
        else:
            orientation_func = lambda s: s

        self.original_read = orientation_func(self.oriented_read)
        self.original_qual = orientation_func(self.oriented_qual)

        self.qlen = len(self.oriented_read)

        self.parsed_mismatches = dict([(int(m.group(1)), (m.group(2), m.group(3))) 
            for m in bowtie_hit.mismatch_re.finditer(self.mismatches)])


    def __str__(self):
        return "\t".join([str(getattr(self, field_name)) for (field_name, _) in bowtie_hit.fields])

