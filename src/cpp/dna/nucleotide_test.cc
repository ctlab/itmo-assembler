#define BOOST_TEST_MODULE nucleotide test
#include <boost/test/unit_test.hpp>

#include "nucleotide.h"

BOOST_AUTO_TEST_CASE( conversions_test )
{
    for (Nucleotide n = 0; n < NUCLEOTIDES_NUMBER; ++n) {
        char c = nucleotide2char(n);
        Nucleotide n1 = char2nucleotide(c);
        char c1 = nucleotide2char(n1);

        BOOST_CHECK_EQUAL(n, n1);
        BOOST_CHECK_EQUAL(c, c1);

        BOOST_CHECK(is_nucleotide(c));
    }

    BOOST_CHECK(!is_nucleotide('a'));
    BOOST_CHECK(!is_nucleotide('g'));
    BOOST_CHECK(!is_nucleotide('c'));
    BOOST_CHECK(!is_nucleotide('t'));

    BOOST_CHECK(!is_nucleotide('Z'));
    BOOST_CHECK(!is_nucleotide('B'));
    BOOST_CHECK(!is_nucleotide('1'));
}
