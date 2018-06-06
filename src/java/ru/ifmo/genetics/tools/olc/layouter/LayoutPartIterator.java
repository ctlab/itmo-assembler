package ru.ifmo.genetics.tools.olc.layouter;

import ru.ifmo.genetics.io.readers.ReaderInSmallMemory;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class LayoutPartIterator implements Iterator<LayoutPart> {

    private static final int EMPTY = -2;
    private static final int END = -1;
    
    private ReaderInSmallMemory reader;
    LayoutPart buf = new LayoutPart(EMPTY, EMPTY);
    
    public LayoutPartIterator(ReaderInSmallMemory reader) {
        this.reader = reader;
    }
    
    @Override
    public boolean hasNext() {
        if (buf.readNum != EMPTY)
            return buf.readNum != END;
        
        try {
            buf = new LayoutPart(reader.readInteger(), reader.readInteger());
            return buf.readNum != END;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public LayoutPart next() {
        // once an end - always an end
        if (buf.readNum == END) {
            return new LayoutPart(buf);
        }
        
        if (buf.readNum != EMPTY) {
            LayoutPart r = new LayoutPart(buf);
            buf.readNum = EMPTY;
            return r;
        }
        try {
            return new LayoutPart(reader.readInteger(), reader.readInteger());
        } catch (IOException e) {
            throw new NoSuchElementException(e.toString());
        }
    }

    @Override
    public void remove() {
        // once an end - always an end
        if (buf.readNum == END) {
            return;
        }
        
        if (buf.readNum != EMPTY) {
            buf.readNum = EMPTY;
            return;
        }
        
        try {
            // this is needed to avoid END removing
            buf = new LayoutPart(reader.readInteger(), reader.readInteger());
            if (buf.readNum != END) {
                buf.readNum = EMPTY;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
