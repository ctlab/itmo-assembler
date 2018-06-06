#define BOOST_TEST_MODULE tree_set test
#include <boost/test/unit_test.hpp>

#include <string>
#include <cstring>

#include "tree_set.hpp"

using std::vector;

template<typename T>
void test_adding_into_set(TreeSet<int>& set, vector<T> const & es) {
    size_t n = es.size();

    for (size_t i = 0; i <= n; ++i) {
        for (size_t j = 0; j < i; ++j) {
            BOOST_CHECK_EQUAL(set.contains(es[j]), true);
        }
        for (size_t j = i; j < n; ++j) {
            BOOST_CHECK_EQUAL(set.contains(es[j]), false);
        }

        if (i < n) {
            set.add(es[i]);
        }
    }

    for (size_t i = 0; i <= n; ++i) {
        for (size_t j = 0; j < n; ++j) {
            BOOST_CHECK_EQUAL(set.contains(es[j]), true);
        }

        if (i < n) {
            set.add(es[i]);
        }
    }
}

BOOST_AUTO_TEST_CASE( adding_test ) {
    vector<int> es;
    es.push_back(0);
    es.push_back(4);
    es.push_back(42);
    es.push_back(-1);

    TreeSet<int> set;

    test_adding_into_set(set, es);

    vector<int> es2;
    for (int i = 256; i < 512; ++i) {
        es2.push_back(i);
    }

    test_adding_into_set(set, es2);
}
