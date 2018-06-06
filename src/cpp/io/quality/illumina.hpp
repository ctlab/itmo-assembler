#pragma once

#include <cmath>
#include <cassert>
#include <iostream>

#include "quality_format.h"

class Illumina: public QualityFormat
{
public:
    Illumina()
    {
        for (Phred phred = 0; phred <= MAX_PHRED; ++phred)
        {
            double res = (phred / -10.) / log(10);
            if (phred == 2)
            {
                res = log(0.75);
            }
            errors_lns_[phred] = res;
        }
    }

    Phred get_phred(char c) const
    {
        assert((c >= '@') && (c <= static_cast<int>('@' + MAX_PHRED)));
		return c - '@';
    }

    char get_char(Phred phred) const 
    {
        assert((phred >= 0) && (phred <= MAX_PHRED));
        return phred + '@';

    }

    double get_error_probability_ln(Phred phred) const
    {
        return errors_lns_[phred];
    }

private:
    static const Phred MAX_PHRED = 62;

    double errors_lns_[MAX_PHRED + 1];
};
