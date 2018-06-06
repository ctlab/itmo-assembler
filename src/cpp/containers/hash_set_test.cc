#define BOOST_TEST_MODULE hash_set test
#include <boost/test/unit_test.hpp>
#include <boost/interprocess/managed_shared_memory.hpp>
#include <boost/interprocess/allocators/allocator.hpp>
#include <boost/functional/hash.hpp>

#include <string>
#include <cstring>

#include <iostream>

#include "hash_set.hpp"

using namespace boost::interprocess;

template<typename Hash, typename Pred, typename Alloc>
void test_adding_into_hashset(HashSet<int, Hash, Pred, Alloc>& h, std::vector<int> const & es) {
    size_t n = es.size();

    for (size_t i = 0; i <= n; ++i) {
        BOOST_CHECK_EQUAL(h.size(), i);
        for (size_t j = 0; j < i; ++j) {
            BOOST_CHECK_EQUAL(h.contains(es[j]), true);
        }
        for (size_t j = i; j < n; ++j) {
            BOOST_CHECK_EQUAL(h.contains(es[j]), false);
        }

        if (i < n) {
            BOOST_CHECK_EQUAL(h.add(es[i]), true);
        }
    }

    for (size_t i = 0; i <= n; ++i) {
        for (size_t j = 0; j < n; ++j) {
            BOOST_CHECK_EQUAL(h.contains(es[j]), true);
        }

        if (i < n) {
            BOOST_CHECK_EQUAL(h.add(es[i]), false);
        }
    }

    BOOST_CHECK_THROW(overflow_hash_set(h), HashSetFullExcpetion);
}

template<typename Hash, typename Pred, typename Alloc>
void overflow_hash_set(HashSet<int, Hash, Pred, Alloc>& h) {

    int e = 0;
    size_t sz = h.capacity();
    for (size_t i = 0; i < sz; ++i) {
        h.add(e++);
    }
}

size_t zero_hash(int x) {
    return 0;
}

BOOST_AUTO_TEST_CASE( adding_test ) {
    std::vector<int> es;
    es.push_back(0);
    es.push_back(4);
    es.push_back(42);
    es.push_back(-1);

    HashSet<int> h(8, 932874231);
    BOOST_CHECK(h.capacity() >= 8);

    test_adding_into_hashset(h, es);

    HashSet<int, size_t (*) (int)> h2(8, 932874231, zero_hash);
    BOOST_CHECK(h2.capacity() >= 8);

    test_adding_into_hashset(h2, es);
}

BOOST_AUTO_TEST_CASE( sharing_test ) {
    std::vector<int> es;
    es.push_back(0);
    es.push_back(4);
    es.push_back(42);
    es.push_back(-1);

    const char * mem_name("MySharedMemory");

    try {
        shared_memory_object::remove(mem_name);
        typedef allocator<int, managed_shared_memory::segment_manager> ShmemAllocator;
        typedef HashSet<int, boost::hash<int>, std::equal_to<int>, ShmemAllocator>
            SharedHashSet; 

        {
            managed_shared_memory segment(create_only, mem_name, 65536);
            const ShmemAllocator alloc_instance(segment.get_segment_manager());

            SharedHashSet * h = segment.construct<SharedHashSet>("MyHashSet")
                (8, 932874231, boost::hash<int>(), std::equal_to<int>(), alloc_instance);

            test_adding_into_hashset(*h, es);
        }

        {
            managed_shared_memory segment2(open_only, mem_name);

            SharedHashSet const * h2 = segment2.find<SharedHashSet>("MyHashSet").first;

            for (size_t i = 0; i < es.size(); ++i) {
                BOOST_CHECK_EQUAL(h2->contains(es[i]), true);
            }

            BOOST_CHECK(!h2->contains(10));
            BOOST_CHECK(!h2->contains(-10));

            segment2.destroy<SharedHashSet>("MyHashSet");
        }


    } catch (...) {
        shared_memory_object::remove(mem_name);
        throw;
    }
    shared_memory_object::remove(mem_name);
}

