#pragma once

#include <utility>

#include "../dna/dnaq.h"
#include "../debrujin/compact_de_brujin_graph.hpp"

enum AssembleStatus {OK, FAIL};

struct FragmentAssembleParameters
{
    size_t min_len;
    size_t max_len;
    FragmentAssembleParameters(size_t min_len, size_t max_len)
        : min_len(min_len), max_len(max_len) 
    {}
};

template<class KmerService>
std::pair<AssembleStatus, DnaQ *> assemble_fragment(
        FragmentAssembleParameters const & parameters,
        CompactDeBrujinGraph<KmerService> const & graph,
        DnaQ const & left, DnaQ const & right)
{
    return std::make_pair(OK, NULL);
}
