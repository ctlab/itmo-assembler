#pragma once

#include <climits>
#include <cstdint>
#include <cassert>

#include "../common.h"
#include "../dna/nucleotide.h"

class Kmer32Service
{
public:
    typedef uint64_t Kmer;

    Kmer32Service(int k)
        : k2_(k * BITS_PER_NUCLEOTIDE),
        unused_bits_(sizeof(Kmer) * CHAR_BIT - k2_)

    {
        assert(k > 0);
        assert(k < 32);
    }

    Kmer reverse_complement(Kmer kmer) const
    {
        kmer = ((kmer & 0x3333333333333333ULL) << 2) | 
               ((kmer & 0xccccccccccccccccULL) >> 2);

        kmer = ((kmer & 0x0f0f0f0f0f0f0f0fULL) << 4) | 
               ((kmer & 0xf0f0f0f0f0f0f0f0ULL) >> 4);

        kmer = ((kmer & 0x00ff00ff00ff00ffULL) << 8) |
               ((kmer & 0xff00ff00ff00ff00ULL) >> 8);

        kmer = ((kmer & 0x0000ffff0000ffffULL) << 16) |
               ((kmer & 0xffff0000ffff0000ULL) >> 16);

        kmer = ((kmer & 0x00000000ffffffffULL) << 32) |
               ((kmer & 0xffffffff00000000ULL) >> 32);

        kmer = kmer ^ 0xffffffffffffffffULL;

        return kmer >> unused_bits_;
    }


    Kmer append(Kmer kmer, Nucleotide nuc) const
    {
        kmer <<= BITS_PER_NUCLEOTIDE;
        kmer += nuc;
        return kmer;
    }

    Kmer prepend(Kmer kmer, Nucleotide nuc) const
    {
        kmer += (static_cast<Kmer>(nuc) << k2_);
        return kmer;
    }

    Kmer absent_kmer() const
    {
        return static_cast<Kmer>(-1);
    }

private:
    const int k2_;
    const int unused_bits_;
};
