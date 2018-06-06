/*
 * Basic things about nucleotides. Implementation.
 */

#include "nucleotide.h"

#include <climits>
#include <cassert>

char nucleotide2char(Nucleotide n) {
    assert(n < NUCLEOTIDES_NUMBER);
    return NUCLEOTIDES[n];
}

bool is_nucleotide(char c) {
    static bool first_call = true;
    static bool is_nucleotide_cash[1 << CHAR_BIT] = {};
    if (first_call) {
        for (Nucleotide n = 0; n < NUCLEOTIDES_NUMBER; ++n) {
            is_nucleotide_cash[static_cast<unsigned char>(nucleotide2char(n))] = 
                true;
        }
        first_call = false;
    }
    return is_nucleotide_cash[static_cast<unsigned char>(c)];
}

Nucleotide char2nucleotide(char c) {
    static bool first_call = true;
    static Nucleotide char2nucleotide_cash[1 << CHAR_BIT];
    if (first_call) {
        for (Nucleotide n = 0; n < NUCLEOTIDES_NUMBER; ++n) {
            char2nucleotide_cash[static_cast<unsigned char>(nucleotide2char(n))] = n;
        }

    }
    assert(is_nucleotide(c));
    return char2nucleotide_cash[static_cast<unsigned char>(c)];
}


