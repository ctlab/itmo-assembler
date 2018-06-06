#define BOOST_TEST_MODULE dnaq test
#include <boost/test/unit_test.hpp>

#include "fragments_assembling.hpp"

#include <string>
#include <vector>
#include <iostream>
#include <iomanip>

#include "../kmer/kmer32_service.h"


#include "../common.h"
#include "../dna/simple_immutable_dna.h"
#include "../dna/dna.h"
#include "../dna/dnaq.h"
#include "../debrujin/compact_de_brujin_graph.hpp"
#include "../io/quality/illumina.hpp"
#include "../util.h"


using namespace boost;
using namespace std;

Illumina const illumina;

typedef Kmer32Service::Kmer Kmer;

Kmer string_to_kmer(string const & s)
{
    return dna_to_kmer<Kmer>(SimpleImmutableDna(s));
}

template<class KmerService>
bool add_edge(CompactDeBrujinGraph<KmerService> & graph, string edge)
{
    return graph.add_edge(string_to_kmer(edge));
}

BOOST_AUTO_TEST_CASE( assemble_fragment_test ) 
{
    string fragment = "AGCGTAAGATATAGTTGATCCTACTTAAAA"; 
    string reads =    "AGCGTAAGACA.......CCCTGCTTAAAA"; 
    string misreads = ".........!........!...!.......";
    string quals =    "rrbaGSBIIBB.......BBBHYEabhrrr";
    
    DnaQ left(reads.substr(0, reads.find('.')),
              quals.substr(0, quals.find('.')),
              illumina);

    DnaQ right(reads.substr(reads.rfind('.') + 1),
               quals.substr(quals.rfind('.') + 1),
               illumina);

    right.reverse().complement();
                            
    FragmentAssembleParameters parameters(20, 20);

    const int k = 5;
    CompactDeBrujinGraph<Kmer32Service> graph(k, 256);

    size_t begin_pos = 1;
    size_t end_pos = fragment.length() - 2;

    for (size_t i = begin_pos; i + k + 1 <= end_pos; ++i)
    {
        BOOST_CHECK_EQUAL(add_edge(graph, fragment.substr(i, k + 1)), true);
    }

}
