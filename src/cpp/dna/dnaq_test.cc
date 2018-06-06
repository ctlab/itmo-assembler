#define BOOST_TEST_MODULE dnaq test
#include <boost/test/unit_test.hpp>

#include <cstring>

#include "dna.h"
#include "dnaq.h"
#include "nucleotide.h"
#include "phreded_nucleotide.h"

using namespace std;
using namespace boost;

BOOST_AUTO_TEST_CASE( construction_test ) {
    const size_t sz = 5;
    Nucleotide nucs[sz] = {0, 2, 3, 1, 0};
    Phred phreds[sz] = {10, 20, 0, 13, 42};
    Nucleotide nucs2[sz] = {3, 0, 2, 2, 1};
    Phred phreds2[sz] = {19, 1, 34, 10, 6};

    DnaQ dnaq1(nucs, phreds, sz);
    DnaQ dnaq2(nucs, phreds, sz);
    DnaQ dnaq3(nucs2, phreds, sz);
    DnaQ dnaq4(nucs, phreds2, sz);

    BOOST_CHECK_EQUAL(dnaq1.size(), sz);
    BOOST_CHECK_EQUAL(dnaq2.size(), sz);
    BOOST_CHECK_EQUAL(dnaq3.size(), sz);
    BOOST_CHECK_EQUAL(dnaq4.size(), sz);

    BOOST_CHECK_EQUAL(dnaq1, dnaq2);
    BOOST_CHECK(dnaq1 != dnaq3);
    BOOST_CHECK(dnaq1 != dnaq4);
    BOOST_CHECK(dnaq3 != dnaq4);
}


BOOST_AUTO_TEST_CASE( element_access_test ) {
    const size_t sz = 5;
    Nucleotide nucs[sz] = {0, 2, 3, 1, 0};
    Phred phreds[sz] = {10, 20, 0, 13, 42};
    Nucleotide nucs2[sz] = {3, 0, 2, 2, 1};
    Phred phreds2[sz] = {19, 1, 34, 10, 6};

    DnaQ dnaq1(nucs, phreds, sz);
    DnaQ dnaq2(nucs2, phreds2, sz);

    for (size_t i = 0; i < sz; ++i) {
        BOOST_CHECK_EQUAL(dnaq1.nuc_at(i), nucs[i]); 
        BOOST_CHECK_EQUAL(dnaq2.nuc_at(i), nucs2[i]); 
        BOOST_CHECK_EQUAL(dnaq1.phred_at(i), phreds[i]); 
        BOOST_CHECK_EQUAL(dnaq2.phred_at(i), phreds2[i]); 
    }

    for (size_t i = 0; i < sz; ++i) {
        dnaq1.set_nuc(i, nucs2[i]);
        dnaq1.set_phred(i, phreds2[i]);
    }

    BOOST_CHECK_EQUAL(dnaq1, dnaq2);
}

void check_reverse_complementness(
        DnaQ const & dnaq, DnaQ const & dnaq_r,
        DnaQ const & dnaq_c, DnaQ const & dnaq_rc) {
    BOOST_CHECK_EQUAL(*auto_ptr<DnaQ>(reverse(dnaq)), dnaq_r);
    BOOST_CHECK_EQUAL(*auto_ptr<DnaQ>(complement(dnaq)), dnaq_c);
    BOOST_CHECK_EQUAL(*auto_ptr<DnaQ>(reverse_complement(dnaq)), dnaq_rc);
}

BOOST_AUTO_TEST_CASE( reverese_complementing_test ) { 
    const size_t sz = 5;
    Nucleotide nucs[sz] = {0, 2, 3, 1, 0};
    Phred phreds[sz] = {10, 20, 0, 13, 42};

    Nucleotide nucs_r[sz] = {0, 1, 3, 2, 0};
    Phred phreds_r[sz] = {42, 13, 0, 20, 10};

    Nucleotide nucs_c[sz] = {3, 1, 0, 2, 3};
    Phred phreds_c[sz] = {10, 20, 0, 13, 42};

    Nucleotide nucs_rc[sz] = {3, 2, 0, 1, 3};
    Phred phreds_rc[sz] = {42, 13, 0, 20, 10};

    DnaQ dnaq(nucs, phreds, sz);
    DnaQ dnaq_r(nucs_r, phreds_r, sz);
    DnaQ dnaq_c(nucs_c, phreds_c, sz);
    DnaQ dnaq_rc(nucs_rc, phreds_rc, sz);
    
    check_reverse_complementness(dnaq, dnaq_r, dnaq_c, dnaq_rc);
    check_reverse_complementness(dnaq_r, dnaq, dnaq_rc, dnaq_c);
    check_reverse_complementness(dnaq_c, dnaq_rc, dnaq, dnaq_r);
    check_reverse_complementness(dnaq_rc, dnaq_c, dnaq_r, dnaq);

    dnaq.reverse();
    BOOST_CHECK_EQUAL(dnaq, dnaq_r);

    dnaq.complement();
    BOOST_CHECK_EQUAL(dnaq, dnaq_rc);

    dnaq.reverse();
    BOOST_CHECK_EQUAL(dnaq, dnaq_c);
}

