/*
 * Basic DNA interface
 */

#include <cstring>
#include <memory>
#include <iostream>

#include "dna.h"

using namespace std;

bool operator==(const Dna & a, const Dna & b) {
    size_t sz = a.size();

    if (sz != b.size()) {
        return false;
    }

    for (size_t i = 0; i < sz; ++i) {
        if (a.nuc_at(i) != b.nuc_at(i)) {
            return false;
        }
    }
    return true;
}

bool operator!=(const Dna & a, const Dna & b) {
    return !(a == b);
}

ostream & operator<<(ostream & out, Dna const & dna) {
    for (size_t i = 0; i < dna.size(); ++i) {
        out << nucleotide2char(dna.nuc_at(i));
    }
    return out;
}
