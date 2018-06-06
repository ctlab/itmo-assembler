#pragma once

#include <iostream>
#include <string>

inline size_t read_vertex(std::istream & in)
{
    size_t res;
    in >> res;
    res <<= 1;
    if (in.peek() == 'r')
    {
        std::string rc;
        in >> rc;
        ++res;
    }
    return res;
}

inline void print_vertex(std::ostream & out, size_t vertex)
{
    out << (vertex / 2);
    if (vertex & 1)
        out << "rc";
}

