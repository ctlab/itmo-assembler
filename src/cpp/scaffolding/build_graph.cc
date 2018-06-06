#include <iostream>
#include <fstream>
#include <boost/lexical_cast.hpp>

//#include <boost/foreach.hpp>

#include "../dna/simple_immutable_dna.h"
#include "../dna/dna.h"
#include "../io/util.h"
#include "contigs_graph_builder.h"
#include "contigs_graph.h"

//#define foreach BOOST_FOREACH

extern size_t lefts_counter;
extern size_t rights_counter;
extern size_t pair_counter;
extern size_t large_counter;

using namespace std;
using namespace boost;

ContigsGraph build_graph(size_t k, size_t dist, size_t index_capacity, 
        char const * contigs_filename, char const * datalist_filename)
{
    ContigsGraphBuilder builder(k, index_capacity, static_cast<size_t>(dist * 1.1));

    {
        ifstream contigs_stream(contigs_filename);
        if (!contigs_stream)
        {
            cerr << "file " << contigs_filename << " not found\n";
            return ContigsGraph(); 
        }

        string s;
        getline(contigs_stream, s);
        size_t x = 0;
        while (contigs_stream)
        {
            string contig = "";
            while (1)
            {
                getline(contigs_stream, s);
                if (!contigs_stream || (s[0] == '>'))
                {
                    break;
                }

                contig += s;
            }

            // cerr << '"' << contig << "\"\n";
            builder.add_contig(SimpleImmutableDna(contig));
            ++x;
            if ((x & 0xffff) == 0)
            {
                cerr << x << " contigs, " << builder.index_size() << " kmers added \n";
            }
        }
    }

    cerr << "adding contigs done, indexed " << builder.index_size() << " kmers\n";

    {
        ifstream datalist_stream(datalist_filename);
        if (!datalist_stream)
        {
            cerr << "file " << datalist_filename << " not found\n";
            return ContigsGraph(); 
        }

        string lib_name;
        while (datalist_stream >> lib_name)
        {
            size_t x = 0;
			lefts_counter = 0;
			rights_counter = 0;
			pair_counter = 0;

            cerr << "loading from " << lib_name << "\n";
            ifstream left_stream((lib_name + "_1.fastq").c_str());
            ifstream right_stream((lib_name + "_2.fastq").c_str());
            if (!left_stream || !right_stream)
            {
                cerr << "lib " << lib_name << " not found\n";   
                return ContigsGraph(); 
            }
            while (1)
            {
                SimpleImmutableDna left = get_dna_from_fastq_stream(left_stream);
                SimpleImmutableDna right = get_dna_from_fastq_stream(right_stream);

                if (!left_stream || !right_stream)
                    break;

                // cerr << left << " " << right << " " << dist << "\n";
                builder.add_connection(left, right, dist);
                ++x;
                if (((x & 0xfffff) == 0) || ((x & (x - 1)) == 0))
                {
                    cerr << x << " pairs processed, "
						<< pair_counter << " not dropped, "
						<< large_counter << " large, average counters: " 
						<< lefts_counter / double(pair_counter) << ", "
					   	<< rights_counter / double(pair_counter) << "\n";
                }


            }
        }

    }

    return builder.build();
}

void print_contig_name(ostream & out, size_t contig)
{
    out << (contig / 2);
    if (contig & 1)
        out << "rc";
}

int main(int argc, char ** argv)
{
    if (argc < 5)
    {
        cerr << "usage: build_graph <k> <dist> <index-capacity> <contigs-file> <datalist-file>\n";
        return 1;
    }

    size_t k = lexical_cast<size_t>(argv[1]);
    size_t dist = lexical_cast<size_t>(argv[2]);
    size_t index_capacity = lexical_cast<size_t>(argv[3]);

    ContigsGraph graph = build_graph(k, dist, index_capacity, argv[4], argv[5]);

    cerr << "here we go!\n";

    ofstream out("graph.out");

    size_t sz = graph.edges.size();
    for (size_t i = 0; i < sz; ++i) 
    {
        typedef pair<size_t, ContigsGraph::EdgeInfo> Entry;
        // cerr << graph.edges[i].size() << "\n";
        //foreach(Entry e, graph.edges[i])
		for (ContigsGraph::Followers::const_iterator it = graph.edges[i].begin(),
				end = graph.edges[i].end(); it != end; ++it)
        {
			Entry const & e = *it;
            if (e.second.mean < 0)
                continue;
            print_contig_name(out, i);
            out << " ";
            print_contig_name(out, e.first);
            out << " " << static_cast<int>((graph.vertices[i]->size() + round(e.second.mean))) << " " 
                 << static_cast<int>(round(e.second.standard_deviation)) << " "
                 << e.second.weight << "\n";

        }
    }

    return 0;
}
