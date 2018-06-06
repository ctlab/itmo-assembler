/*
 * DNA with quality class implementation.
 */

#include <cassert>
#include <cstring>
#include <vector>
#include <deque>
#include <functional>
#include <algorithm>

#include <boost/foreach.hpp>


#include "../common.h"
#include "dna.h"
#include "nucleotide.h"
#include "phreded_nucleotide.h"
#include "dnaq.h"

#define foreach BOOST_FOREACH

using namespace std;
using namespace boost;

DnaQ::DnaQ(Nucleotide * nucs, Phred * phreds, size_t size) 
    : array_(size) 
{
    for (size_t i = 0; i < size; ++i) 
    {
        array_[i] = PhrededNucleotide(nucs[i], phreds[i]);
    }
    
}

DnaQ::DnaQ(string const & nucs, string const & phreds, QualityFormat const & qf)
    : array_(nucs.size()) 
{
    assert(nucs.size() == phreds.size());
    for (size_t i = 0, size = nucs.size(); i < size; ++i) 
    {
        array_[i] = PhrededNucleotide(char2nucleotide(nucs[i]),
                                      qf.get_phred(phreds[i]));
    }
}

DnaQ::DnaQ(DnaQ const & other) 
    : array_(other.array_) 
{
}

DnaQ& DnaQ::reverse() 
{
    std::reverse(array_.begin(), array_.end());
    return *this;
}

DnaQ& DnaQ::complement() 
{
    foreach(PhrededNucleotide & pn, array_) 
    {
        pn.complement();
    }
    return *this;
}

DnaQ& DnaQ::substring(size_t begin, size_t end) 
{
    size_t size = array_.size();
    
    assert(begin < end);
    assert(end <= size);
    
    remove_first(begin);
    remove_last(size - end);

    return *this;
}

DnaQ * DnaQ::clone() const 
{
    return new DnaQ(*this);
}

Nucleotide DnaQ::nuc_at(size_t i) const 
{
    return array_[i].nuc();
}

void DnaQ::set_nuc(size_t i, Nucleotide n) 
{
    array_[i].set_nuc(n);
}

Phred DnaQ::phred_at(size_t i) const 
{
    return array_[i].phred();
}

void DnaQ::set_phred(size_t i, Phred phred) 
{
    array_[i].set_phred(phred);
}

void DnaQ::append(Nucleotide nuc, Phred phred) 
{
    array_.push_back(PhrededNucleotide(nuc, phred));
}

void DnaQ::prepend(Nucleotide nuc, Phred phred) 
{
    array_.push_front(PhrededNucleotide(nuc, phred));
}

void DnaQ::remove_last(size_t count) 
{
    assert(count <= array_.size());
    array_.erase(array_.end() - count, array_.end());
}

void DnaQ::remove_first(size_t count) 
{
    assert(count <= array_.size());
    array_.erase(array_.begin(), array_.begin() + count);
}

vector<PhrededNucleotide> DnaQ::to_vector() const 
{
    vector<PhrededNucleotide> res(array_.begin(), array_.end());    
    return res;
}

bool operator==(const DnaQ & a, const DnaQ & b) 
{
    size_t sz = a.size();

    if (sz != b.size()) 
    {
        return false;
    }

    for (size_t i = 0; i < sz; ++i) 
    {
        if ((a.nuc_at(i) != b.nuc_at(i)) ||
            (a.phred_at(i) != b.phred_at(i))) 
        {
            return false;
        }
    }
    return true;
}

bool operator!=(const DnaQ & a, const DnaQ & b) 
{
    return !(a == b);
}

