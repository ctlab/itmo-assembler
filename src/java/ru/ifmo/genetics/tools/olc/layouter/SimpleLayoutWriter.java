package ru.ifmo.genetics.tools.olc.layouter;

import java.io.IOException;
import java.io.Writer;

public class SimpleLayoutWriter implements LayoutWriter {

    private Writer writer;
    int contigsMade = 0;
    
    public SimpleLayoutWriter(Writer writer) {
        this.writer = writer;
    }
    
    @Override
    public void addLayout(int cur, int shift) throws IOException {
        writer.write(cur + " " + shift + "\n");
    }
    
    @Override
    public void close() throws IOException {
        writer.close();
    }
    
    @Override
    public void flush() throws IOException {
        writer.write("-1 " + (contigsMade++) + "\n");
    }
}
