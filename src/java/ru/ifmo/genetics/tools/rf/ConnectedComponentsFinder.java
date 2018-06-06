package ru.ifmo.genetics.tools.rf;

import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.structures.arrays.BigBytesArray;
import ru.ifmo.genetics.structures.set.BigLongHashSet;
import ru.ifmo.genetics.structures.set.LongHashSetInterface;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ConnectedComponentsFinder {
    private static int k;
    private static int p; // = k + 1

    private LongHashSetInterface edges;
    private BigBytesArray marks;

    private static List<String> kmersFiles = new ArrayList<String>();

    private void buildGraph() throws IOException {
        long totalToRead = 0;
        for (String kmersFile : kmersFiles) {
            FileInputStream fileIn = new FileInputStream(kmersFile);
            DataInputStream in = new DataInputStream(new BufferedInputStream(fileIn));
            long toRead = fileIn.getChannel().size() / 8;
            totalToRead += toRead;
            fileIn.close();
        }
        edges = new BigLongHashSet(totalToRead, 0.65f);
        System.err.println("have to read " + totalToRead + " k-mers");
        long xx = 0;
        Timer timer = new Timer();
        timer.start();
        for (String kmersFile : kmersFiles) {
            FileInputStream fileIn = new FileInputStream(kmersFile);
            DataInputStream in = new DataInputStream(new BufferedInputStream(fileIn));
            long toRead = fileIn.getChannel().size() / 8;
            xx += toRead;
            for (long i = 0; i < toRead; ++i) {
                edges.add(in.readLong());
            }
            fileIn.close();
            System.err.println(xx + " out of " + totalToRead + " k-mers loaded");

            double done = ((double) xx) / totalToRead;
            double total = timer.getTime() / done / 1000;
            double remained = total * (1 - done);
            double elapsed = total * done;

            System.err.println(100 * done + "% done");
            System.err.println("estimated  total time: " + total + ", remaining: " + remained + ", elapsed: "
                    + elapsed);
        }
    }

    public long markComponent(long e, byte c) {
        long vertexMask = (1L << (2*k)) - 1;
        Queue<Long> q = new ArrayDeque<Long>();

        q.add(e);

        long size = 0;

        while (!q.isEmpty()) {
            long currentEdge = q.poll();
            long i = edges.getPosition(currentEdge);
            if (i == -1) {
                continue;
            }

            if (marks.get(i) == c) {
                continue;
            }

            marks.set(i, c);
            ++size;

            if ((size & 0xfffff) == 0) {
                System.err.println(size);
            }

            for (int j = 0; j < 4; ++j) {
                q.add(((currentEdge & vertexMask) << 2) + j);
                q.add((currentEdge >>> 2) + j * (vertexMask + 1));
            }
        }

        return size;
    }

    public void run() throws IOException {
        buildGraph();

        long n = edges.capacity();
        System.err.println(n);
        marks = new BigBytesArray(n);
        int componentsNumber = 0;
        long remained = edges.size();
        for (long i = 0; i < n; ++i) {
            if (marks.get(i) != 0) {
                continue;
            }

            long e = edges.elementAt(i);
            if (e == -1) { // :TODO: change to FREE someway
                continue;
            }

            ++componentsNumber;

            System.err.println("component " + componentsNumber + ":");
            long componentSize = markComponent(e, (byte)1);
            remained -= componentSize;
            System.err.println("component " + componentsNumber + " calculateSize: " + componentSize);
            System.err.println("remained " + remained + " vertices");
        }
    }

    public static void main(String args[]) {
        if (args.length < 2) {
            System.err.println("Usage: ConnectedComponentsFinder <k> <kmers>+");
            System.exit(666);
        }

        kmersFiles.addAll(Arrays.asList(args).subList(1, args.length));

        k = Integer.parseInt(args[0]);
        p = k + 1;


        Timer t = new Timer();
        try {
            new ConnectedComponentsFinder().run();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace(System.err);
            System.exit(42);
        }
        System.err.println("total time = " + t);

    }
}
