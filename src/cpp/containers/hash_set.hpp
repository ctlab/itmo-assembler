/*
 * This file contains open-addres fixed-size hash set class interface.
 */

#pragma once

#include <functional>
#include <algorithm>
#include <vector>


#include "../common.h"
#include "hash_set_base.hpp"


template<typename T>
size_t default_hash(T x) 
{
    size_t res = 0;
    size_t const P = 0x987123adu;

    for (size_t i = 0; i < sizeof(T); i += 4) {
        size_t t = x & 0xffffffffu;
        res += t;
        res *= P;
        (x >>= 16) >>= 16;
    }
    return res;
}

template<
    typename T,
    typename Hash = size_t (*) (T),
    typename Pred = std::equal_to<T>,
    typename Alloc = std::allocator<T>
    >
class HashSet: protected HashSetBase<T, Hash, Pred, Alloc> 
{
    typedef HashSetBase<T, Hash, Pred, Alloc> Base;
public:
    HashSet(size_t capacity,
            T const & absent_elemnt,
            Hash const & hash = default_hash<T>,
            Pred const & equals = Pred(),
            Alloc const & alloc = Alloc());

    bool add(T const & e);
    bool contains(T const & e) const;

    size_t capacity() const { return Base::table_.size(); }
    size_t size() const { return size_; }

protected:
    size_t size_;
    size_t size_threshold_;

};

struct HashSetFullExcpetion 
{
};


template<typename T, typename Hash, typename Pred, typename Alloc>
HashSet<T, Hash, Pred, Alloc>::HashSet(size_t capacity,
                                T const & absent_elemnt,
                                Hash const & hash,
                                Pred const & equals, 
                                Alloc const & alloc)
    : Base(capacity, absent_elemnt, hash, equals, alloc),
      size_(0),
      size_threshold_(capacity - 1)
{
}

template<typename T, typename Hash, typename Pred, typename Alloc>
bool HashSet<T, Hash, Pred, Alloc>::add(T const & e) 
{
    assert(!equals_(e, Base::FREE));
    if (size_ >= size_threshold_) 
        throw HashSetFullExcpetion();

    typename Base::PositionsIterator pos_it = Base::get_positions(e);
    bool res = pos_it.is_end();

    Base::table_[*pos_it] = e;

    if (res) 
        ++size_;

    return res;
}

template<typename T, typename Hash, typename Pred, typename Alloc>
bool HashSet<T, Hash, Pred, Alloc>::contains(T const & e) const 
{
    return !Base::get_positions(e).is_end();
}

