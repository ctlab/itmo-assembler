/*
 * Builder of contigs graph.
 */

#pragma once

#include <vector>
#include <limits>
#include <boost/shared_ptr.hpp>

#include "../common.h"
#include "../dna/dna.h"
#include "../containers/hash_multimap.hpp"
#include "../kmer/kmer32_service.h"
#include "../stat/accumulator.h"
#include "contigs_graph.h"

class ContigsGraphBuilder
{
public:
    ContigsGraphBuilder(
            size_t k,
            size_t index_capacity,
            size_t max_distance,
            Kmer absent_kmer = 0xde5665ae50d21a37ULL);

    void add_contig(Dna const & contig);

    /*
     * genome:                 ......................
     * mate_pair = true:
     *                            <--        -->
     *                            mp1        mp2
     *                            mp2        mp1
     *                            |<-distance->|
     *
     * mate_pair = false (not supported yet):
     *                            -->        <--
     *                            pe1        pe2
     *                            pe2        pe1
     *                            |<-distance->|
     */
    void add_connection(Dna const & left, Dna const & right,
            size_t distance, bool mate_pair = true);

    size_t index_size() const 
    {
        return index_.size();
    }

    ContigsGraph build();

private:
    struct KmerPosition
    {
        unsigned contig;
        unsigned offset; KmerPosition() { }
        KmerPosition(size_t contig, size_t offset) 
            : contig(contig), offset(offset) 
        {
        }
    };

    typedef HashMultiMap<Kmer, KmerPosition> Index;

    const size_t k_;
    const size_t max_distance_;
    Index index_;
    std::vector<boost::shared_ptr<Dna> > contigs_;
    std::vector<std::map<size_t, Accumulator> > accs_;
    const Kmer32Service kmer_service_;

    void index_contig(size_t contig);
    void index_contig(size_t contig, size_t begin, size_t end);
};

