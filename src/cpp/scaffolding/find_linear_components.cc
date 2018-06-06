#include <iostream>
#include <fstream>
#include <string>
#include <vector>

#include "vertices_io.h"

using namespace std;

int main(int argc, char **argv)
{
    if (argc > 1)
    {
        cerr << "Usage " << argv[0] << " < graph_file\n";
        return 1;
    }
    vector <vector <size_t> > in, out;

    while (cin)
    {
        size_t v1 = read_vertex(cin);
        size_t v2 = read_vertex(cin);
        if (out.size() <= v1)
            out.resize(v1 + 1);
        out[v1].push_back(v2);

        if (in.size() <= v2)
            in.resize(v2 + 1);
        in[v2].push_back(v1);

        cin.ignore(255, '\n');
    }

    size_t n = max(in.size(), out.size());
    if (in.size() < n)
        in.resize(n);
    if (out.size() < n)
        out.resize(n);
    vector <size_t> component(n, 0);
    size_t cur = 1;
    for (size_t i = 0; i < n; ++i)
    {
        if ((i & ((1 << 18) - 1)) == 0)
            cerr << i << endl;
        if ((component[i] != 0) || (in[i].size() != 1) || (out[i].size() != 1))
            continue;
        int len = 1;
        for (size_t v = in[i][0]; (component[v] == 0) && (in[v].size() == 1) && (out[v].size() == 1); v = in[v][0])
        {
            ++len;
            component[v] = cur;
        }
        for (size_t v = out[i][0]; (component[v] == 0) && (in[v].size() == 1) && (out[v].size() == 1); v = out[v][0])
        {
            ++len;
            component[v] = cur;
        }
        if (len >= 1)
            cout << len << "\n";
    }
}

