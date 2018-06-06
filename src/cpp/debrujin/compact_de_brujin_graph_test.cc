#define BOOST_TEST_MODULE dnaq test
#include <boost/test/unit_test.hpp>

#include "compact_de_brujin_graph.hpp"

#include <string>
#include <vector>
#include <iostream>
#include <iomanip>

#include "../kmer/kmer32_service.h"


#include "../common.h"
#include "../dna/simple_immutable_dna.h"
#include "../dna/dna.h"
#include "../util.h"


using namespace boost;
using namespace std;

BOOST_AUTO_TEST_CASE( add_contains_test ) 
{
    const int k = 30;
    Kmer32Service kmer_service(k+1);
    typedef Kmer32Service::Kmer Kmer;

    string s = "ATGCTGATAGGGATCATCAGTCGGCATCTA";
    SimpleImmutableDna dna1(s + "C");

    Kmer e1 = dna_to_kmer<Kmer>(dna1);
    Kmer e1_rc = kmer_service.reverse_complement(e1);

    CompactDeBrujinGraph<Kmer32Service> graph(k, 16);

    BOOST_CHECK_EQUAL(graph.contains_edge(e1), false);
    BOOST_CHECK_EQUAL(graph.contains_edge(e1_rc), false);

    BOOST_CHECK_EQUAL(graph.add_edge(e1), true);

    BOOST_CHECK_EQUAL(graph.contains_edge(e1), true);
    BOOST_CHECK_EQUAL(graph.contains_edge(e1_rc), true);

    BOOST_CHECK_EQUAL(graph.add_edge(e1_rc), false);
    
    BOOST_CHECK_EQUAL(graph.contains_edge(e1), true);
    BOOST_CHECK_EQUAL(graph.contains_edge(e1_rc), true);


    SimpleImmutableDna dna2(s + "T");
    SimpleImmutableDna dna3("A" + s);
    SimpleImmutableDna dna4("C" + s);

    Kmer e2 = dna_to_kmer<Kmer>(dna2);
    Kmer e3 = dna_to_kmer<Kmer>(dna3);
    Kmer e4 = dna_to_kmer<Kmer>(dna4);

    Kmer v = dna_to_kmer<Kmer>(SimpleImmutableDna(s));

    graph.add_edge(e2);
    graph.add_edge(e3);
    graph.add_edge(e4);

    vector<Kmer> outcome = {e1, e2};
    vector<Kmer> income = {e3, e4};

    BOOST_CHECK(outcome == graph.outcome_edges(v));
    BOOST_CHECK(income == graph.income_edges(v));
}
