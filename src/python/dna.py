from random import randrange

nucs = "ATGC"
all_nucs = "ATGCNatgcn"
complemented_nuc = { 'A' : 'T',
                     'T' : 'A',
                     'G' : 'C',
                     'C' : 'G',
                     'N' : 'N',
                     'a' : 't',
                     't' : 'a',
                     'g' : 'c',
                     'c' : 'g',
                     'n' : 'n'}

def complement_nuc(n):
    if n in complemented_nuc:
        return complemented_nuc[n]
    return n

def reverse_complemented(dna):
    return "".join(map(complement_nuc, reversed(dna)))

rc = reverse_complemented

def random_dna(length):
    return "".join([nucs[randrange(len(nucs))] for i in xrange(length)])
