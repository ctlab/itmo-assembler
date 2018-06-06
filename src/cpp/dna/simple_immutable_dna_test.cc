#define BOOST_TEST_MODULE simple_immutable_dna test
#include <boost/test/unit_test.hpp>

#include <cstring>
#include <vector>
#include <string>

#include "dna.h"
#include "simple_immutable_dna.h"
#include "nucleotide.h"
#include "phreded_nucleotide.h"

using namespace std;
using namespace boost;

BOOST_AUTO_TEST_CASE( construction_test ) 
{
    size_t sz = 5;
    vector<Nucleotide> nucs = {0, 2, 3, 1, 0};
    vector<Nucleotide> nucs2 = {3, 0, 2, 2, 1};
    string nucs2s = "TACCG";

    SimpleImmutableDna sidna1(nucs);
    SimpleImmutableDna sidna2(nucs);
    SimpleImmutableDna sidna3(nucs2);
    SimpleImmutableDna sidna4(nucs2s);

    BOOST_CHECK_EQUAL(sidna1.size(), sz);
    BOOST_CHECK_EQUAL(sidna2.size(), sz);
    BOOST_CHECK_EQUAL(sidna3.size(), sz);
    BOOST_CHECK_EQUAL(sidna4.size(), sz);

    BOOST_CHECK_EQUAL(sidna1, sidna2);
    BOOST_CHECK_EQUAL(sidna3, sidna4);
    BOOST_CHECK(sidna1 != sidna3);
}


BOOST_AUTO_TEST_CASE( element_access_test ) 
{
    const size_t sz = 5;
    vector<Nucleotide> nucs = {0, 2, 3, 1, 0};
    vector<Nucleotide> nucs2 = {3, 0, 2, 2, 1};

    SimpleImmutableDna sidna1(nucs);
    SimpleImmutableDna sidna2(nucs2);

    for (size_t i = 0; i < sz; ++i) 
    {
        BOOST_CHECK_EQUAL(sidna1.nuc_at(i), nucs[i]); 
        BOOST_CHECK_EQUAL(sidna2.nuc_at(i), nucs2[i]); 
    }
}

void check_reverse_complementness(
        SimpleImmutableDna const & sidna, SimpleImmutableDna const & sidna_r,
        SimpleImmutableDna const & sidna_c, SimpleImmutableDna const & sidna_rc) 
{
    BOOST_CHECK_EQUAL(*auto_ptr<SimpleImmutableDna>(reverse(sidna)), sidna_r);
    BOOST_CHECK_EQUAL(*auto_ptr<SimpleImmutableDna>(complement(sidna)), sidna_c);
    BOOST_CHECK_EQUAL(*auto_ptr<SimpleImmutableDna>(reverse_complement(sidna)), sidna_rc);
}

BOOST_AUTO_TEST_CASE( reverese_complementing_test ) 
{ 
    vector<Nucleotide> nucs = {0, 2, 3, 1, 0};

    vector<Nucleotide> nucs_r = {0, 1, 3, 2, 0};

    vector<Nucleotide> nucs_c = {3, 1, 0, 2, 3};

    vector<Nucleotide> nucs_rc = {3, 2, 0, 1, 3};

    SimpleImmutableDna sidna(nucs);
    SimpleImmutableDna sidna_r(nucs_r);
    SimpleImmutableDna sidna_c(nucs_c);
    SimpleImmutableDna sidna_rc(nucs_rc);
    
    check_reverse_complementness(sidna, sidna_r, sidna_c, sidna_rc);
    check_reverse_complementness(sidna_r, sidna, sidna_rc, sidna_c);
    check_reverse_complementness(sidna_c, sidna_rc, sidna, sidna_r);
    check_reverse_complementness(sidna_rc, sidna_c, sidna_r, sidna);

    sidna.reverse();
    BOOST_CHECK_EQUAL(sidna, sidna_r);

    sidna.complement();
    BOOST_CHECK_EQUAL(sidna, sidna_rc);

    sidna.reverse();
    BOOST_CHECK_EQUAL(sidna, sidna_c);
}

BOOST_AUTO_TEST_CASE( substring_test ) 
{ 
    vector<Nucleotide> nucs = {0, 2, 3, 1, 0};

    vector<Nucleotide> nucs2 = {0, 2, 3};

    vector<Nucleotide> nucs3 = {2, 3, 1, 0};

    vector<Nucleotide> nucs4 = {2, 3};

    vector<Nucleotide> nucs4_r = {3, 2};

    vector<Nucleotide> nucs4_c = {1, 0};

    vector<Nucleotide> nucs4_rc = {0, 1};

    SimpleImmutableDna sidna(nucs);
    SimpleImmutableDna sidna2(nucs2);
    SimpleImmutableDna sidna3(nucs3);
    SimpleImmutableDna sidna4(nucs4);
    SimpleImmutableDna sidna4_r(nucs4_r);
    SimpleImmutableDna sidna4_c(nucs4_c);
    SimpleImmutableDna sidna4_rc(nucs4_rc);
    
    BOOST_CHECK_EQUAL(*auto_ptr<SimpleImmutableDna>(substring(sidna, 0, 3)), sidna2);
    BOOST_CHECK_EQUAL(*auto_ptr<SimpleImmutableDna>(substring(sidna, 1, 5)), sidna3);
    BOOST_CHECK_EQUAL(*auto_ptr<SimpleImmutableDna>(substring(sidna, 1, 3)), sidna4);

    check_reverse_complementness(sidna4, sidna4_r, sidna4_c, sidna4_rc);

    sidna.substring(1, 5);
    BOOST_CHECK_EQUAL(sidna, sidna3);

    sidna.substring(0, 2);
    BOOST_CHECK_EQUAL(sidna, sidna4);
}

BOOST_AUTO_TEST_CASE( to_kmer_test) 
{
    BOOST_CHECK_EQUAL(dna_to_kmer<Kmer>(SimpleImmutableDna("TATGCA")), 0xcd8u);
}

