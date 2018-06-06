#include <iostream>
#include <vector>
#include <cmath>
#include <boost/shared_ptr.hpp>

//#include <boost/foreach.hpp>

#include "../common.h"
#include "../dna/dna.h"
#include "../kmer/kmer32_service.h"
#include "contigs_graph.h"
#include "contigs_graph_builder.h"

//#define foreach BOOST_FOREACH

using boost::shared_ptr;
using std::vector;
using std::cerr;
using std::cout;

size_t lefts_counter, rights_counter;
size_t pair_counter, large_counter;

ContigsGraphBuilder::ContigsGraphBuilder(
        size_t k,
        size_t index_capacity,
        size_t max_distance,
        Kmer absent_kmer)
    : k_(k),
      max_distance_(max_distance),
      index_(index_capacity, absent_kmer),
      kmer_service_(k_)
{
}

void ContigsGraphBuilder::add_contig(Dna const & contig)
{
    accs_.resize(accs_.size() + 2);
    contigs_.push_back(shared_ptr<Dna>(contig.clone()));
    index_contig(contigs_.size() - 1);

    contigs_.push_back(shared_ptr<Dna>(reverse_complement(contig).release()));
}

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
void ContigsGraphBuilder::add_connection(
        Dna const & left, Dna const & right,
        size_t distance, bool mate_pair)
{
    if ((left.size() < k_) || (right.size() < k_))
        return;

    assert(mate_pair);

    assert(distance >= left.size() + right.size());

    size_t distance_between_beginings = distance - left.size() - right.size();


    Kmer left_kmer = dna_to_kmer<Kmer>(*substring(left, 0, k_));
    Kmer right_kmer = dna_to_kmer<Kmer>(*substring(right, 0, k_));

    vector<KmerPosition> left_positions = index_.get_all(left_kmer);
    vector<KmerPosition> right_positions = index_.get_all(right_kmer);

    vector<KmerPosition> left_rc_positions = 
        index_.get_all(kmer_service_.reverse_complement(left_kmer));
    vector<KmerPosition> right_rc_positions =
        index_.get_all(kmer_service_.reverse_complement(right_kmer));

    // cerr << "? " << left_kmer << " " << kmer_service_.reverse_complement(left_kmer) << "\n";
    // cerr << "? " << right_kmer << " " << kmer_service_.reverse_complement(right_kmer) << "\n";

    left_positions.reserve(left_positions.size() + left_rc_positions.size());
    right_positions.reserve(right_positions.size() + right_rc_positions.size());


    typedef vector<KmerPosition> KmerVector;
    for (KmerVector::const_iterator it = left_rc_positions.begin(),
           end = left_rc_positions.end(); it != end; ++it)
    {
        KmerPosition const & left_rc_pos = *it;
        size_t contig_size = contigs_[left_rc_pos.contig]->size();
        left_positions.push_back(KmerPosition(left_rc_pos.contig ^ 1, contig_size - left_rc_pos.offset - k_));
    }

    for (KmerVector::const_iterator it = right_rc_positions.begin(),
           end = right_rc_positions.end(); it != end; ++it)
    {
        KmerPosition const & right_rc_pos = *it;
        size_t contig_size = contigs_[right_rc_pos.contig]->size();
        right_positions.push_back(KmerPosition(right_rc_pos.contig ^ 1, contig_size - right_rc_pos.offset - k_));
    }

    // if left or right end has a period we're getting out of here
    {
        bool firstly = true;
        size_t prev_contig = 0;
        for (KmerVector::const_iterator it1 = left_positions.begin(),
               end1 = left_positions.end(); it1 != end1; ++it1)
        {
            KmerPosition const & left_pos = *it1;
            if (!firstly && (left_pos.contig == prev_contig))
            {
                return;
            }
            firstly = false;
            prev_contig = left_pos.contig;

        }
    }

    {
        bool firstly = true;
        size_t prev_contig = 0;
        for (KmerVector::const_iterator it2 = right_positions.begin(),
               end2 = right_positions.end(); it2 != end2; ++it2)
        {
            KmerPosition const & right_pos = *it2;
            if (!firstly && (right_pos.contig == prev_contig))
            {
                return;
            }
            firstly = false;
            prev_contig = right_pos.contig;

        }
    }

    if ((left_positions.size() > 1) || (right_positions.size() > 1))
    {
        ++large_counter;
        return;
    }

    lefts_counter += left_positions.size();
    rights_counter += right_positions.size();
    ++pair_counter;

    // foreach(KmerPosition const & left_pos, left_positions)
    for (KmerVector::const_iterator it1 = left_positions.begin(),
           end1 = left_positions.end(); it1 != end1; ++it1)
    {
        KmerPosition const & left_pos = *it1;

        assert(left_kmer == dna_to_kmer<Kmer>(
                    *substring(*contigs_[left_pos.contig], 
                        left_pos.offset, left_pos.offset + k_)));

        //foreach(KmerPosition const & right_pos, right_positions)
        for (KmerVector::const_iterator it2 = right_positions.begin(),
               end2 = right_positions.end(); it2 != end2; ++it2)
        {
            KmerPosition const & right_pos = *it2;
            assert(right_kmer == dna_to_kmer<Kmer>(
                        *substring(*contigs_[right_pos.contig], 
                            right_pos.offset, right_pos.offset + k_)));
            size_t left_contig = left_pos.contig;
            size_t right_contig = right_pos.contig;
            
            size_t left_contig_rc = left_contig ^ 1;
            size_t right_contig_rc = right_contig ^ 1;

            /*
             * right reversed complemented contig goes before left:
             *
             *   left contig
             *   ---------->    
             *                <-----------
             *                right contig
             *
             *       ---->         <---  
             *    left kmer      right kmer
             *
             */


            int inter_contig_distance = 
                distance_between_beginings - 
                (left_pos.offset + right_pos.offset);

            accs_[right_contig_rc][left_contig].add(inter_contig_distance);
            accs_[left_contig_rc][right_contig].add(inter_contig_distance);

            /*
            if (left_contig == right_contig)
            {
                cout << "(" << left_contig << ", " << left_pos.offset << "), ("
                            << right_contig << ", " << right_pos.offset << "): "
                            << inter_contig_distance << " " 
                            << left_positions.size() << " " << right_positions.size() << "\n";
            }
            */
        }
    }
}


