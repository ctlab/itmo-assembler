#pragma once

#include "../../dna/nucleotide.h"

struct QualityFormat
{
    virtual Phred get_phred(char c) const = 0;
    virtual char get_char(Phred phred) const = 0;
    virtual double get_error_probability_ln(Phred phred) const = 0;
    virtual ~QualityFormat() {}
};
