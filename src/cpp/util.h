/*
 * Some useful functions.
 */

#pragma once

#include <vector>

template<typename T, typename Alloc>
std::ostream & operator<<(std::ostream & out, std::vector<T, Alloc> const & v)
{
    out << "[ ";
    for (size_t i = 0; i < v.size(); ++i) {
        if (i != 0) 
        {
            out << ", ";
        }
        out << v[i];
    }
    out << " ]";
    return out;
}


