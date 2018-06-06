#include <fstream>
#include <iostream>
#include <algorithm>
#include <vector>
#include <set>
#include <map>

#include <boost/lexical_cast.hpp>

using namespace std;
using namespace boost;

typedef set<size_t> Vertices;
typedef map<size_t, Vertices> Edges;

Edges edges, redges;
Vertices vertices;

int main(int argc, char ** argv)
{
    if (argc > 1)
    {
        cerr << "usage: dense_edges < in_graph > out_graph\n";
        return 1;
    }
    size_t edges_num = 0;
    while (1)
    {
        size_t from, to;
        if (!(cin >> from >> to))
            break;

        ++edges_num;
        
        edges[from].insert(to);
        redges[to].insert(from);
        vertices.insert(from);
        vertices.insert(to);
    }

    cerr << edges_num << " edges read\n";

    for (Vertices::const_iterator it = vertices.begin(), end = vertices.end(); it != end; ++it)
    {
        size_t i = *it;;
        if ((edges[i].size() == 1) && (redges[i].size() == 1))
        {
            size_t prev = *redges[i].begin();
            size_t next = *edges[i].begin();

            edges[i].clear();
            redges[i].clear();

            edges[prev].erase(i);
            redges[next].erase(i);

            edges[prev].insert(next);
            redges[next].insert(prev);
        }
    }

    for (Vertices::const_iterator it = vertices.begin(), end = vertices.end(); it != end; ++it)
    {
        size_t i = *it;
         for (Vertices::const_iterator it2 = edges[i].begin(), end2 = edges[i].end();
                it2 != end2; ++it2)
        {
            cout << i << " " << *it2 << "\n";
        }
   }
}
