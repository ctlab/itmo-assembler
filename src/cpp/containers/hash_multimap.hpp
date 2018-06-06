/*
 * This file contains open-addres fixed-size hash multi map class.
 */

#pragma once

#include <functional>
#include <algorithm>
#include <vector>

#include "../common.h"
#include "hash_set_base.hpp"
#include "hash_set.hpp"


template<
    typename K, typename V,
    typename Hash = size_t (*) (K),
    typename Pred = std::equal_to<K>
    >
class HashMultiMap: protected HashSetBase<K, Hash, Pred> 
{
    typedef HashSetBase<K, Hash, Pred> Base;
    typedef std::vector<V> Values;
public:
    HashMultiMap(
            size_t capacity,
            K const & absent_elemnt,
            Hash const & hash = default_hash<K>,
            Pred const & equals = Pred());

    bool put(K const & key, V const & value);
    std::vector<V> get_all(K const & key) const;

    size_t capacity() const { return Base::table_.size(); }
    size_t size() const { return size_; }

protected:
    size_t size_;
    size_t size_threshold_;

    Values vs_;

};

template<typename K, typename V, typename Hash, typename Pred>
HashMultiMap<K, V, Hash, Pred>::HashMultiMap(
        size_t capacity,
        K const & absent_elemnt,
        Hash const & hash,
        Pred const & equals)
    : Base(capacity, absent_elemnt, hash, equals),
      size_(0),
      size_threshold_(capacity - 1),
      vs_(Base::table_.size())
{
}

template<typename K, typename V, typename Hash, typename Pred>
bool HashMultiMap<K, V, Hash, Pred>::put(K const & k, V const & v) 
{
    assert(!equals_(k, Base::FREE));
    if (size_ >= size_threshold_) 
        throw HashSetFullExcpetion();

    typename Base::PositionsIterator pos_it = Base::get_positions(k);

    ++size_;

    for (; !pos_it.is_end(); ++pos_it) 
        ;

    Base::table_[*pos_it] = k;
    vs_[*pos_it] = v;

    return true;
}

template<typename K, typename V, typename Hash, typename Pred>
std::vector<V> HashMultiMap<K, V, Hash, Pred>::get_all(K const & k) const 
{
    std::vector<V> res;
    typename Base::PositionsIterator pos_it = Base::get_positions(k);
    
    for (; !pos_it.is_end(); ++pos_it) 
    {
        res.push_back(vs_[*pos_it]);
    }

    return res;
}

