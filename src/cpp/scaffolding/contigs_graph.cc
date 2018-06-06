#include <iostream>
#include <boost/shared_ptr.hpp>

#include "../dna/dna.h"
#include "../dna/simple_immutable_dna.h"
#include "../io/util.h"
#include "vertices_io.h"
#include "contigs_graph.h"

using namespace std;

ContigsGraph::ContigsGraph(istream & contigs_stream, istream & edges_stream)
{
    while (1)
    {
        SimpleImmutableDna contig = get_dna_from_fastq_stream(contigs_stream);

        if (!contigs_stream)
            break;

        vertices.push_back(boost::shared_ptr<Dna>(contig.clone()));
        vertices.push_back(boost::shared_ptr<Dna>(reverse_complement(contig)));
    }

    edges.resize(vertices.size());

    while (1)
    {
        size_t from = read_vertex(edges_stream);
        size_t to = read_vertex(edges_stream);
        double mean, error;
        edges_stream >> mean >> error;
        int weight;
        edges_stream >> weight;

        if (!edges_stream)
            break;

        edges[from][to] = EdgeInfo(mean, error, weight);
    }

}
