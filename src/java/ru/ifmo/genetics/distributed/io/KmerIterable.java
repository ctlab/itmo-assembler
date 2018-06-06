package ru.ifmo.genetics.distributed.io;

import ru.ifmo.genetics.distributed.clusterization.types.Kmer;

import java.util.Iterator;

/**
 * Author: Sergey Melnikov
 */
public interface KmerIterable {
    Iterator<Kmer> kmerIterator(int k) ;
}
