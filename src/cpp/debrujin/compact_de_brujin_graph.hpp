#pragma once

/*
 * Thist file contains class for compact representation of de Brujin graph
 */

#include <vector>
#include <algorithm>

#include "../dna/nucleotide.h"
#include "../containers/hash_set.hpp"

template<class KmerService>
class CompactDeBrujinGraph
{
public:
    typedef typename KmerService::Kmer Kmer;

    CompactDeBrujinGraph(int k, size_t capacity)
        : edge_kmer_service_(k + 1),
        vertex_kmer_service_(k),
        edges_(capacity, edge_kmer_service_.absent_kmer())
    {

    }

    bool add_edge(Kmer edge) 
    {
        return edges_.add(get_edge_key(edge));
    }

    bool contains_edge(Kmer edge) const 
    { 
        return edges_.contains(get_edge_key(edge));
    }

    std::vector<Kmer> outcome_edges(Kmer vertex) const 
    {
        std::vector<Kmer> res;
        res.reserve(NUCLEOTIDES_NUMBER);
        for (Nucleotide nuc = 0; nuc < NUCLEOTIDES_NUMBER; ++nuc)
        {
            Kmer e = vertex_kmer_service_.append(vertex, nuc);
            if (contains_edge(e))
                res.push_back(e);
        }
        return res;
    }

    std::vector<Kmer> income_edges(Kmer vertex) const
    {
        std::vector<Kmer> res;
        res.reserve(NUCLEOTIDES_NUMBER);
        for (Nucleotide nuc = 0; nuc < NUCLEOTIDES_NUMBER; ++nuc)
        {
            Kmer e = vertex_kmer_service_.prepend(vertex, nuc);
            if (contains_edge(e))
                res.push_back(e);
        }
        return res;
    }

private:

    KmerService edge_kmer_service_;
    KmerService vertex_kmer_service_;

    HashSet<Kmer> edges_;

    Kmer get_edge_key(Kmer edge) const
    {
        return std::min(edge, edge_kmer_service_.reverse_complement(edge));
    }

};

