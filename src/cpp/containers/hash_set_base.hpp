/*
 * Internals of all hash containers.
 */

#pragma once

#include <functional>
#include <algorithm>
#include <vector>

#include "../common.h"

template<
    typename T,
    typename Hash,
    typename Pred,
    typename Alloc = std::allocator<T>
    >
class HashSetBase 
{
protected:
    struct PositionsIterator;
    typedef std::vector<T, Alloc> Table;
    
    HashSetBase(size_t capacity,
            T const & absent_element,
            Hash const & hash,
            Pred const & equals,
            Alloc const & alloc = Alloc());

    const T FREE;

    Hash const hash_;
    Pred const equals_;
    Alloc const alloc_;

    Table table_;


    PositionsIterator get_positions(T const & e) const;

    struct PositionsIterator 
    {
        PositionsIterator(T const & t, size_t start_pos, 
                          HashSetBase<T, Hash, Pred, Alloc>  const * base)
            : t_(t), pos_(start_pos), base_(base) 
        {
            if (!is_valid())
            {
                operator++();
            }
        }

        PositionsIterator& operator++() 
        {
            while (1) 
            {
                pos_++;
                pos_ %= base_->table_.size();
                if (is_valid())
                {
                    break;
                }
            } 
            return *this;
        }

        size_t operator*() const 
        {
            return pos_;
        }

        bool is_end() const
        {
            T const & t(base_->table_[pos_]);
            return base_->equals_(t, base_->FREE);
        }

    private:
        T const t_;
        size_t pos_;
        HashSetBase<T, Hash, Pred, Alloc> const * base_;

        bool is_valid() const {
            return is_end() || (base_->equals_(base_->table_[pos_], t_));
        }

    };
};

template<typename T, typename Hash, typename Pred, typename Alloc>
HashSetBase<T, Hash, Pred, Alloc>::HashSetBase(size_t capacity,
                                T const & absent_element,
                                Hash const & hash,
                                Pred const & equals, 
                                Alloc const & alloc)
    : FREE(absent_element),
      hash_(hash),
      equals_(equals),
      alloc_(alloc),
      table_(capacity, FREE, alloc_) 
{
}

template<typename T, typename Hash, typename Pred, typename Alloc>
typename HashSetBase<T, Hash, Pred, Alloc>::PositionsIterator
    HashSetBase<T, Hash, Pred, Alloc>::get_positions(T const & e) const 
{
    size_t cur_position = hash_(e) % table_.size();

    PositionsIterator begin(e, cur_position, this);
    return begin;
}