ContigsGraph ContigsGraphBuilder::build() 
{
    ContigsGraph res;
    res.vertices = contigs_;
    res.edges.resize(contigs_.size());

    for (size_t i = 0; i < contigs_.size(); ++i) 
    {
        typedef std::pair<size_t, Accumulator> Entry;
        //foreach(Entry const entry, accs_[i])
        for (std::map<size_t, Accumulator>::const_iterator it = accs_[i].begin(),
                end = accs_[i].end(); it != end; ++it)
        {
            Entry const entry = *it;
            /*
            // :TODO: check using boost for this
            vector<int> const & distances(entry.second);
            size_t size = distances.size();

            double mean = 0;
            foreach(int d, distances)
            {
                mean += static_cast<double>(d) / size;
            }

            double squares_sum = 0;

            if (size > 1)
            {
                foreach(int d, distances)
                {
                    squares_sum += pow(d - mean, 2) / (size - 1);
                }
            }

            double standard_deviation = sqrt(squares_sum);

            res.edges[i][entry.first] = ContigsGraph::EdgeInfo(mean, standard_deviation);
            */
            Accumulator const & acc = entry.second;
            res.edges[i][entry.first] = ContigsGraph::EdgeInfo(
                    acc.mean(), acc.error(), acc.size());
        }

        accs_[i].clear();
    }

    return res;
}

void ContigsGraphBuilder::index_contig(size_t contig)
{
    size_t contig_size = contigs_[contig]->size();
    if (contig_size <= 2 * max_distance_) 
    {
        index_contig(contig, 0, contig_size);
        return;
    }

    index_contig(contig, 0, max_distance_);
    index_contig(contig, contig_size - max_distance_, contig_size);
}

void ContigsGraphBuilder::index_contig(size_t contig, size_t begin, size_t end)
{
    Dna const & dna(*contigs_[contig]);
    if (dna.size() < k_)
        return;

    Kmer cur_kmer = dna_to_kmer<Kmer>(*substring(dna, begin, begin + k_ - 1));
    Kmer kmer_mask = (static_cast<Kmer>(1) << (2 * k_)) - 1;

    for (size_t i = begin + k_ - 1; i < end; ++i) 
    {
        cur_kmer <<= BITS_PER_NUCLEOTIDE;
        cur_kmer += dna.nuc_at(i);
        cur_kmer &= kmer_mask;

        // cerr << "+ " << cur_kmer << " " << contig << " " << i - k_ + 1 << "\n";

        index_.put(cur_kmer, KmerPosition(contig, i - k_ + 1));
    }
}

