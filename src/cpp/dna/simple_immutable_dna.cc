#include <iostream>
#include <iomanip>
#include <cstring>
#include <vector>
#include <cassert>
#include <string>
#include <boost/shared_array.hpp>

#include "simple_immutable_dna.h"

using namespace std;
using boost::shared_array;

SimpleImmutableDna::SimpleImmutableDna(string nucs)
    : reversed_(false),
      complement_mask_(0),
      size_(nucs.size()),
      offset_(0),
      nucs_(new Cell[size_ / NUCS_PER_CELL + 1])
{
    for (size_t i = 0; i < size_; ++i) 
    {
        size_t internal_shift = BITS_PER_NUCLEOTIDE * (i % NUCS_PER_CELL);
        if (internal_shift == 0) 
        {
            nucs_[i / NUCS_PER_CELL] = 0;
        }
        nucs_[i / NUCS_PER_CELL] |= (char2nucleotide(nucs[i]) << internal_shift);
    }
}

SimpleImmutableDna::SimpleImmutableDna(vector<Nucleotide> nucs)
    : reversed_(false),
      complement_mask_(0),
      size_(nucs.size()),
      offset_(0),
      nucs_(new Cell[size_ / NUCS_PER_CELL + 1])
{
    for (size_t i = 0; i < size_; ++i) 
    {
        size_t internal_shift = BITS_PER_NUCLEOTIDE * (i % NUCS_PER_CELL);
        if (internal_shift == 0) 
        {
            nucs_[i / NUCS_PER_CELL] = 0;
        }
        nucs_[i / NUCS_PER_CELL] |= (nucs[i] << internal_shift);
    }
}

SimpleImmutableDna& SimpleImmutableDna::substring(size_t begin, size_t end)
{
    assert(begin <= end);
    assert(end <= size_);

    offset_ += reversed_ ? (size_ - end) : begin;
    size_ = end - begin;
    return *this;
}

Nucleotide SimpleImmutableDna::nuc_at(size_t i) const
{
    size_t pos = get_position(i);
    return raw_nuc_at(pos) ^ complement_mask_;
}

size_t SimpleImmutableDna::get_position(size_t i) const 
{
    return offset_ + (reversed_ ? (size_ - i - 1) : i);
}

Nucleotide SimpleImmutableDna::raw_nuc_at(size_t i) const 
{
    size_t internal_shift = BITS_PER_NUCLEOTIDE * (i % NUCS_PER_CELL);
    return (nucs_[i / NUCS_PER_CELL] >> internal_shift) & NUCLEOTIDE_MASK;
}
