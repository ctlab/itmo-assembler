#pragma once

#include <boost/array.hpp>
#include <algorithm>

#include "../common.h"

#include "node.hpp"


template<typename T> 
struct TreeSet {
    void add(T const & e) {
        root.add(pod2byte_array(e));
    }

    bool contains(T const & e) const {
        return root.contains(pod2byte_array(e));
    }

private:
    Node<sizeof(T)> root;
};

template<typename T>
boost::array<byte, sizeof(T)> pod2byte_array(T const & t) {
    byte const * bytes = reinterpret_cast<byte const *>(&t);
    boost::array<byte, sizeof(T)> res;
    std::copy(bytes, bytes + sizeof(T), res.begin());
    return res;
}

