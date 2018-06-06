/*
 * Graph, where vertices are contigs, and oriented edge describe
 * what follows what in genome and what distance is between them.
 */

#pragma once

#include <map>
#include <vector>
#include <boost/shared_ptr.hpp>
#include <iosfwd>

#include "../common.h"
#include "../dna/dna.h"

class ContigsGraph
{
public:
    ContigsGraph() { }
    ContigsGraph(std::istream & contigs_stream, std::istream & edges_stream);
    struct EdgeInfo
    {
        double mean;
        double standard_deviation;
        int weight;
        EdgeInfo() { }
        EdgeInfo(double mean, double standard_deviation, int weight)
            : mean(mean), standard_deviation(standard_deviation), weight(weight)
        {
        }
    };

    typedef std::map<size_t, EdgeInfo> Followers;

    size_t size() const { return vertices.size(); }

    std::vector<Followers> edges;
    std::vector<boost::shared_ptr<Dna> > vertices;
};
