package ru.ifmo.genetics.tools.olc.suffixArray;

import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.writers.DoubleFastaWriter;
import ru.ifmo.genetics.io.writers.FastaDedicatedWriter;
import ru.ifmo.genetics.io.writers.WritersUtils;
import ru.ifmo.genetics.tools.olc.gluedDnasString.DnaStringGluer;
import ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.TestUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.values.PathInValue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class SABuilderTest {
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



    @Test
    public void smallHandTest() throws ExecutionFailedException, IOException {
        String[] reads = {"AGA", "AC"};

        runAndCheck(reads, true);
    }

    @Test
    public void handTest() throws IOException, ExecutionFailedException {
        String[] reads = {"ACA", "ACAA", "AC", "ACACAC", "CAC", "AA", "AAACCA", "CAACAAC"};

        runAndCheck(reads, true);
    }

    @Test
    public void randomTests() throws IOException, ExecutionFailedException {
        for (int i = 0; i < 100; ++i) {
            String[] reads = TestUtils.generateReads(r, 100, 100);

            runAndCheck(reads, false);
        }
    }



    void runAndCheck(String[] stringReads, boolean debugOutput) throws ExecutionFailedException, IOException {
        FileUtils.createOrClearDir(testDir);

        reads = TestUtils.convert(stringReads);
        WritersUtils.writeDnasToFastaFile(reads, readsFile.get());
        reads = TestUtils.doubleReads(reads);

        gluer.simpleRun();
        fullString = new GluedDnasString(gluer.fullStringFile.get());


        divider.simpleRun();

        if (debugOutput) {
            System.err.println("After dividing:");
            SuffixArray[] sas = loadSA(divider.bucketsDir.get());
            System.err.println(sas[0].allToGoodString());
            System.err.println();
        }

        sorter.simpleRun();

        if (debugOutput) {
            System.err.println("After sorting:");
            SuffixArray[] sas = loadSA(sorter.sortedBucketsDir.get());
            System.err.println(sas[0].allToGoodString());
            System.err.println();
        }

        SuffixArray[] sas = loadSA(sorter.sortedBucketsDir.get());
        checkSA(sas);
    }


    SuffixArray[] loadSA(File dir) throws IOException {
        SuffixArray[] sas = new SuffixArray[divider.bucketsNumberOut.get()];
        for (int i = 0; i < divider.bucketsNumberOut.get(); i++) {
            SuffixArray sa = BucketsSorter.loadSuffixArrayBucket(fullString, dir, i, divider.bucketCharsNumberOut.get());
            sas[i] = sa;
        }
        return sas;
    }

    void checkSA(SuffixArray sa) {
        for (int i = 0; i + 1 < sa.length; i++) {
            int x = (int) sa.get(i);
            int y = (int) sa.get(i + 1);
            assertTrue(BucketsSorter.compareTill$(sa.text, x, y) <= 0);
        }
    }
    void checkSA(SuffixArray[] sas) {
        for (SuffixArray sa : sas) {
            checkSA(sa);
        }

        SuffixArray lastSA = null;
        for (SuffixArray sa : sas) {
            if (sa.length > 0) {
                if (lastSA != null) {
                    int x = (int) lastSA.get(lastSA.length - 1);
                    int y = (int) sa.get(0);
                    assertTrue(BucketsSorter.compareTill$(sa.text, x, y) <= 0);
                }
                lastSA = sa;
            }
        }
    }

}