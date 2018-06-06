#include <iostream>

#include <boost/lexical_cast.hpp>

#include "vertices_io.h"

using namespace std;
using namespace boost;

int main(int argc, char ** argv)
{
    if (argc < 4)
    {
        cerr << "usage select_edges <error-low> <error-hi> <weight-low>\n";
        return 1;
    }
    double err_lo = lexical_cast<double>(argv[1]);
    double err_hi = lexical_cast<double>(argv[2]);
    size_t weight_lo = lexical_cast<size_t>(argv[3]);

    size_t edges_num = 0;
    while (1)
    {
        size_t from = read_vertex_vertex(cin); size_t to = read_vertex(cin);
        double distance, error;
        cin >> distance >> error;
        size_t weight;
        cin >> weight;
        if (!cin)
            break;

        if ((error < err_lo) || (error > err_hi) || (weight < weight_lo))
        {
            continue;
        }

        ++edges_num;
        print_vertex(cout, from);
        cout << " ";
        print_vertex(cout, to);
        cout << " " << distance << " " << error << " " << weight << "\n";
    }

    cerr << edges_num << " edges ok\n";

}

