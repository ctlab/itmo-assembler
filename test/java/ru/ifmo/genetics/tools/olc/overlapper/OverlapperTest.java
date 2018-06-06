package ru.ifmo.genetics.tools.olc.overlapper;

import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.writers.DoubleFastaWriter;
import ru.ifmo.genetics.io.writers.FastaDedicatedWriter;
import ru.ifmo.genetics.io.writers.WritersUtils;
import ru.ifmo.genetics.tools.olc.gluedDnasString.DnaStringGluer;
import ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString;
import ru.ifmo.genetics.tools.olc.overlaps.Overlaps;
import ru.ifmo.genetics.tools.olc.overlaps.OverlapsList;
import ru.ifmo.genetics.tools.olc.suffixArray.BucketsDivider;
import ru.ifmo.genetics.tools.olc.suffixArray.BucketsSorter;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.TestUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.values.PathInValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class OverlapperTest {
    Random r;
    {
        long seed = (long) (Long.MAX_VALUE * Math.random());
        r = new Random(seed);
        System.err.println("Seed = " + seed);
    }


    // good data for checking
    /**
     * Doubled reads
     */
    ArrayList<Dna> reads;
    GluedDnasString fullString;


    // run module
    PathInValue testDir = new PathInValue("testDir");
    PathInValue readsFile = testDir.append("reads.fasta");

    DnaStringGluer gluer = new DnaStringGluer();
    {
        gluer.workDir.set(testDir);
        gluer.readsFile.set(readsFile);
    }
    BucketsDivider divider = new BucketsDivider();
    {
        divider.workDir.set(testDir);
        divider.fullStringFile.set(gluer.fullStringFile);
    }
    BucketsSorter sorter = new BucketsSorter();
    {
        sorter.workDir.set(testDir);
        sorter.fullStringFile.set(gluer.fullStringFile);
        sorter.bucketsDir.set(divider.bucketsDir);
        sorter.bucketCharsNumberIn.set(divider.bucketCharsNumberOut);
    }
    Overlapper overlapper = new Overlapper();
    {
        overlapper.workDir.set(testDir);
        overlapper.fullStringFile.set(gluer.fullStringFile);
        overlapper.sortedBucketsDir.set(sorter.sortedBucketsDir);
        overlapper.bucketCharsNumberIn.set(divider.bucketCharsNumberOut);
        overlapper.bucketsNumberIn.set(divider.bucketsNumberOut);
    }




    @Test
    public void nanoHandTest() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = {"TGC"};

        runAndCheck(stringReads, 2, 0, 10);
    }

    @Test
    public void tinyHandTest() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = {"AA", "AC"};

        runAndCheck(stringReads, 1, 0, 10);
    }

    @Test
    public void tinyHandTest2() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = {"AAA", "A"};

        runAndCheck(stringReads, 1, 0, 10);
    }

    @Test
    public void smallHandTest() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = {"AA", "AC", "AAC", "A", "G", "TGC", "TGCC"};

        runAndCheck(stringReads, 2, 1, 10);
    }

    @Test
    public void handTest() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = {"AAA", "AA", "AC", "AAC", "CCAA", "ATGC", "TG", "TGC", "TGCC", "TGCG", "A"};

        runAndCheck(stringReads, 2, 1, 2);
    }


    @Test
    public void randomTestWithoutErrors1() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = TestUtils.generateReads(r, 10, 5);
        System.err.println("reads = " + Arrays.toString(stringReads));

        runAndCheck(stringReads, 1, 0, 10);
    }

    @Test
    public void randomTestWithoutErrors2() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = TestUtils.generateReads(r, 20, 5);
        System.err.println("reads = " + Arrays.toString(stringReads));

        runAndCheck(stringReads, 1, 0, 10);
    }

    @Test
    public void randomTestWithoutErrors3() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = TestUtils.generateReads(r, 50, 5);

        runAndCheck(stringReads, 1, 0, 10);
    }


    
    @Test
    public void randomTestWithOneError1() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = TestUtils.generateReads(r, 10, 5);
        System.err.println("reads = " + Arrays.toString(stringReads));

        runAndCheck(stringReads, 2, 1, 10);
    }

    @Test
    public void randomTestWithOneError2() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = TestUtils.generateReads(r, 20, 5);
        System.err.println("reads = " + Arrays.toString(stringReads));

        runAndCheck(stringReads, 2, 1, 10);
    }

    @Test
    public void randomTestWithOneError3() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = TestUtils.generateReads(r, 50, 5);

        runAndCheck(stringReads, 2, 1, 10);
    }


    
    @Test
    public void randomTestWithTwoErrors1() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = TestUtils.generateReads(r, 10, 5);
        System.err.println("reads = " + Arrays.toString(stringReads));

        runAndCheck(stringReads, 3, 2, 10);
    }

    @Test
    public void randomTestWithTwoErrors2() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = TestUtils.generateReads(r, 20, 5);

        runAndCheck(stringReads, 3, 2, 10);
    }

    @Test
    public void randomTestWithTwoErrors3() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = TestUtils.generateReads(r, 50, 5);

        runAndCheck(stringReads, 3, 2, 10);
    }


    @Test
    public void randomTests() throws IOException, InterruptedException, ExecutionFailedException {
        for (int mo = 1; mo <= 10; mo += 2) {
            for (int en = 0; en < mo && en <= 2; en++) {
                for (int maxN = 10; maxN <= 100; maxN += 30) {
                    String[] stringReads = TestUtils.generateReads(r, maxN, 10);
                    runAndCheck(stringReads, mo, en, 5);
                }
            }
        }
    }




    void runAndCheck(String[] stringReads, int mo, int en, int ews) throws ExecutionFailedException, IOException, InterruptedException {
        FileUtils.createOrClearDir(testDir);

        reads = TestUtils.convert(stringReads);
        WritersUtils.writeDnasToFastaFile(reads, readsFile.get());
        reads = TestUtils.doubleReads(reads);

        gluer.simpleRun();
        fullString = new GluedDnasString(gluer.fullStringFile.get());

        Overlaps overlapsByOverlapper = findOverlapsByOverlapper(mo, en, ews);
        Overlaps overlapsNaive = findOverlapsNaive(mo, en, ews);

        check(overlapsByOverlapper, overlapsNaive);
    }


    void check(Overlaps overlapsByOverlapper, Overlaps overlapsNaive) throws IOException, InterruptedException, ExecutionFailedException {
        System.err.println("Overlaps found by overlapper = " + overlapsByOverlapper.calculateSize());
        System.err.println("Overlaps found by naive method = " + overlapsNaive.calculateSize());

        for (int i = 0; i < reads.size(); i++) {
            OverlapsList listO = overlapsByOverlapper.getForwardOverlaps(i);
            OverlapsList listN = overlapsNaive.getForwardOverlaps(i);
            
            // check listO in listN
            for (int p = 0; p < listO.size(); p++) {
                int j = listO.getTo(p);
                int cs = listO.getCenterShift(p);
                int bs = overlapsNaive.centerShiftToBeginShift(i, j, cs);
                if (!listN.contains(j, cs))  {
                    assertTrue("Overlap was found by Overlapper, naive method didn't find it:\n\t\ta = '" + reads.get(i) + "', an = " + i +
                        ", b = '" + reads.get(j) + "', bn = " + j + ", center shift = " + cs + ", begin shift = " + bs,
                        listN.contains(j, cs));
                }
            }
            // check listN in listO
            for (int p = 0; p < listN.size(); p++) {
                int j = listN.getTo(p);
                int cs = listN.getCenterShift(p);
                int bs = overlapsNaive.centerShiftToBeginShift(i, j, cs);
                assertTrue("Overlap was found by naive method, Overlapper didn't find it:\n\t\ta = '" + reads.get(i) + "', an = " + i +
                        ", b = '" + reads.get(j) + "', bn = " + j + ", center shift = " + cs + ", begin shift = " + bs,
                        listO.contains(j, cs));
            }
        }
    }


    Overlaps findOverlapsByOverlapper(int mo, int en, int ews) throws ExecutionFailedException, IOException, InterruptedException {
        divider.simpleRun();

        sorter.simpleRun();

//        System.err.println("After sorting:");
//        sas = loadSA(sorter.sortedBucketsDir.get().getFile());
//        System.err.println(sas[0].allToGoodString());
//        System.err.println();

        overlapper.minOverlap.set(mo);
        overlapper.errorsNumber.set(en);
        overlapper.errorsWindowSize.set(ews);

        overlapper.simpleRun();

        Overlaps overlaps = new Overlaps(reads, overlapper.overlapsDir.get().listFiles());
        return overlaps;
    }


    Overlaps findOverlapsNaive(int mo, int en, int ews) {
        Overlaps overlaps = new Overlaps(reads, false);
        
        for (int i = 0; i < reads.size(); i++) {
            Dna a = reads.get(i);
            for (int j = 0; j < reads.size(); j++) {
                Dna b = reads.get(j);
                if (b.length() < mo) {
                    continue;
                }
                for (int shift = 0; a.length() - shift >= mo; shift++) {
                    if ((shift == 0) && (i == j)) {
                        continue;
                    }
                    
                    if (checkOverlap(i, j, shift, en, ews)) {
                        overlaps.addRawOverlapWithSyncUsingBeginShift(i, j, shift);
                    }
                }
            }
        }
        
        return overlaps;
    }

    boolean checkOverlap(int i, int j, int beginShift, int en, int ews) {
        if (beginShift < 0) {
            return checkOverlap(j, i, -beginShift, en, ews);
        }

        Dna a = reads.get(i);
        Dna b = reads.get(j);

        int len = Math.min(a.length() - beginShift, b.length());
        return Overlapper.checkErrorsNumber(a, b, beginShift, 0, len, en, ews);

//        System.err.println("Naive overlapper: found overlap from " + i + "(" + a + ")" + " to " + j + "(" + b + ")" +
//                ", beginShift " + beginShift);
    }


}
