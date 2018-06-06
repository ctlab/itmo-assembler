#define BOOST_TEST_MODULE dnaq test
#include <boost/test/unit_test.hpp>

#include "../common.h"
#include "../dna/simple_immutable_dna.h"
#include "../dna/dna.h"
#include "kmer32_service.h"


using namespace boost;

BOOST_AUTO_TEST_CASE( rc_test ) {
    Kmer32Service kmer_service(31);
    typedef Kmer32Service::Kmer Kmer;

    SimpleImmutableDna dna("ATGCTGATAGGGATCATCAGTCGGCATCTAC");
    SimpleImmutableDna dna_rc("GTAGATGCCGACTGATGATCCCTATCAGCAT");

    Kmer kmer = dna_to_kmer<Kmer>(dna);
    Kmer kmer_rc = dna_to_kmer<Kmer>(dna_rc);
    
    BOOST_CHECK_EQUAL(kmer_service.reverse_complement(kmer), kmer_rc);
}
