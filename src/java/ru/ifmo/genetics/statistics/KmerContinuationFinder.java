package ru.ifmo.genetics.statistics;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.DnaQBuilder;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.kmers.ShortKmer;
import ru.ifmo.genetics.io.readers.BinqReader;
import ru.ifmo.genetics.io.sources.Source;
import ru.ifmo.genetics.tools.io.LazyLongReader;
import ru.ifmo.genetics.utils.KmerUtils;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class KmerContinuationFinder {

    private static int totalReads = 0;
    private static LongSet set = new LongOpenHashSet();

    public static void main(String[] args) throws IOException {
        int md = Integer.parseInt(args[0]);
        int len = Integer.parseInt(args[1]);
        int kmerNumber = Integer.parseInt(args[2]);
        kmerNumber = 1;
        int i = 3;
        for (; (i < args.length) && !args[i].equals("--"); ++i);
        String[] kmerFiles = Arrays.copyOfRange(args, 3, i);
        String[] readFiles = Arrays.copyOfRange(args, i + 1, args.length);

        LazyLongReader reader = new LazyLongReader(kmerFiles);
        i = 0;
        while (true) {
            try {
                long[] kmers = new long[1000];
                for (int q = 0; q < kmers.length; ++q) {
                    long c = reader.readLong();
                    if (!set.contains(c)) {
                        kmers[q] = c;
                    } else {
                        --q;
                    }
                }
                ++i;
                if (i > kmerNumber) {
                    break;
                }
                processKmer(readFiles, kmers, len, md);
            } catch (EOFException e) {
                break;
            }
        }
        System.err.println("total kmers: " + set.size());
    }

    public static void processKmer(String[] readFiles, long[] kmers, int len, int md) throws IOException {
        long ct = System.currentTimeMillis();
        Long2IntMap map = new Long2IntOpenHashMap();
        int z = 0;
        for (long l : kmers) {
            map.put(l, z++);
        }

        long reads = 0;

        List<byte[]>[] dnaqs = new ArrayList[kmers.length];
        IntList[] starts = new IntArrayList[kmers.length];
        for (int q = 0; q < dnaqs.length; ++q) {
            dnaqs[q] = new ArrayList<byte[]>();
            starts[q] = new IntArrayList();
        }
        for (String s : readFiles) {
            Source<DnaQ> source = new BinqReader(s);
            for (DnaQ d : source) {
                int j = 0;
                for (ShortKmer k : ShortKmer.kmersOf(d, len)) {
                    int q;
                    if (map.containsKey(k.fwKmer())) {
                        q = map.get(k.fwKmer());
                    } else if (map.containsKey(k.rcKmer())) {
                        q = map.get(k.rcKmer());
                    } else {
                        continue;
                    }
                    if (kmers[q] == k.fwKmer()) {
                        dnaqs[q].add(d.toNucArray());
                        starts[q].add(-j);
                        ++reads;
                    }
                    if (kmers[q] == k.rcKmer()) {
                        byte[] ar = d.toNucArray();
                        for (int i = 0; i < ar.length / 2; ++i) {
                            int w = ar.length - i - 1;
                            byte b = ar[i];
                            ar[i] = (byte)(3 - ar[w]);
                            ar[w] = (byte)(3 - b);
                        }
                        dnaqs[q].add(ar);
                        starts[q].add(-d.length + j + len);
                        ++reads;
                    }
                }
                ++j;
            }
        }
        System.err.println("reading: " + (System.currentTimeMillis() - ct));
        ct = System.currentTimeMillis();
        totalReads += reads;
        for (int q = 0; q < kmers.length; ++q) {
            int mp = Integer.MAX_VALUE;
            int Mp = Integer.MIN_VALUE;
            for (int i = 0; i < starts[q].size(); ++i) {
                mp = Math.min(mp, starts[q].get(i));
                Mp = Math.max(Mp, starts[q].get(i) + dnaqs[q].get(i).length);
            }
            for (int i = 0; i < starts[q].size(); ++i) {
                starts[q].set(i, starts[q].get(i) - mp);
            }

            int[] components = new int[dnaqs[q].size()];
            int c = 0;
            for (int i = 0; i < dnaqs[q].size(); ++i) {
                byte[] cd = dnaqs[q].get(i);
                int cp = starts[q].get(i);
                int[] cmds = new int[c];
                for (int j = 0; j < i; ++j) {
                    byte[] ccd = dnaqs[q].get(j);
                    int ccp = starts[q].get(j);
                    int d = dist(cd, cp, ccd, ccp);
                    cmds[components[j]] = Math.max(cmds[components[j]], d);
                }
                int cur = -1;
                int cmd = Integer.MAX_VALUE;
                for (int j = 0; j < cmds.length; ++j) {
                    if (cmd > cmds[j]) {
                        cmd = cmds[j];
                        cur = j;
                    }
                }
                if ((cur == -1) || (cmd > md)) {
                    components[i] = c++;
                    continue;
                }
                components[i] = cur;
            }

            for (int comp = 0; comp < c; ++comp) {
                int[][] ar = new int[Mp - mp][4];
                for (int i = 0; i < components.length; ++i) {
                    if (components[i] == comp) {
                        byte[] d = dnaqs[q].get(i);
                        int s = starts[q].get(i);
                        for (int j = 0; j < d.length; ++j) {
                            ++ar[j + s][d[j]];
                        }
                    }
                }
                for (int i = 0; i < ar.length; ++i) {
                    int w = 0;
                    for (int j = 1; j < 4; ++j) {
                        if (ar[i][j] > ar[i][w]) {
                            w = j;
                        }
                    }
                    ar[i][0] = w;
                }
                for (int i = 0; i < components.length; ++i) {
                    if (components[i] == comp) {
                        DnaQ nd = new DnaQ(dnaqs[q].get(i));
                        int s = starts[q].get(i);
                        for (int j = 0; j < nd.length(); ++j) {
                            nd.setNuc(j, (byte)ar[j + s][0]);
                        }
                        for (ShortKmer kmer : ShortKmer.kmersOf(nd, len)) {
                            set.add(kmer.fwKmer());
                            set.add(kmer.rcKmer());
                        }
                    }
                }
            }
        }
        System.err.println("correcting: " + (System.currentTimeMillis() - ct));
    }

    public static int dist(byte[] a, int posA, byte[] b, int posB) {
        int beg = Math.max(posA, posB);
        int end = Math.min(posA + a.length, posB + b.length);
        byte[] na = Arrays.copyOfRange(a, beg - posA, end - posA);
        byte[] nb = Arrays.copyOfRange(b, beg - posB, end - posB);
        return dist(na, nb);
    }

    public static int dist(byte a, byte b) {
        return a == b ? 0 : 1;
    }

    public static int dist(byte[] a, byte[] b) {
        int l1 = a.length;
        int l2 = b.length;

        int[][] dist = new int[l1+1][l2+1];
        for (int i = 0; i < dist.length; ++i) {
            Arrays.fill(dist[i], Integer.MAX_VALUE);
        }
        for (int i = 0; i <= l1; ++i) {
            dist[i][0] = i;
        }
        for (int i = 0; i <= l2; ++i) {
            dist[0][i] = i;
        }
        for (int i = 1; i <= l1; ++i) {
            for (int j = 1; j <= l2; ++j) {
                dist[i][j] = Math.min(dist[i][j], dist[i - 1][j - 1] + dist(a[i - 1], b[j - 1]));
                dist[i][j] = Math.min(dist[i][j], dist[i - 1][j] + 1);
                dist[i][j] = Math.min(dist[i][j], dist[i][j - 1] + 1);
            }
        }
        return dist[l1][l2];
    }

}
