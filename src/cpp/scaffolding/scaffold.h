#pragma once

#include <deque>

#include <boost/shared_ptr.hpp>


// :TODO: on 64-bit sizeof(size_t) == 8 and sizeof(int) == 4
class Scaffold
{
    Scaffold(boost::shared_ptr<Dna const> contig)
        : begin_position_(0), end_position_(contig->size())
    {
        contigs_.push_back(contig);
        offsets_.push_back(0);
    }

    void append(boost::shared_ptr<Dna const> contig, size_t distance)
    {
        contigs_.push_back(contig);
        offsets_.push_back(end_position_ + distance);
        end_position_ += distance + contig->size();
    }

    void prepend(boost::shared_ptr<Dna const> contig, size_t distance)
    {
        contigs_.push_front(contig);
        begin_position_ -= distance + contig->size();
        offsets_.push_front(begin_position_);
    }


private:
    int begin_position_;
    int end_position_;

    std::deque<boost::shared_ptr<Dna const> > contigs_;
    std::deque<int> offsets_;
};

