/*
 * Describes common constants, types, etc.
 */

#pragma once

typedef unsigned char byte;

#ifndef __x86_64__
typedef unsigned long long uint64;
#else
typedef __uint64_t uint64;
#endif

// :TODO: make a template parameter
typedef uint64 Kmer;
