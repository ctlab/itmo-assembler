package ru.ifmo.genetics.tools.olc.arrays;

import java.io.*;

/**
 * Class writes FiveByteArray piecemeal and without saving in memory. <br>
 * FiveByteArray are saving in 5b format.
 */
public class FiveByteArrayWriter {
    DataOutputStream outHigh, outLow;
    
    public FiveByteArrayWriter(String filename) throws FileNotFoundException {
        outHigh = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename + ".high")));
        outLow = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename + ".low")));
    }
    
    public void write(long value) throws IOException {
//      if (value < 0 || value > MAX_VALUE) {
//          throw new AssertionError("Value " + value + " unsupported");
//      }
      byte h = (byte) (value >> 32);
      outHigh.write(h);
      int l = (int) value;
      outLow.writeInt(l);
  }
    
    public void close() throws IOException {
        outHigh.close();
        outLow.close();
    }
}
