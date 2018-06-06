/*
 * Nucleotide with quality interface;
 */

#pragma once

#include <iostream>

#include "../common.h"
#include "nucleotide.h"

struct PhrededNucleotide {
    PhrededNucleotide() : value(0) { }
    PhrededNucleotide(Nucleotide nuc, Phred phred)
        : value((phred << BITS_PER_NUCLEOTIDE) + nuc) {
    }

    void set_nuc(Nucleotide nuc) {
        value = (value & CLEAR_NUC_MASK) + nuc;
    }

    void set_phred(Phred phred) {
        value = (value & NUCLEOTIDE_MASK) + 
                    (phred << BITS_PER_NUCLEOTIDE);
    }

    Nucleotide nuc() const {
        return value & NUCLEOTIDE_MASK;
    }

    Phred phred() const {
        return value >> BITS_PER_NUCLEOTIDE;
    }

    void complement() {
        value ^= NUCLEOTIDE_MASK;
    }

    bool operator==(PhrededNucleotide o) const {
        return value == o.value;
    }

    bool operator!=(PhrededNucleotide o) const {
        return value != o.value;
    }

    byte value;

private:
    static const byte CLEAR_NUC_MASK = static_cast<byte>(-1) ^ NUCLEOTIDE_MASK;
};

inline std::ostream& operator<<(std::ostream& out, PhrededNucleotide const & pn) {
    return out << "(" << pn.nuc() << ", " << pn.phred() << ")";
}
