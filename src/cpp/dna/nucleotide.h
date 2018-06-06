/*
 * Basic things about nucleotides.
 */

#pragma once

typedef unsigned Nucleotide;

typedef unsigned Phred;

const unsigned NUCLEOTIDES_NUMBER = 4;

const char NUCLEOTIDES[NUCLEOTIDES_NUMBER] = {'A', 'G', 'C', 'T'};

const unsigned BITS_PER_NUCLEOTIDE = 2;

const unsigned NUCLEOTIDE_MASK = 3;

bool is_nucleotide(char c);

char nucleotide2char(Nucleotide n);

Nucleotide char2nucleotide(char c);

