package ru.ifmo.genetics.tools.olc.layouter;

public class LayoutPart implements Comparable<LayoutPart> {
    public int readNum;
    public int shift;

    public LayoutPart(int readNum, int shift) {
        super();
        this.readNum = readNum;
        this.shift = shift;
    }

    public LayoutPart(LayoutPart o) {
        readNum = o.readNum;
        shift = o.shift;
    }

    @Override
    public String toString() {
        return "LayoutPart [to=" + readNum + ", shift=" + shift + "]";
    }

    @Override
    public int hashCode() {
        return shift * 1000000007 + readNum * 7;
    }

    @Override
    public boolean equals(Object o) {
        LayoutPart other = (LayoutPart)o;
        return readNum == other.readNum && shift == other.shift;
    }

    @Override
    public int compareTo(LayoutPart o) {
        if (shift != o.shift) {
            return shift - o.shift;
        }
        return readNum - o.readNum;
    }

}

