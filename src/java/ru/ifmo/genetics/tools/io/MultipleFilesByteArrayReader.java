package ru.ifmo.genetics.tools.io;

import org.apache.log4j.Logger;
import ru.ifmo.genetics.utils.tool.Tool;

import java.io.*;

public class MultipleFilesByteArrayReader {

    File[] files;
    DataInputStream in = null;
    int ind = 0;

    boolean eof = false;
    Logger logger = Logger.getLogger("reader");

    public MultipleFilesByteArrayReader(File[] files) throws FileNotFoundException, EOFException {
        this.files = files;
        in = new DataInputStream(new BufferedInputStream(new FileInputStream(files[0])));
        Tool.debug(logger, "reading from " + files[0].getName());
    }

    public MultipleFilesByteArrayReader(String[] filenames) throws FileNotFoundException, EOFException {
        files = new File[filenames.length];
        for (int i = 0; i < files.length; ++i) {
            files[i] = new File(filenames[i]);
        }
        in = new DataInputStream(new BufferedInputStream(new FileInputStream(files[0])));
    }

    public synchronized int read(byte[] b) throws IOException {
        if (eof) {
            return -1;
        }
        while (true) {
            int x = in.read(b);
            if (x > 0) {
                return x;
            }

            ++ind;
            if (ind == files.length) {
                eof = true;
                return -1;
            }
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(files[ind])));
            Tool.debug(logger, "reading from " + files[ind].getName());
        }
    }

}
