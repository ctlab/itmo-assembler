package ru.ifmo.genetics.tools.ec.simple;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.DnaQBuilder;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.structures.map.ArrayLong2LongHashMap;
import ru.ifmo.genetics.tools.ec.DnaQReadDispatcher;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ApplyFixesWorker implements Runnable {

    DnaQReadDispatcher dispatcher;
    CountDownLatch latch;

    ArrayLong2LongHashMap fixes;
    boolean interrupted = false;
    int len;
    long id;

    public ApplyFixesWorker(DnaQReadDispatcher dispatcher, CountDownLatch latch, ArrayLong2LongHashMap fixes, int len, long id) {
        this.dispatcher = dispatcher;
        this.latch = latch;
        this.fixes = fixes;
        this.len = len;
        this.id = id;
    }

    private DnaQ fastCorrect(DnaQ read) {
        DnaQBuilder db = new DnaQBuilder(read);
        for (int i = 0; i + len <= db.length(); ++i) {
            long currentKmer = DnaTools.toLong(db.subDnaQ(i, i + len));
            if (!fixes.containsKey(currentKmer)) {
                continue;
            }
            int delta = 0;
            long fix = fixes.get(currentKmer);
            for (int j = 0; j < 8; ++j) {
                int cfix = (int)(fix >> (j * 8)) & 255;
                if (cfix == 0) {
                    break;
                }
                int type = cfix >> 5;
                int pos = len - (cfix & 31);
                int rpos = i + pos;
                if (type == 0) {
                    db.insert(rpos, db.nucAt(rpos));
                    ++delta;
                } else if (type == 4) {
                    db.delete(rpos);
                    //--delta;
                } else {
                    db.setNuc(rpos, (byte)(db.nucAt(rpos) ^ type));
                }
            }
            if (delta < -1) {
                delta = -1;
            }
            i += delta;
        }
        return db.toDnaQ();
    }

    public void interrupt() {
        interrupted = true;
    }

    public void run() {
        while (!interrupted) {
            List<DnaQ> list = dispatcher.getWorkRange(id);
            if (list == null) {
                break;
            }
            for (int i = 0; i < list.size(); ++i) {
                list.set(i, fastCorrect(list.get(i)));
            }
            try {
                dispatcher.writeDnaQs(list, id);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        latch.countDown();
    }

}
