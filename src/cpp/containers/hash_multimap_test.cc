#define BOOST_TEST_MODULE hash_multimap test
#include <boost/test/unit_test.hpp>
#include <boost/interprocess/managed_shared_memory.hpp>
#include <boost/interprocess/allocators/allocator.hpp>
#include <boost/functional/hash.hpp>

#include <string>
#include <cstring>

#include <iostream>

#include "hash_multimap.hpp"

using namespace boost::interprocess;

BOOST_AUTO_TEST_CASE( adding_test ) {
    HashMultiMap<int, int> h(8, 932874231);
    BOOST_CHECK(h.capacity() >= 8);

    BOOST_CHECK_EQUAL(h.put(10, 100), true);
    BOOST_CHECK_EQUAL(h.put(10, 101), true);
    BOOST_CHECK_EQUAL(h.put(0, 12), true);

    std::vector<int> v1, v2, v3;
    v1.push_back(100); v1.push_back(101);
    v2.push_back(12);

    BOOST_CHECK(h.get_all(10) == v1);
    BOOST_CHECK(h.get_all(0) == v2);
    BOOST_CHECK(h.get_all(3) == v3);
}

