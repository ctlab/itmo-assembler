/*
 * Common input/output-functions
 */

#pragma once

#include <iostream>
#include <algorithm>
#include <string>

#include "../dna/simple_immutable_dna.h"

template <class T>
std::istream & read_binary(std::istream & in, T & out, bool reverse_order = false)
{
    size_t s = sizeof(T);
    char *ptr = reinterpret_cast<char*>(&out);
    in.read(ptr, s);
    if (reverse_order)
    {
        std::reverse(ptr, ptr + s);
    }
    return in;
}

template <class T>
T read_binary(std::istream & in, bool reverse_order = false)
{
    T t;
    read_binary(in, t, reverse_order);
    return t;
}


inline SimpleImmutableDna get_dna_from_fastq_stream(std::istream & in)
{
    std::string s;
    getline(in, s); // header
    getline(in, s); // dna
    SimpleImmutableDna res(s);

    getline(in, s); // +
    getline(in, s); // quality
    return res;
}

