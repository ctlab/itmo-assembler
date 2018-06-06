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
map<size_t, bool> marks;
map<size_t, int> component;

vector<size_t> queue;
vector<set<int> > cedges;

void dfs(size_t u)
{
    if (marks[u])
        return;
    marks[u] = true;

    for (Vertices::const_iterator it = edges[u].begin(), end = edges[u].end();
            it != end; ++it)
    {
        dfs(*it);
    }

    queue.push_back(u);
}


size_t dfs2(size_t u, int c)
{
    if (component[u] != -1)
        return 0;
    size_t res = 1;
    component[u] = c;

    for (Vertices::const_iterator it = redges[u].begin(), end = redges[u].end();
            it != end; ++it)
    {
        res += dfs2(*it, c);
    }
    return res;
}


int main(int argc, char ** argv)
{
    if (argc > 1)
    {
        cerr << "usage: dense_graph < in_graph > out_graph\n";
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

    queue.reserve(vertices.size());
    for (Vertices::const_iterator it = vertices.begin(), end = vertices.end(); it != end; ++it)
    {
        size_t i = *it;;
        dfs(i);
        component[i] = -1;
    }

    cerr << "dfsed\n";

    reverse(queue.begin(), queue.end());
    int cur_comp = 0;
    for (size_t i = 0, sz = queue.size(); i < sz; ++i)
    {
        size_t u = queue[i];
        if (component[u] != -1)
            continue;

        dfs2(u, cur_comp++);
    }

    cerr << cur_comp  << " components\n";
    vector<vector<size_t> > components(cur_comp);
    for (Vertices::const_iterator it = vertices.begin(), end = vertices.end(); it != end; ++it)
    {
        size_t i = *it;;
        components[component[i]].push_back(i);
    }
    cedges.resize(cur_comp);

    for (Edges::const_iterator it1 = edges.begin(), end1 = edges.end(); it1 != end1; ++it1)
    {
        size_t i = it1->first;
        if (component[i] == -1)
            continue;
        for (Vertices::const_iterator it = it1->second.begin(), end = it1->second.end();
                it != end; ++it)
        {
            cedges[component[i]].insert(component[*it]);
        }
    }

    // printing result
    cout << cur_comp << "\n";

    int non_trivial = 0;
    for (int i = 0; i < cur_comp; ++i)
    {
        size_t sz = components[i].size();
        if (sz != 1)
        {
            ++non_trivial;
        }

        cout << sz << " :";
        for (size_t j = 0; j < sz; ++j)
        {
            cout << " " << components[i][j];
        }
        cout << "\n";
    }

    cerr << non_trivial << " non-trivial\n";
    for (int i = 0; i < cur_comp; ++i)
    {
        for (set<int>::const_iterator it = cedges[i].begin(), end = cedges[i].end();
                it != end; ++it)
        {
            cout << i << " " << *it << "\n";
        }
    }
}
