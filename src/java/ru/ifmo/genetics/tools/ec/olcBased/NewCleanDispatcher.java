package ru.ifmo.genetics.tools.ec.olcBased;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.ifmo.genetics.io.MultiFile2MemoryMap;
import ru.ifmo.genetics.utils.tool.Progress;

public class NewCleanDispatcher {

    final DataInputStream in;
    final int workRange;

    long kmersProcessed = 0;
    long lastKmersProcessed = 0;
    MultiFile2MemoryMap mf;

    Progress progress;

    long lastTime;

    public NewCleanDispatcher(DataInputStream in, int workRange, MultiFile2MemoryMap mf, Progress progress) {
        this.in = in;
        this.workRange = workRange;
        this.mf = mf;
        this.progress = progress;

        this.lastTime = System.currentTimeMillis();
    }

    public synchronized List<byte[]> getWorkRange() {
        List<byte[]> list = new ArrayList<byte[]>();
        while (true) {
            try {
                int chain = in.readInt();
                int l = in.readInt() + 4;
                byte[] temp = new byte[l];
                for (int i = 0; i < 4; ++i) {
                    temp[i] = (byte)((chain >>> (8 * (3 - i))) & 255);
                }
                int tl = 4;
                while (tl < l) {
                    tl += in.read(temp, tl, l - tl);
                }

                list.add(temp);
                if (list.size() == workRange) {
                    break;
                }

                ++kmersProcessed;
                /*
                if (kmersProcessed - lastKmersProcessed >= 1000) {
                    System.err.println(kmersProcessed + " kmers processed");
                    lastKmersProcessed = kmersProcessed;
                    mf.dump();
                    System.err.println("dumped");
                    System.err.println("it took " + (System.currentTimeMillis() - lastTime) / 1000 + " s");
                    lastTime = System.currentTimeMillis();
                }
                */
            } catch (EOFException e) {
                break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        progress.updateDoneTasksGreatly(Kmer2ReadIndexBuilder.kmersProcessed);
//        if (System.currentTimeMillis() - lastTime >= 5 * 60 * 1000) {
//            lastTime = System.currentTimeMillis();
//            System.err.println("chains processed: " + Kmer2ReadIndexBuilder.kmersProcessed);
//            System.err.println("reads processed:  " + Kmer2ReadIndexBuilder.readsProcessed);
//            System.err.println("reads skipped:    " + Kmer2ReadIndexBuilder.readsSkipped);
//            System.err.println("reads changed:    " + Kmer2ReadIndexBuilder.readsChanged);
//            try {
//                mf.dump();
//            } catch (IOException e) {
//                System.err.println("ERROR: unable to dump");
//            }
//            System.err.println("----------------------");
//        }
        return list.isEmpty() ? null : list;
    }

}
