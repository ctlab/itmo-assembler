#pragma once

#include <iostream>

#include <vector>
#include <algorithm>
#include <climits>
#include <boost/array.hpp>

#include "../common.h"

template<size_t L> struct InternalNode;

template<size_t L>
struct LeafNode {
    typedef boost::array<byte, L> Element;

    size_t size_of() const {
        return es.capacity() * sizeof(Element);
    }

    void add(Element const & e) {
        es.insert(std::upper_bound(es.begin(), es.end(), e), e);
    }

    bool contains(Element const & e) const {
        return std::find(es.begin(), es.end(), e) != es.end();
    }

    std::vector<Element> const & elements() const {
        return es;
    }

private:
    std::vector<Element> es;
};


template<size_t L>
struct Node {
    typedef boost::array<byte, L> Element;

    Node() : is_leaf(true) {
        raw_node.leaf = new LeafNode<L>();
    }

    ~Node() {
        if (is_leaf) {
            delete raw_node.leaf;
        } else {
            delete raw_node.internal;
        }
    }

    void add(Element const & e) {
        if (is_leaf) {
            raw_node.leaf->add(e);
            if (raw_node.leaf->size_of() > InternalNode<L>::size_of()) {
                split();
            }
        } else {
            raw_node.internal->add(e);
        }
    }

    bool contains(Element const & e) const {
        if (is_leaf) {
            return raw_node.leaf->contains(e);
        } else {
            return raw_node.internal->contains(e);
        }
    }

private:
    bool is_leaf;
    union {
        InternalNode<L> * internal;
        LeafNode<L> * leaf;
    } raw_node;

    void split() {
        InternalNode<L> * new_node = new InternalNode<L>();
        std::vector<Element> const & es(raw_node.leaf->elements());
        for (size_t i = 0; i < es.size(); ++i) {
            new_node->add(es[i]);
        }
        delete raw_node.leaf;
        raw_node.internal = new_node;
        is_leaf = false;
    }
};

template<>
struct Node<1> {
    typedef boost::array<byte, 1> Element;

    Node() : es(std::vector<bool>(1 << CHAR_BIT)) {
    }

    void add(Element const & e) {
        es[e.front()] = true;
    }

    bool contains(Element const & e) const {
        return es[e.front()];
    }

private:
    std::vector<bool> es; 

};

template<size_t L>
struct InternalNode {
    typedef boost::array<byte, L> Element;

    static size_t size_of() {
        return (1 << CHAR_BIT) * sizeof(Node<L - 1>);
    }

    void add(Element const & e) {
        boost::array<byte, L - 1> tail;
        std::copy(e.begin() + 1, e.end(), tail.begin());
        children[e.front()].add(tail);
    }

    bool contains(Element const & e) const {
        boost::array<byte, L - 1> tail;
        std::copy(e.begin() + 1, e.end(), tail.begin());
        return children[e.front()].contains(tail);
    }

private:
    boost::array<Node<L - 1>, 1 << CHAR_BIT> children;
};

