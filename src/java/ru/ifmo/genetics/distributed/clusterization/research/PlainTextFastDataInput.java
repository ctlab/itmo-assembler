package ru.ifmo.genetics.distributed.clusterization.research;

import java.io.*;
import java.util.StringTokenizer;

public class PlainTextFastDataInput implements DataInput {
    private BufferedReader br;
    private StringTokenizer st;

    public PlainTextFastDataInput(InputStream inputStream) {
        br = new BufferedReader(new InputStreamReader(inputStream));
    }

    public PlainTextFastDataInput(FileReader fileReader) {
        br = new BufferedReader(fileReader);
    }

    @Override
    public void readFully(byte[] b) throws IOException {
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return 0;
    }

    @Override
    public boolean readBoolean() throws IOException {
        return Boolean.parseBoolean(readToken());
    }

    @Override
    public byte readByte() throws IOException {
        return Byte.parseByte(readToken());
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return readInt();
    }

    @Override
    public short readShort() throws IOException {
        return Short.parseShort(readToken());
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return readInt();
    }

    @Override
    public char readChar() throws IOException {
        return 0;
    }

    @Override
    public int readInt() throws IOException {
        return Integer.parseInt(readToken());
    }

    @Override
    public long readLong() throws IOException {
        return Long.parseLong(readToken());
    }

    @Override
    public float readFloat() throws IOException {
        return Float.parseFloat(readToken());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.parseDouble(readToken());
    }

    @Override
    public String readLine() throws IOException {
        st = null;
        return br.readLine();
    }

    public String readToken() throws IOException {
        while (st == null || !st.hasMoreTokens()) {
            String s = br.readLine();
            if (s == null) {
                return null;
            }
            st = new StringTokenizer(s);
        }
        return st.nextToken();
    }

    @Override
    public String readUTF() throws IOException {
        return readToken();
    }
}
