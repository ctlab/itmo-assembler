/*
 * Immutable simple DNA implementation.
 */

#pragma once

#include <cstring>
#include <climits>
#include <string>
#include <memory>
#include <vector>
#include <boost/shared_array.hpp>


#include "dna.h"
#include "nucleotide.h"

struct SimpleImmutableDna: public Dna 
{
    SimpleImmutableDna(std::string nucs);
    SimpleImmutableDna(std::vector<Nucleotide> nucs);

    size_t size() const { return size_; };
    Nucleotide nuc_at(size_t i) const;

    SimpleImmutableDna& reverse() 
    { 
        reversed_ = !reversed_;
        return *this;
    }

    SimpleImmutableDna& complement() 
    {
        complement_mask_ ^= NUCLEOTIDE_MASK;
        return *this;
    }

    SimpleImmutableDna& substring(size_t begin, size_t end);

    SimpleImmutableDna * clone() const 
    {
        return new SimpleImmutableDna(*this);
    }

private:
    typedef unsigned Cell;
    static size_t const NUCS_PER_CELL = 
        CHAR_BIT * sizeof(Cell) / BITS_PER_NUCLEOTIDE;

    bool reversed_;
    Nucleotide complement_mask_;
    size_t size_;
    size_t offset_;
    boost::shared_array<Cell> nucs_;

    size_t get_position(size_t i) const;
    Nucleotide raw_nuc_at(size_t i) const;
};
