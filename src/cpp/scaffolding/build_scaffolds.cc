#include <iostream>
#include <fstream>
#include <vector>
#include <algorithm>

#include <boost/shared_ptr.hpp>
#include <boost/foreach.hpp>

#include "contigs_graph.h"
#include "scaffold.h"

using namespace std;
using namespace boost;

#define foreach BOOST_FOREACH

typedef ContigsGraph::EdgeInfo EdgeInfo;

struct Edge
{
    size_t from;
    size_t to;
    EdgeInfo info;
    Edge() { }
    Edge(size_t from, size_t to, EdgeInfo const & info) 
        : from(from), to(to), info(info) { }
};

bool compare_by_weight(Edge const & e1, Edge const & e2)
{
    return e1.info.weight > e2.info.weight;
}

int main(int argc, char ** argv)
{
    if (argc != 2)
    {
        cerr << "usage: build_scaffolds <contigs_file> < graph_file > scaffolds_file\n";
        return 1;
    }

    ifstream contigs_stream(argv[1]);
    ContigsGraph graph(contigs_stream, cin);

    vector<Edge> edges;

    for (size_t i = 0, sz = graph.size(); i != sz; ++i) 
    {
        typedef pair<size_t, EdgeInfo> Entry;
        foreach(Entry const & e, graph.edges[i])
        {
            edges.push_back(Edge(i, e.first, e.second));
        }
    }

    sort(edges.begin(), edges.end(), compare_by_weight);



    return 0;
}
