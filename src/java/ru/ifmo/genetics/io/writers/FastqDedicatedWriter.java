package ru.ifmo.genetics.io.writers;

import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.dna.LightDnaQ;
import ru.ifmo.genetics.io.formats.Illumina;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class FastqDedicatedWriter extends AbstractCommentableDedicatedWriter<LightDnaQ> {

    public final QualityFormat qf;

    public FastqDedicatedWriter(File file, boolean compress, QualityFormat qf) throws IOException {
        super(file);
        if (compress) {
            this.file = new File(FileUtils.addExtensionIfNot(file.getPath(), ".gz"));
            writingThread = new Thread(new WriterTask(
                    new GZIPOutputStream(new FileOutputStream(this.file))
            ));
        }
        this.qf = qf;
    }

    @Override
    protected void writeData(Iterable<String> comments, Iterable<LightDnaQ> data, PrintWriter out) {
        writeData(comments, data, out, qf, true);
    }

    static void writeData(Iterable<String> comments, Iterable<? extends LightDnaQ> data, PrintWriter out,
                                  QualityFormat qf, boolean checkSize) {
        Iterator<String> comIt = comments.iterator();
        Iterator<? extends LightDnaQ> dataIt = data.iterator();
        while (dataIt.hasNext()) {
            String comment = comIt.next();
            LightDnaQ dnaQ = dataIt.next();

            out.println("@" + comment);
            String dnaQstr = DnaTools.toString(dnaQ);
            if (dnaQstr.length() == 0) {
                throw new RuntimeException("Empty DnaQ!");
            }
            out.println(dnaQstr);

            out.println("+");
            out.println(DnaTools.toPhredString(dnaQ, qf));
        }
        if (checkSize && comIt.hasNext()) {
            throw new RuntimeException("Different size of data and comments to write!");
        }
    }

}
