package ru.ifmo.genetics.tools.olc.arrays;

import java.io.*;

import static ru.ifmo.genetics.tools.olc.arrays.Util.read5ByteFromStream;
import static ru.ifmo.genetics.tools.olc.arrays.Util.write5ByteToStream;

public class FiveByteArray {
    private static final long MAX_VALUE = (1L << 40) - 1;

    private static final long HIGH_MASK = 0xFFL;
    private static final long LOW_MASK = 0xFFFFFFFFL;


    private byte[] arrayHigh;
    private int[] arrayLow;

    /**
     * Supposing (0 <= lenght <= Integer.MAX_VALUE)
     */
    public final int length;

    
    public FiveByteArray(int length) {
        this.length = length;
        arrayHigh = new byte[length];
        arrayLow = new int[length];
    }
    
    public FiveByteArray(FiveByteArray other) {
        arrayHigh = other.arrayHigh.clone();
        arrayLow = other.arrayLow.clone();
        length = other.length;
    }
    
    /**
     *  Loads array from high/low format
     */
    public FiveByteArray(String filename) throws IOException {
        // reading high array
        FileInputStream fileIn = new FileInputStream(filename + ".high");
        DataInputStream in = new DataInputStream(new BufferedInputStream(fileIn));
        long len = fileIn.getChannel().size();
        if (len > Integer.MAX_VALUE) {
            throw new RuntimeException("Unsupported array length!");
        }
        length = (int) len;
        
        arrayHigh = new byte[length];
        in.read(arrayHigh);
        in.close();
        
        // reading low array
        fileIn = new FileInputStream(filename + ".low");
        in = new DataInputStream(new BufferedInputStream(fileIn));
        len = fileIn.getChannel().size();
        if (len != 4L * length) {
            throw new RuntimeException("High and low arrays have different length!");
        }
        
        arrayLow = new int[length];
        for (int i = 0; i < length; i++) {
            arrayLow[i] = in.readInt();
        }
        in.close();
    }
    
    /**
     *  Saves array to high/low format
     */
    public void save(String filename) throws IOException {
        // writing high array
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename + ".high")));
        out.write(arrayHigh);
        out.close();

        // writing low array
        out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename + ".low")));
        for (int v : arrayLow) {
            out.writeInt(v);
        }
        out.close();
    }
    

    /***
     * Loads array from 5b format
     */
    public FiveByteArray(InputStream is) throws IOException {
        long l = read5ByteFromStream(is);
        if (l > Integer.MAX_VALUE) {
            throw new RuntimeException("Unsupported array length!");
        }
        length = (int) l;
        
        arrayHigh = new byte[length];
        arrayLow = new int[length];
        
        long xor = 0;
        for (int i = 0; i < length; i++) {
            long value = read5ByteFromStream(is);
            
            // Inline set method

            //        if (value < 0 || value > MAX_VALUE) {
            //            throw new AssertionError("Value " + value + " unsupported");
            //        }
            arrayHigh[i] = (byte) (value >> 32);
            arrayLow[i] = (int) value;
            
            xor ^= value;
        }
        
        long checksum = read5ByteFromStream(is);
        if (xor != checksum) {
            throw new UnsupportedEncodingException();
        }
    }

    /***
     * Saves array to 5b format
     */
    public void save(OutputStream os) throws IOException {
        write5ByteToStream(os, length);
        
        long xor = 0;
        for (int i = 0; i < length; i++) {
            long value = get(i);
            write5ByteToStream(os, value);
            xor ^= value;
        }
        
        write5ByteToStream(os, xor);
    }

    
    public long get(int index) {
        long h = arrayHigh[index] & HIGH_MASK;
        long l = arrayLow[index] & LOW_MASK;
        return (h << 32) | l;
    }

    public void set(int index, long value) {
//        if (value < 0 || value > MAX_VALUE) {
//            throw new AssertionError("Value " + value + " unsupported");
//        }
        arrayHigh[index] = (byte) (value >> 32);
        arrayLow[index] = (int) value;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < length; i++) {
            sb.append(get(i));
            if (i < length - 1) {
                sb.append(", ");
            } else {
                sb.append("]");
            }

        }
        return sb.toString();
    }
    
}
