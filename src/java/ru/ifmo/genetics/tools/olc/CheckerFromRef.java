package ru.ifmo.genetics.tools.olc;

import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.statistics.QuantitativeStatistics;
import ru.ifmo.genetics.tools.olc.overlapper.Overlapper;
import ru.ifmo.genetics.tools.olc.overlaps.Overlaps;
import ru.ifmo.genetics.tools.olc.overlaps.OverlapsList;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class that loads reads' info from file and answers to queries "is overlap x good?".
 */
public class CheckerFromRef {
    public final String READS_INFO_FILE = ReadsGenerator.READS_INFO_FILE;
    public final String GENOME_FASTA_FILE = "../data/full/coli.fasta";


    final List<ReadsGenerator.ReadInfo> reads;
    public final String genome;
    public final String genomeRC;


    public CheckerFromRef() {
        try {
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(READS_INFO_FILE));
            reads = (List<ReadsGenerator.ReadInfo>) is.readObject();
            is.close();
            System.err.println("Overlaps checker: Loaded " + reads.size() + " reads' info");
            genome = ReadsGenerator.loadFromFasta(GENOME_FASTA_FILE);
            genomeRC = DnaTools.reverseComplement(genome);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public ReadsGenerator.ReadInfo getReadInfo(int i) {
        ReadsGenerator.ReadInfo ri = new ReadsGenerator.ReadInfo(reads.get(i / 2));
        if (i % 2 == 1) {
            ri.rc = !ri.rc;
        }
        return ri;
    }
    
    public String getGoodDNAString(ReadsGenerator.ReadInfo ri) {
        if (!ri.rc) {
            return genome.substring(ri.beginPos, ri.beginPos + ri.len);
        } else {
            int begin = genome.length() - 1 - (ri.beginPos + ri.len - 1);
            int end = genome.length() - 1 - (ri.beginPos) + 1;
            return genomeRC.substring(begin, end);
        }
    }

    public char getChar(ReadsGenerator.ReadInfo ri, int pos) {
        int rPos = ri.beginPos;
        if (!ri.rc) {
            rPos += pos;
        } else {
            rPos += ri.len - 1 - pos;
        }

        if (rPos < 0 || rPos >= genome.length()) {
            return 0;
        }
        char res = genome.charAt(rPos);
        if (ri.rc) {
            res = DnaTools.complement(res);
        }
        return res;
    }


    public boolean checkOverlap(int i, int j, int centerShift) {
        ReadsGenerator.ReadInfo ri = getReadInfo(i);
        ReadsGenerator.ReadInfo rj = getReadInfo(j);

        int beginShift = Overlaps.centerShiftToBeginShiftUsingLen(ri.len, rj.len, centerShift);
        if (beginShift < 0) {
            return checkOverlap(j, i, -centerShift);
        }

        List<Integer> errors = new ArrayList<Integer>(5);
        boolean gf = true;
        for (int p = 0; (p < rj.len) && gf; p++) {
            char c1 = getChar(ri, beginShift + p);
            char c2 = getChar(rj, p);
            if (c1 != c2) {
                if (c1 == 0 || c2 == 0) {
                    gf = false;
                } else {
                    errors.add(p);
                }
            }
        }
        gf &= Overlapper.checkErrorsNumber(errors, 2, 100);


        errors.clear();
        boolean gs = true;
        for (int p = 0; (p < ri.len) && gs; p++) {
            char c1 = getChar(ri, p);
            char c2 = getChar(rj, -beginShift + p);
            if (c1 != c2) {
                if (c1 == 0 || c2 == 0) {
                    gs = false;
                } else {
                    errors.add(p);
                }
            }
        }
        gs &= Overlapper.checkErrorsNumber(errors, 2, 100);

//        if (Math.random() < 0.0001) {
//            System.out.println("Ri = " + ri + ", Rj = " + rj + ", beginShift = " + beginShift +
//                ", gf = " + gf + ", gs = " + gs);
//        }

        return gf || gs;
    }


    public void checkGoodOverlapsWasFound(Overlaps overlaps) {
        System.err.println();
        System.err.println("check good overlaps was found");

        System.err.println("Sorting");
        List<ReadsGenerator.ReadInfo> sortedReads = new ArrayList<ReadsGenerator.ReadInfo>(reads);
        Collections.sort(sortedReads);

        System.err.println("Checking");
        int kk = 0;
        int noOverlap = 0, notFound = 0;

        for (int i = 0; i + 1 < sortedReads.size(); i++) {
            ReadsGenerator.ReadInfo ri1 = sortedReads.get(i);
            ReadsGenerator.ReadInfo ri2 = sortedReads.get(i + 1);
            int r1 = getNotRCReadNumber(ri1);
            int r2 = getNotRCReadNumber(ri2);

            if (i < kk) {
                System.err.println("ri1 = " + ri1);
                System.err.println("r1 = " + r1);
//                System.err.println("ri2 = " + ri2);
            }

            int beginShift = ri2.beginPos - ri1.beginPos;
            int centerShift = overlaps.beginShiftToCenterShift(r1, r2, beginShift);
            int overlap = overlaps.calculateOverlapLen(r1, r2, centerShift);
            if (i < kk) {
                System.err.println("beginShift = " + beginShift + ", centerShift = " + centerShift + ", " +
                        "overlap = " + overlap);
            }

            if (overlap < 40) {
                noOverlap++;
                if (i < kk) {
                    System.err.println("no overlap");
                }
            } else {
                boolean found = overlaps.containsOverlap(r1, r2, centerShift);
                if (!found) {
                    notFound++;
                    if (i < kk) {
                        System.err.println("not found");
                    }
                } else {
                    if (i < kk) {
                        System.err.println("ok");
                    }
                }
            }
        }
        System.err.println("All = " + (sortedReads.size() - 1) + ", no overlap = " + noOverlap +
                ", not found = " + notFound);

        System.err.println();
    }

    public void checkOverlapsRC(Overlaps overlaps) {
        System.err.println();
        System.err.println("check overlaps rc");

        QuantitativeStatistics<Integer> stat = new QuantitativeStatistics<Integer>();
        int all = 0, bad = 0;
        for (int i = 0; i < overlaps.readsNumber; i++) {
            /*   unchecked
            if (overlaps.overlaps[i] != null) {
                ReadsGenerator.ReadInfo ri = getReadInfo(i);
                OverlapsList list = overlaps.overlaps[i];
                for (int j = 0; j < list.size(); j++) {
                    int to = list.getTo(j);
                    int centerShift = list.getCenterShift(j);
                    int overlap = overlaps.calculateOverlapLen(i, to, centerShift);

                    ReadsGenerator.ReadInfo rj = getReadInfo(to);
                    if (ri.rc != rj.rc) {
                        bad++;
                        stat.add(overlap);
                    }
                    all++;
                }
            }
            */
        }

        stat.printToFile("work/overlaps_badRC.stat");
        System.err.println("All = " + all + ", bad = " + bad);
        System.err.println();
    }

    public int getNotRCReadNumber(ReadsGenerator.ReadInfo read) {
        if (!read.rc) {
            return 2 * read.no;
        } else {
            return 2 * read.no + 1;
        }
    }
    public int getRCReadNumber(ReadsGenerator.ReadInfo read) {
        if (!read.rc) {
            return 2 * read.no + 1;
        } else {
            return 2 * read.no;
        }
    }


    public String genomeRC(int beginPos, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int rPos = beginPos - i;
            char res = genome.charAt(rPos);
            res = DnaTools.complement(res);
            sb.append(res);
        }

        return sb.toString();
    }

    /*
    public void checkOverlaps(Overlaps overlaps) {
        int all = 0;
        int wrong = 0;
        int notFound = 0;

        for (int i = 0; i < overlaps.readsNumber; i++) {
            OverlapsList list = overlaps.getForwardOverlaps(i);
            for (int j = 0; j < list.size(); j++) {
                int to = list.getTo(j);
                int shift = list.getCenterShift(j);

                all++;
                boolean good = checkOverlap(i, to, shift);
                if (!good) {
                    wrong++;
                }
            }
        }

        for (String s : goodOverlaps) {
            StringTokenizer st = new StringTokenizer(s);
            int i = Integer.parseInt(st.nextToken());
            int j = Integer.parseInt(st.nextToken());
            int sh = Integer.parseInt(st.nextToken());

            if (!overlaps.containsOverlap(i, j, sh)) {
                notFound++;
            }
        }

        System.out.println(wrong + " overlaps wrong (" + String.format("%.1f", wrong * 100.0 / all) + "%) from " + all + ", "
                + notFound + " overlaps not found (" + String.format("%.1f", notFound * 100.0 / goodOverlaps.size()) +"%) from " + goodOverlaps.size());
    }
    */

}
