#pragma once

#include <cmath>

class Accumulator
{
public:
    Accumulator() : n_(0), sum_(0), squares_sum_(0) { }

    void add(double x)
    {
        ++n_;
        sum_ += x;
        squares_sum_ += x * x;
    }

    size_t size() const 
    {
        return n_;
    }

    double mean() const
    {
        return sum_ / n_;
    }

    double error() const
    {
        if (n_ <= 1)
            return 0;
        double m = mean();
        return std::sqrt((squares_sum_ - 2 * m * sum_ + n_ * m * m) / (n_ - 1));
    }

private:
    size_t n_;
    double sum_;
    double squares_sum_;
};
