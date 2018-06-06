#include <iostream>

using namespace std;

size_t read_vertex(istream & in)
{
    size_t res;
    in >> res;
    res <<= 1;
    if (in.peek() == 'r')
    {
        string rc;
        in >> rc;
        ++res;
    }
    return res;
}

int main()
{
    while (1)
    {
        size_t from = read_vertex(cin); size_t to = read_vertex(cin);
        double d, e, w;
        cin >> d >> e >> w;
        if (!cin)
            break;

        cout << from << " " << to << "\n";
    }
}