BOOST_AUTO_TEST_CASE( substring_test ) { 
    const size_t sz = 5;
    Nucleotide nucs[sz] = {0, 2, 3, 1, 0};
    Phred phreds[sz] = {10, 20, 0, 13, 42};

    const size_t sz2 = 3;
    Nucleotide nucs2[sz2] = {0, 2, 3};
    Phred phreds2[sz2] = {10, 20, 0};

    const size_t sz3 = 4;
    Nucleotide nucs3[sz3] = {2, 3, 1, 0};
    Phred phreds3[sz3] = {20, 0, 13, 42};

    const size_t sz4 = 2;
    Nucleotide nucs4[sz4] = {2, 3};
    Phred phreds4[sz4] = {20, 0};

    Nucleotide nucs4_r[sz4] = {3, 2};
    Phred phreds4_r[sz4] = {0, 20};

    Nucleotide nucs4_c[sz4] = {1, 0};
    Phred phreds4_c[sz4] = {20, 0};

    Nucleotide nucs4_rc[sz4] = {0, 1};
    Phred phreds4_rc[sz4] = {0, 20};

    DnaQ dnaq(nucs, phreds, sz);
    DnaQ dnaq2(nucs2, phreds2, sz2);
    DnaQ dnaq3(nucs3, phreds3, sz3);
    DnaQ dnaq4(nucs4, phreds4, sz4);
    DnaQ dnaq4_r(nucs4_r, phreds4_r, sz4);
    DnaQ dnaq4_c(nucs4_c, phreds4_c, sz4);
    DnaQ dnaq4_rc(nucs4_rc, phreds4_rc, sz4);
    
    BOOST_CHECK_EQUAL(*auto_ptr<DnaQ>(substring(dnaq, 0, 3)), dnaq2);
    BOOST_CHECK_EQUAL(*auto_ptr<DnaQ>(substring(dnaq, 1, 5)), dnaq3);
    BOOST_CHECK_EQUAL(*auto_ptr<DnaQ>(substring(dnaq, 1, 3)), dnaq4);

    check_reverse_complementness(dnaq4, dnaq4_r, dnaq4_c, dnaq4_rc);

    dnaq.substring(1, 5);
    BOOST_CHECK_EQUAL(dnaq, dnaq3);

    dnaq.substring(0, 2);
    BOOST_CHECK_EQUAL(dnaq, dnaq4);
}


BOOST_AUTO_TEST_CASE( byte_array_test ) {
    const size_t sz = 5;
    Nucleotide nucs[sz] = {0, 2, 3, 1, 0};
    Phred phreds[sz] = {10, 20, 0, 13, 42};
    PhrededNucleotide pnucs[sz];

    for (size_t i = 0; i < sz; ++i) {
        pnucs[i] = PhrededNucleotide(nucs[i], phreds[i]);
    }

    DnaQ dnaq1(nucs, phreds, sz);
    DnaQ dnaq2(pnucs, pnucs + sz);

    BOOST_CHECK_EQUAL(dnaq1, dnaq2);

    vector<PhrededNucleotide> pnucs2(dnaq2.to_vector());

    BOOST_CHECK_EQUAL(pnucs2.size(), sz);
    for (size_t i = 0; i < sz; ++i) {
        BOOST_CHECK_EQUAL(pnucs[i], pnucs2[i]);
    }
}

BOOST_AUTO_TEST_CASE( appending_removing_test ) {
    const size_t sz = 5;
    Nucleotide nucs[sz] = {0, 2, 3, 1, 0};
    Phred phreds[sz] = {10, 20, 0, 13, 42};

    const size_t sz2 = 7;
    Nucleotide nucs2[sz2] = {2, 0, 1, 3, 3, 0, 0};
    Phred phreds2[sz2] = {39, 10, 19, 0, 14, 42, 51};

    DnaQ dnaq1(nucs, phreds, sz);

    DnaQ dnaq2(nucs + 2, phreds + 2, sz - 2);
    DnaQ dnaq3(nucs + 2, phreds + 2, sz - 4);

    DnaQ dnaq4(nucs2 + 1, phreds2 + 1, sz2 - 2);

    DnaQ dnaq5(nucs2, phreds2, sz2);

    DnaQ dnaq(nucs + 1, phreds + 1, sz - 2);

    dnaq.prepend(nucs[0], phreds[0]);
    dnaq.append(nucs[sz - 1], phreds[sz - 1]);

    BOOST_CHECK_EQUAL(dnaq, dnaq1);

    dnaq.remove_first(2);

    BOOST_CHECK_EQUAL(dnaq, dnaq2);

    dnaq.remove_last(2);

    BOOST_CHECK_EQUAL(dnaq, dnaq3);

    dnaq.append(nucs2[4], phreds2[4]);
    dnaq.append(nucs2[5], phreds2[5]);

    dnaq.prepend(nucs2[2], phreds2[2]);
    dnaq.prepend(nucs2[1], phreds2[1]);

    BOOST_CHECK_EQUAL(dnaq, dnaq4);

    dnaq.append(nucs2[6], phreds2[6]);
    dnaq.prepend(nucs2[0], phreds2[0]);

    BOOST_CHECK_EQUAL(dnaq, dnaq5);
}

