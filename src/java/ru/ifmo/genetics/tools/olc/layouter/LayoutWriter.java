package ru.ifmo.genetics.tools.olc.layouter;

import java.io.IOException;

public interface LayoutWriter {
    
    public void addLayout(int cur, int shift) throws IOException;
    
    public void close() throws IOException;

    public void flush() throws IOException;
    
}
