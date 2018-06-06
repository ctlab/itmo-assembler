/*
 * DNA with quality class interface.
 */

#pragma once

#include <cstring>
#include <deque>
#include <vector>
#include <algorithm>
#include <string>

#include "../common.h"
#include "../io/quality/quality_format.h"
#include "dna.h"
#include "nucleotide.h"
#include "phreded_nucleotide.h"

struct DnaQ: public Dna {
    DnaQ(Nucleotide * nucs, Phred * phreds, size_t size);
    template<typename T>
        DnaQ(T begin, T end); // T - random-access iterator by phreded nucs
    DnaQ(DnaQ const &); 
    DnaQ(std::string const & nucs,
         std::string const & phreds,
         QualityFormat const & qf);

    size_t size() const { return array_.size(); }

    DnaQ& reverse();
    DnaQ& complement();

    DnaQ& substring(size_t begin, size_t end);

    DnaQ * clone() const;

    Nucleotide nuc_at(size_t i) const;
    void set_nuc(size_t i, Nucleotide n);

    Phred phred_at(size_t i) const;
    void set_phred(size_t i, Phred phred);

    void append(Nucleotide nuc, Phred phred);
    void prepend(Nucleotide nuc, Phred phred);

    void remove_last(size_t count = 1);
    void remove_first(size_t count = 1);

    std::vector<PhrededNucleotide> to_vector() const;

    ~DnaQ() {};

private:

    std::deque<PhrededNucleotide> array_;
};

bool operator==(const DnaQ &, const DnaQ &);
bool operator!=(const DnaQ &, const DnaQ &);

template<typename T>
DnaQ::DnaQ(T begin, T end)
    : array_(std::deque<PhrededNucleotide>(begin, end)) {
}
    
