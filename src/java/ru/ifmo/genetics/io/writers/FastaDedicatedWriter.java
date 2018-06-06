package ru.ifmo.genetics.io.writers;

import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.TextUtils;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class FastaDedicatedWriter extends AbstractCommentableDedicatedWriter<LightDna> {

    public static int LINE_LIMIT = 70;


    public FastaDedicatedWriter(File file, boolean compress) throws IOException {
        super(file);
        if (compress) {
            this.file = new File(FileUtils.addExtensionIfNot(file.getPath(), ".gz"));
            writingThread = new Thread(new WriterTask(
                    new GZIPOutputStream(new FileOutputStream(this.file))
            ));
        }
    }

    @Override
    protected void writeData(Iterable<String> comments, Iterable<LightDna> data, PrintWriter out) {
        writeData(comments, data, out, true);
    }

    static void writeData(Iterable<String> comments, Iterable<? extends LightDna> data, PrintWriter out,
                           boolean checkSize) {
        Iterator<String> comIt = comments.iterator();
        Iterator<? extends LightDna> dataIt = data.iterator();
        while (dataIt.hasNext()) {
            out.println(">" + comIt.next());

            String dna = DnaTools.toString(dataIt.next());
            if (dna.length() == 0) {
                throw new RuntimeException("Empty DNA!");
            }
            TextUtils.printWithLineLimit(
                    dna,
                    out,
                    LINE_LIMIT
            );
        }
        if (checkSize && comIt.hasNext()) {
            throw new RuntimeException("Different size of data and comments to write!");
        }
    }

}
