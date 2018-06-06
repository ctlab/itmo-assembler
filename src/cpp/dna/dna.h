/*
 * Basic DNA interface
 */

#pragma once

#include <cstring>
#include <memory>
#include <iosfwd>


#include "nucleotide.h"

struct Dna 
{
    virtual size_t size() const = 0;
    virtual Nucleotide nuc_at(size_t i) const = 0;

    virtual Dna& reverse() = 0;
    virtual Dna& complement() = 0;

    virtual Dna& substring(size_t begin, size_t end) = 0;

    virtual Dna * clone() const = 0;
    
    virtual ~Dna() {};

};

bool operator==(const Dna &, const Dna &);
bool operator!=(const Dna &, const Dna &);

std::ostream & operator<<(std::ostream &, Dna const &);


template<typename T>
std::auto_ptr<T> reverse(T const & dna) 
{
    std::auto_ptr<T> ptr(dna.clone());
    ptr->reverse();
    return ptr;
}

template<typename T>
std::auto_ptr<T> complement(T const & dna) 
{
    std::auto_ptr<T> ptr(dna.clone());
    ptr->complement();
    return ptr;
}

template<typename T>
std::auto_ptr<T> reverse_complement(T const & dna) 
{
    std::auto_ptr<T> ptr(dna.clone());
    ptr->reverse().complement();
    return ptr;
}

template<typename T>
std::auto_ptr<T> substring(T const & dna, size_t begin, size_t end) {
    std::auto_ptr<T> ptr(dna.clone());
    ptr->substring(begin, end);
    return ptr;
}

template<typename K>
K dna_to_kmer(Dna const & dna) 
{
    K res = 0;
    for (size_t i = 0; i < dna.size(); ++i)
    {
        res <<= BITS_PER_NUCLEOTIDE;
        res += dna.nuc_at(i);
    }
    return res;
}

