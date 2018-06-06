#define BOOST_TEST_MODULE contigs_graph_builder test
#include <boost/test/unit_test.hpp>

#include <string>
#include <cmath>

#include "../dna/simple_immutable_dna.h"
#include "contigs_graph_builder.h"

using namespace std;

const double precision = 1e-3;

BOOST_AUTO_TEST_CASE( build_test )
{
    /*
     *            1->         2->       <-3
     * ATGCGATCGAAGCACC TTA ATGGCTT AT CACGAA GCG
     *             0           2          5
     *             1           3          4
     * TACGCTAGCTTCGTGG AAT TACCGAA TA GTGCTT CGC
     *             <-11       12->
     *                         <-22     21->
     *           <-31        32->   
     *            <-42                   41->
     *  
     */
    
    SimpleImmutableDna contig1("ATGCGATCGAAGCACC");
    SimpleImmutableDna contig2("ATGGCTT");
    SimpleImmutableDna contig3("TTCGTG");

    SimpleImmutableDna mp11("GGTG");
    SimpleImmutableDna mp12("GGCT");
    size_t d1 = 12; // real 13

    SimpleImmutableDna mp21("ACGA");
    SimpleImmutableDna mp22("AAGC");
    size_t d2 = 12; // real 11

    SimpleImmutableDna mp31("TGCT");
    SimpleImmutableDna mp32("TGGC");
    size_t d3 = 12; // real 14

    SimpleImmutableDna mp41("CGAA");
    SimpleImmutableDna mp42("GTGC");
    size_t d4 = 23; // real 23

    ContigsGraphBuilder builder(4, 15, 6);
    try 
    {
        builder.add_contig(contig1);
        builder.add_contig(contig2);
        builder.add_contig(contig3);
    } 
    catch (HashSetFullExcpetion const & e)
    {
        BOOST_FAIL("index capacity too small");
    }

    builder.add_connection(mp11, mp12, d1);
    builder.add_connection(mp21, mp22, d2);
    builder.add_connection(mp31, mp32, d3);
    builder.add_connection(mp41, mp42, d4);

    ContigsGraph graph = builder.build();

    BOOST_CHECK_EQUAL(graph.vertices.size(), 3u * 2u);
    BOOST_CHECK_EQUAL(graph.edges.size(), 3u * 2u);

    BOOST_CHECK_CLOSE(graph.edges[0][2].mean, 1.5, precision);
    BOOST_CHECK_CLOSE(graph.edges[0][2].standard_deviation, sqrt(2)/2., precision);
    BOOST_CHECK_EQUAL(graph.edges[0][2].weight, 2);

    BOOST_CHECK_CLOSE(graph.edges[0][5].mean, 12., precision);
    BOOST_CHECK_CLOSE(graph.edges[0][5].standard_deviation, 0, precision);
    BOOST_CHECK_EQUAL(graph.edges[0][5].weight, 1);

    BOOST_CHECK_CLOSE(graph.edges[2][5].mean, 3., precision);
    BOOST_CHECK_CLOSE(graph.edges[2][5].standard_deviation, 0, precision);
    BOOST_CHECK_EQUAL(graph.edges[2][5].weight, 1);

    BOOST_CHECK_CLOSE(graph.edges[3][1].mean, 1.5, precision);
    BOOST_CHECK_CLOSE(graph.edges[3][1].standard_deviation, sqrt(2)/2., precision);
    BOOST_CHECK_EQUAL(graph.edges[3][1].weight, 2);

    BOOST_CHECK_CLOSE(graph.edges[4][1].mean, 12., precision);
    BOOST_CHECK_CLOSE(graph.edges[4][1].standard_deviation, 0, precision);
    BOOST_CHECK_EQUAL(graph.edges[4][1].weight, 1);

    BOOST_CHECK_CLOSE(graph.edges[4][3].mean, 3., precision);
    BOOST_CHECK_CLOSE(graph.edges[4][3].standard_deviation, 0, precision);
    BOOST_CHECK_EQUAL(graph.edges[4][3].weight, 1);

}
    

