package ru.ifmo.genetics.tools;

import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.tools.olc.overlapper.Overlapper;
import ru.ifmo.genetics.utils.TextUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FullGenomeRepeatInfo {
    final String genomeFastaFile;
    final String genome;

    public final int minLen = 40;

    public final int en = 1;
    public final int ews = 40;


    public FullGenomeRepeatInfo(String genomeFastaFile) throws IOException {
        this.genomeFastaFile = genomeFastaFile;
        genome = loadFromFasta(genomeFastaFile);
    }

    void run() throws IOException {
        PrintWriter out = new PrintWriter("repeats-distribution");
        PrintWriter allSbOut = new PrintWriter("repeats-all-substr-distribution");

        System.err.println("Indexing... ");

        // index[i] = a set of positions in genome such that
        //      substrings starting in these positions equals to substring starting from position i
        List<Integer>[] index = new List[genome.length()];
        List<Integer>[] newIndex = new List[genome.length()];

        HashMap<String, Integer> firstStrPos = new HashMap<String, Integer>();
        for (int p = 0; p + minLen <= genome.length(); p++) {
            String s = genome.substring(p, p + minLen);
            Integer firstPos = firstStrPos.get(s);
            if (firstPos == null) {
                firstStrPos.put(s, p);
            } else {
                addToList(index, firstPos, p);
            }
        }

        System.out.println("Starting to identify repeats... Write any char to stop");
        for (int len = minLen; len < genome.length(); len++) {
            System.err.print("Len = " + len + ", ");

            int indexLen = 0;
            for (int i = 0; i < index.length; i++) {
                if (index[i] != null) {
                    indexLen++;
                }
            }
            allSbOut.println(len + " : " + indexLen);
            System.err.print("index len = " + indexLen + ", ");
            if (indexLen == 0) {
                break;
            }

            // increasing len and identifying repeats that can't be increased
            Arrays.fill(newIndex, null);
            int repeatsCount = 0;

            for (int pos = 0; pos + len <= index.length; pos++) {
                if (index[pos] != null) {
                    String s = genome.substring(pos, pos + len);
                    int[] increased = new int[2];

                    final int[] dx = {-1, 1};
                    for (int di = 0; di < dx.length; di++) {
                        // trying to increase repeat in direction dx[di]

                        String firstStr = getIncreasedString(pos, len, dx[di]);
                        if (firstStr != null) {
                            int newPos = pos + Math.min(dx[di], 0);
                            for (int rPos : index[pos]) {
                                String rStr = getIncreasedString(rPos, len, dx[di]);
                                boolean equals = (rStr != null) &&
                                        Overlapper.checkErrorsNumber(firstStr, rStr, en, ews);
                                if (equals) {
                                    addToList(newIndex, newPos, rPos + Math.min(dx[di], 0));
                                    increased[di]++;
                                }
                            }
                        }
                    }

                    boolean isIncreasedAll = (increased[0] == index[pos].size()) || (increased[1] == index[pos].size());
                    boolean isIncreasedOne = (increased[0] > 0) || (increased[1] > 0);
                    if (!isIncreasedOne) {
                        repeatsCount++;
                        if (Math.random() < -0.1) {
//                            System.out.println();
                            System.out.println();
                            System.out.println("Reapeat of len = " + len);
                            System.out.println("Repeat = " + s);
                            System.out.println("pos = " + pos);
                            System.out.println("index[pos].len = " + index[pos].size());
                            System.out.println("index[pos] = " + index[pos]);
                            System.out.println(TextUtils.fit("initial str = ", 25) + getSubStringToWrite(pos, len));
                            for (int rPos : index[pos]) {
                                System.out.println(TextUtils.fit("pos = " + rPos + ", str = ", 25) + getSubStringToWrite(rPos, len));
                            }
                            System.out.println();
                        }
                    }
                }
            }


            out.println(len + " : " + repeatsCount);
            System.err.println("repeats = " + repeatsCount);

            List<Integer>[] tmp = index;
            index = newIndex;
            newIndex = tmp;

            if (System.in.available() > 0) {
                break;
            }
        }
        out.close();
        allSbOut.close();
    }

    private String getIncreasedString(int pos, int len, int dx) {
        int l = pos + Math.min(dx, 0);
        int r = pos + len + Math.max(dx, 0);
        if (l < 0 || r > genome.length()) {
            return null;
        }
        return genome.substring(l, r);
    }

    private boolean addToList(List<Integer>[] list, int pos, int el) {
        if (list[pos] == null) {
            list[pos] = new ArrayList<Integer>(1);
        }
        if (list[pos].contains(el)) {
            return false;
        }
        list[pos].add(el);
        return true;
    }

    private final static int nucsToAdd = 5;
    private String getSubStringToWrite(int rPos, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = -nucsToAdd; i < 0; i++) {
            char nuc = ' ';
            int ind = rPos + i;
            if (ind >= 0) {
                nuc = genome.charAt(ind);
            }
            sb.append(nuc);
        }

        sb.append("'");
        sb.append(genome.substring(rPos, rPos + len));
        sb.append("'");

        for (int i = 1; i <= nucsToAdd; i++) {
            char nuc = ' ';
            int ind = rPos + len - 1 + i;
            if (ind < genome.length()) {
                nuc = genome.charAt(ind);
            }
            sb.append(nuc);
        }
        return sb.toString();
    }

    String loadFromFasta(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        while (true) {
            String s = br.readLine();
            if (s == null)
                break;
            if (s.startsWith(">"))
                continue;
            sb.append(s);
        }

        System.err.println("Genome length = " + sb.length());
        return sb.toString();
    }

    String reverseComplement(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = s.length() - 1; i >= 0; i--) {
            sb.append(DnaTools.complement(s.charAt(i)));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Using: java FullGenomeRepeatInfo <genome-fasta-file>");
            return;
        }

        new FullGenomeRepeatInfo(args[0]).run();
    }

}
