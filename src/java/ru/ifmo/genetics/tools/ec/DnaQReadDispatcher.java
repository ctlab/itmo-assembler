package ru.ifmo.genetics.tools.ec;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.io.IOUtils;
import ru.ifmo.genetics.io.sources.Source;
import ru.ifmo.genetics.utils.tool.Progress;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DnaQReadDispatcher {
    private final Iterator<DnaQ> in;
    int workRangeSize;
    final OutputStream out;

    long reads = 0;
    public long totalReadsProcessed;
    Progress progress;

    long[] ar;
    int r = 0;
    int w = 0;

    public DnaQReadDispatcher(Source<DnaQ> source, int workRangeSize, Progress progress) {
        in = source.iterator();
        this.workRangeSize = workRangeSize;
        this.progress = progress;
        out = null;
    }

    public DnaQReadDispatcher(Source<DnaQ> source, OutputStream out, int workRangeSize, long totalReadsProcessed,
                              int workersNumber, Progress progress) {
        in = source.iterator();
        this.workRangeSize = workRangeSize;
        this.out = out;
        this.totalReadsProcessed = totalReadsProcessed;
        this.progress = progress;

        ar = new long[workersNumber];
    }

    private List<DnaQ> readRange(int workRangeSize) throws IOException {
        List<DnaQ> list = new ArrayList<DnaQ>();
        while (list.size() < workRangeSize && in.hasNext()) {
            list.add(in.next());
            ++reads;
        }
        return list;
    }


    public List<DnaQ> getWorkRange(long id) {
        List<DnaQ> list;
        try {
            synchronized (in) {
                list = readRange(workRangeSize);
                ar[r] = id;
                r = (r + 1) % ar.length;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return list.isEmpty() ? null : list;
    }

    public List<DnaQ> getWorkRange() {
        List<DnaQ> list;
        try {
            synchronized (in) {
                list = readRange(workRangeSize);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return list.isEmpty() ? null : list;
    }

    public void writeDnaQs(List<DnaQ> list, long id) throws IOException, InterruptedException {
        synchronized (out) {
            while (ar[w] != id) {
                out.wait();
            }
            for (DnaQ dnaq : list) {
                IOUtils.putByteArray(dnaq.toByteArray(), out);
                ++totalReadsProcessed;
                progress.updateDoneTasks(totalReadsProcessed);
            }
            w = (w + 1) % ar.length;
            out.notifyAll();
        }
    }

    public long getReads(){
        return reads;
    }

}
