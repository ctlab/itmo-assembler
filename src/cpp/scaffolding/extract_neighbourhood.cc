#include <fstream>
#include <iostream>
#include <algorithm>
#include <vector>
#include <set>
#include <map>
#include <queue>

#include <boost/lexical_cast.hpp>

using namespace std;
using namespace boost;

typedef set<size_t> Vertices;
typedef map<size_t, Vertices> Edges;

Edges edges, redges;
Vertices vertices;

int main(int argc, char ** argv)
{
    if (argc != 4)
    {
        cerr << "usage: extract_neighbourhood <vertex> <radius> <max-degre> < in_graph > out_graph\n";
        return 1;
    }
    size_t v = lexical_cast<size_t>(argv[1]);
    size_t r = lexical_cast<size_t>(argv[2]);
    size_t max_degree = lexical_cast<size_t>(argv[3]);
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

    Vertices extracted_vertices;

    queue< pair<size_t, size_t> > q;

    q.push(make_pair(v, 0));

    while (!q.empty())
    {
        size_t u = q.front().first;
        size_t dist = q.front().second;
        q.pop();
        if (dist > r)
            continue;

        if ((edges[u].size() > max_degree) || (redges[u].size() > max_degree))
            continue;
        

        if (extracted_vertices.insert(u).second == false)
            continue;

        for (Vertices::const_iterator it = edges[u].begin(), end = edges[u].end();
                it != end; ++it)
        {
            q.push(make_pair(*it, dist + 1));
        }

        for (Vertices::const_iterator it = redges[u].begin(), end = redges[u].end();
                it != end; ++it)
        {
            q.push(make_pair(*it, dist + 1));
        }
    }

    cerr << extracted_vertices.size() << " vertices extracted\n";

    size_t extracted_edges_num = 0;
    for (Vertices::const_iterator it = extracted_vertices.begin(), end = extracted_vertices.end();
            it != end; ++it)
    {
        size_t u = *it;
        for (Vertices::const_iterator it2 = edges[u].begin(), end2 = edges[u].end();
                it2 != end2; ++it2)
        {
            if (extracted_vertices.count(*it2)) 
            {
                cout << u << " " << *it2 << "\n";
                ++extracted_edges_num;
            }
        }

    }

    cerr << extracted_edges_num << " edges extracted\n";
    
}
