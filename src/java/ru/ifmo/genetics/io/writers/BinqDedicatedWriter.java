package ru.ifmo.genetics.io.writers;

import org.apache.log4j.Logger;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.LightDnaQ;
import ru.ifmo.genetics.io.CommentableSink;
import ru.ifmo.genetics.io.IOUtils;
import ru.ifmo.genetics.io.Sink;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.utils.tool.Tool;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BinqDedicatedWriter extends AbstractDedicatedWriter<DnaQ> {


    public BinqDedicatedWriter(File file) throws IOException {
        super(file);
        writingThread = new Thread(new WriterTask(
            new BufferedOutputStream(new FileOutputStream(file))
        ));
    }


    // This method isn't used, because we need another method signature.
    @Override
    protected void writeData(Iterable<DnaQ> data, PrintWriter out) {}

    // Using this method to write data.
    protected void writeData(Iterable<DnaQ> data, OutputStream out) throws IOException {
        writeDataStatic(data, out);
    }


    static void writeDataStatic(Iterable<DnaQ> data, OutputStream out) throws IOException {
        writeDataStatic(data, out, false);
    }
    static void writeDataStatic(Iterable<DnaQ> data, OutputStream out, boolean checkNoReads) throws IOException {
        for (DnaQ dnaq : data) {
            IOUtils.putByteArray(dnaq.toByteArray(), out);
        }
        if (checkNoReads && !data.iterator().hasNext()) {
            String name = "no_name";
            if (data instanceof NamedSource) {
                name = ((NamedSource<DnaQ>) data).name();
            }
            throw new IllegalArgumentException("No reads found in input library '" +name+ "'!");
        }
    }




    private class WriterTask implements Runnable {
        private final OutputStream out;

        public WriterTask(OutputStream outputStream) {
            this.out = outputStream;
        }

        @Override
        public void run() {
            long written = 0;
            while (true) {
                List<DnaQ> data = null;
                try {
                    data = dataQueue.take();
                } catch (InterruptedException e) {
                    log.error("Writing thread was interrupted");
                    break;
                }

                if ((data.size() == 1) && (data.get(0) == null)) {  // stop marker
                    break;
                }

                try {
                    writeData(data, out);
                } catch (IOException e) {
                    Tool.error(log, "Error while writing data", e);
                    break;
                }
                written += data.size();
            }
            try {
                out.close();
            } catch (IOException e) {
                Tool.error(log, "Error while closing output stream", e);
            }
            Tool.debug(log, written + " sequences written");
        }
    }

}
