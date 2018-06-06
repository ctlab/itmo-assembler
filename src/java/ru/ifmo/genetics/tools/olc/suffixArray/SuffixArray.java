package ru.ifmo.genetics.tools.olc.suffixArray;

import ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString;
import ru.ifmo.genetics.tools.olc.arrays.FiveByteArray;

import java.io.*;

public class SuffixArray {

    final GluedDnasString text;

    final FiveByteArray array;

    /**
     * Array length. Text may have another length!<br></br>
     * Supposing (0 <= length <= Integer.MAX_VALUE)
     */
    public final int length;

    
    /**
     * Loads array from 5b format
     */
    public SuffixArray(GluedDnasString text, File f) throws IOException {
        this.text = text;
        InputStream is = new BufferedInputStream(new FileInputStream(f));
        array = new FiveByteArray(is);
        length = array.length;
        is.close();
    }
    
    /**
     * Loads array from high/low format
     */
    public SuffixArray(GluedDnasString text, String filename) throws IOException {
        this.text = text;
        array = new FiveByteArray(filename);
        length = array.length;
    }
    
    public SuffixArray(GluedDnasString text, int length) {
        this.text = text;
        this.length = length;
        array = new FiveByteArray(length);
    }


    /**
     * Saves array to 5b format
     */
    public void save(File f) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
        array.save(os);
        os.close();
    }
    
    /**
     * Saves array to high/low format
     */
    public void save(String filename) throws IOException {
        array.save(filename);
    }
    

    public long get(int index) {
        return array.get(index);
    }
    
    void set(int index, long suffix) {
        array.set(index, suffix);
    }
    
    
    public int getChar(int indexInSufArray, int posInSuffix) {
        return text.get(get(indexInSufArray) + posInSuffix);
    }
    
    public int getCharWithLastZeros(int indexInSufArray, int posInSuffix) {
        return text.getWithLastZeros(get(indexInSufArray) + posInSuffix);
    }
    


    void swap(int pos1, int pos2) {
        long v1 = get(pos1);
        long v2 = get(pos2);
        set(pos1, v2);
        set(pos2, v1);
    }
    
    /**
     * Swaps array[pos1 .. (pos1 + len - 1)] with array[pos2 .. (pos2 + len - 1)].
     */
    void vecswap(int pos1, int pos2, int len) {
        for (int i = 0; i < len; i++, pos1++, pos2++) {
            swap(pos1, pos2);
        }
    }
    


    public String allToString() {
        return toString(0, length);
    }

    public String toString(int beginIndex, int endIndex) {
        StringBuilder sb = new StringBuilder();

        sb.append('[');
        if (beginIndex != 0) {
            sb.append("...");
        }
        for (int i = beginIndex; i < endIndex; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(get(i));
        }
        if (endIndex != length) {
            sb.append(", ...");
        }
        sb.append(']');
        return sb.toString();
    }
    
    public String suffixToString(long suffix) {
        return text.toString(suffix, Math.min(text.length, suffix + 100));
    }
    
    public String toGoodString(int beginIndex, int endIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("i\tSA[i]\tsuffix\n");
        for (int i = beginIndex; i < endIndex; i++) {
            sb.append(i);
            sb.append('\t');
            sb.append(get(i));
            sb.append('\t');
            sb.append(suffixToString(get(i)));
            sb.append('\n');
        }
        return sb.toString();
    }

    public String allToGoodString() {
        return toGoodString(0, length);
    }

}
