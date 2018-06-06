package ru.ifmo.genetics.tools.olc.layouter;

import org.junit.Before;
import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.writers.DoubleFastaWriter;
import ru.ifmo.genetics.io.writers.FastaDedicatedWriter;
import ru.ifmo.genetics.io.writers.WritersUtils;
import ru.ifmo.genetics.tools.olc.overlaps.Overlaps;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.tool.values.PathInValue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static ru.ifmo.genetics.utils.TestUtils.convert;

public class LayouterTest {


    int readsNumber;
    ArrayList<Dna> reads;

    Overlaps<Dna> overlaps;

    // run module
    PathInValue testDir = new PathInValue("testDir");
    PathInValue readsFile = testDir.append("reads.fasta");
    PathInValue overlapsFile = testDir.append("overlaps");

    Layouter layouter = new Layouter();
    {
        layouter.readsFile.set(readsFile);
        layouter.overlapsFile.set(overlapsFile);
    }



    @Before
    public void defaultSetUp() throws IOException {
        int readsNumber = 30;
        String[] rs = new String[readsNumber];
        for (int i = 0; i < readsNumber; ++i) {
            rs[i] = "";
        }
        setUp(rs);
    }

    public void setUp(String... rawReads) throws IOException {
        FileUtils.createOrClearDirRecursively(testDir);

        readsNumber = rawReads.length * 2;
        reads = new ArrayList<Dna>(readsNumber);
        for (int i = 0; i < rawReads.length; ++i) {
            reads.add(new Dna(rawReads[i]));
            reads.add(reads.get(i * 2).reverseComplement());
        }
        if (rawReads.length != 0 && rawReads[0].length() != 0) {
            WritersUtils.writeDnasToFastaFile(convert(rawReads), readsFile.get());
            layouter.readsNumberParameter.set(-1);
        } else {
            layouter.readsNumberParameter.set(readsNumber);
        }

        overlaps = new Overlaps<Dna>(reads, true);
    }

    void saveOverlaps() throws FileNotFoundException {
        overlaps.printToFile(overlapsFile.get().toString());
    }



    @Test
    public void testRemoveTips() throws InterruptedException, IOException {
        overlaps.addOverlap(0, 2, 1, 0);
        overlaps.addOverlap(2, 4, 1, 0);
        overlaps.addOverlap(4, 6, 1, 0);
        overlaps.addOverlap(6, 0, 1, 0);
        overlaps.addOverlap(0, 8, 1, 0);

        overlaps.addOverlap(0, 10, 1 ,0);
        for (int i = 10; i < readsNumber - 2; i += 2) {
            overlaps.addOverlap(i, i + 2, 1, 0);
        }
        saveOverlaps();

        layouter.load();
        layouter.sortOverlaps();
        layouter.removeTips();

        assertEquals("contains overlap", false, layouter.overlaps.containsOverlap(0, 8, 1));
        assertEquals("overlaps count", overlaps.calculateSize() -2, layouter.overlaps.calculateSize());
    }

    @Test
    /**
     * Simple test. Just two paths of slightly different length.
     * 0 -> 1 -> 2
     * 0 -> 3 -> 4 -> 2
     */
    public void testMergeIndelsPaths() throws InterruptedException, IOException {
        overlaps.addOverlap(0, 2, 10, 0);
        overlaps.addOverlap(2, 4, 20, 0);
        overlaps.addOverlap(0, 6, 8, 0);
        overlaps.addOverlap(6, 8, 11, 0);
        overlaps.addOverlap(8, 4, 13, 0);
        saveOverlaps();

        layouter.load();
        layouter.sortOverlaps();
        layouter.mergePathsWithIndel();

        for (int i = 0; i < 10; i += 2) {
            assertEquals("isReadRemoved(" + i + ")", i == 2, layouter.overlaps.isReadRemoved(i));
        }
    }

    @Test
    /**
     * Test merging three paths between same two vertices
     * 0 -> 1 -> 2
     * 0 -> 3 -> 4 -> 2
     * 0 -> 5 -> 6 -> 7 -> 2
     *
     */
    public void testMergeIndelsPathsThreePaths() throws InterruptedException, IOException {
        overlaps.addOverlap(0, 2, 10, 0);
        overlaps.addOverlap(2, 4, 20, 0);

        overlaps.addOverlap(0, 6, 8, 0);
        overlaps.addOverlap(6, 8, 11, 0);
        overlaps.addOverlap(8, 4, 13, 0);

        overlaps.addOverlap(0, 10, 7, 0);
        overlaps.addOverlap(10, 12, 10, 0);
        overlaps.addOverlap(12, 14, 8, 0);
        overlaps.addOverlap(14, 4, 9, 0);
        saveOverlaps();

        layouter.load();
        layouter.sortOverlaps();
        layouter.mergePathsWithIndel();


        Set<Integer> toRemove = new HashSet<Integer>(Arrays.asList(2, 6, 8));

        for (int i = 0; i < readsNumber; i += 2) {
            assertEquals("isReadRemoved(" + i + ")", toRemove.contains(i), layouter.overlaps.isReadRemoved(i));
        }
    }

    @Test
    /**
     * 0 -> 1 -> 2
     * 0 -> 3 -> 2 -> 5
     * 3 -> 4 -> 5
     *
     */
    public void testMergeIndelsPathsThreePaths2() throws InterruptedException, IOException {
        overlaps.addOverlap(0, 2, 10, 0);
        overlaps.addOverlap(2, 4, 10, 0);

        overlaps.addOverlap(0, 6, 8, 0);
        overlaps.addOverlap(6, 4, 10, 0);
        overlaps.addOverlap(4, 10, 10, 0);

        overlaps.addOverlap(6, 8, 8, 0);
        overlaps.addOverlap(8, 10, 10, 0);
        saveOverlaps();

        layouter.load();
        layouter.sortOverlaps();
        layouter.mergePathsWithIndel();


        Set<Integer> toRemove = new HashSet<Integer>(Arrays.asList(2, 8));

        for (int i = 0; i < readsNumber; i += 2) {
            assertEquals("isReadRemoved(" + i + ")", toRemove.contains(i), layouter.overlaps.isReadRemoved(i));
        }
    }

    @Test
    /**
     * 0 -> 1 -> 1rc -> 0rc
     * 0 -> 2 -> 3 -> 3rc -> 2rc -> 0
     *
     */
    public void testMergeIndelsPathsRC() throws InterruptedException, IOException {
        overlaps.addOverlap(0, 2, 10, 0);
        overlaps.addOverlap(2, 3, 10, 0);

        overlaps.addOverlap(0, 4, 6, 0);
        overlaps.addOverlap(4, 6, 6, 0);
        overlaps.addOverlap(6, 7, 8, 0);
        saveOverlaps();

        layouter.load();
        layouter.sortOverlaps();
        layouter.mergePathsWithIndel();

        Set<Integer> toRemove = new HashSet<Integer>(Arrays.asList(2));

        for (int i = 0; i < readsNumber; i += 2) {
            assertEquals("isReadRemoved(" + i + ")", toRemove.contains(i), layouter.overlaps.isReadRemoved(i));
        }
    }

    @Test
    /**
     * 0 -> 1 -> 2
     * 0 -> 2rc -> 3 -> 3rc -> 2 -> 0rc
     * 2rc -> 1rc -> 0rc
     *
     */
    public void testMergeIndelsPathsRC2() throws InterruptedException, IOException {
        overlaps.addOverlap(0, 2, 10, 0);
        overlaps.addOverlap(2, 4, 10, 0);

        overlaps.addOverlap(0, 5, 2, 0);
        overlaps.addOverlap(5, 6, 6, 0);
        overlaps.addOverlap(6, 7, 4, 0);
        saveOverlaps();

        layouter.load();
        layouter.sortOverlaps();
        layouter.mergePathsWithIndel();

        Set<Integer> toRemove = new HashSet<Integer>(Arrays.asList(2));

        for (int i = 0; i < readsNumber; i += 2) {
            assertEquals("isReadRemoved(" + i + ")", toRemove.contains(i), layouter.overlaps.isReadRemoved(i));
        }
    }

    @Test
    /**
     * 0 -> 1 -> 0
     */
    public void testMergeIndelsPathsTrivial() throws InterruptedException, IOException {
        overlaps.addOverlap(0, 2, 1, 0);
        overlaps.addOverlap(2, 0, 3, 0);
        overlaps.addOverlap(0, 4, 10, 0);
        saveOverlaps();

        layouter.load();
        layouter.sortOverlaps();
        layouter.mergePathsWithIndel();

        for (int i = 0; i < readsNumber; i += 2) {
            assertEquals("isReadRemoved(" + i + ")", false, layouter.overlaps.isReadRemoved(i));
        }
    }

    @Test
    /**
     * 0 -> 1 -> 2
     * 0 -> 3 -> 2
     *
     */
    public void testMergePathsDifferent() throws InterruptedException, IOException {
        setUp(
                "AAAACTAC", "CTACGTAGTCGGGTTG",
                "GTTGCCCC", "CTACGGTCTGCGGTTG");

        overlaps.addOverlap(0, 2, 16, 0);
        overlaps.addOverlap(2, 4, 16, 0);

        overlaps.addOverlap(0, 6, 16, 0);
        overlaps.addOverlap(6, 4, 16, 0);
        saveOverlaps();

        layouter.load();
        layouter.sortOverlaps();
        layouter.mergeGraph();

        assertEquals(0, layouter.merges);
    }

    @Test
    /**
     * 0 -> 1 -> 2
     * 0 -> 3 -> 2
     *
     */
    public void testMergePathsAlmostEqual() throws InterruptedException, IOException {
        setUp(
                "AAAACTAC",
                    "CTACGGGTTG",
                          "GTTGCCCC",
                    "CTACCGGTTG");
        overlaps.addOverlap(0, 2, 10, 0);
        overlaps.addOverlap(2, 4, 10, 0);

        overlaps.addOverlap(0, 6, 10, 0);
        overlaps.addOverlap(6, 4, 10, 0);
        saveOverlaps();

        layouter.load();
        layouter.sortOverlaps();
        layouter.mergeGraph();

        assertEquals(1, layouter.merges);
    }


    @Test
    /**
     * 0 -> 1 -> 2 -> 3
     * 0 -> 4 -> 5 -> 3
     */
    public void testMergePathsWithCoveredReads() throws InterruptedException, IOException {
        setUp(
                "AAAACTAC",
                "CTACGGGTTG", "CCCGAG",
                "GTTGCCCCGAGCTAGAAT",
                "CTACGGGTTG", "CGCGAG");
        /**
         *      CTACCGGTTG CCCGAG
         *  AAAACTAC  GTTGCCCCGAGCTAGAAT
         *      CTACCGGTTG CGCGAG
         */
        overlaps.addOverlap(0, 2, 10, 0);
        overlaps.addOverlap(2, 4, 18, 0);
        overlaps.addOverlap(4, 6, 2, 0);

        overlaps.addOverlap(0, 8, 10, 0);
        overlaps.addOverlap(8, 10, 18, 0);
        overlaps.addOverlap(10, 6, 2, 0);
        saveOverlaps();

        layouter.load();
        layouter.sortOverlaps();
        layouter.mergeGraph();

        assertEquals(1, layouter.merges);
    }

    @Test
    /**
     * 0 -> 1 -> 2 -> 3
     * 0 -> 4 -> 5 -> 3
     */
    public void testMergePathsWithCoveredReads2() throws InterruptedException, IOException {
        setUp(
                "AAAACTAC",
                "TTAAAACTACCGGTTG", "CCCGAG",
                "GTTGCCCCGAGCTAGAAT",
                "TTAAAACTACCGGTTG", "CGCGAG");
        /**
         * TTAAAACTACCGGTTG CCCGAG
         *   AAAACTAC  GTTGCCCCGAGCTAGAAT
         * TTAAAACTACCGGTTG CGCGAG
         */
        overlaps.addOverlap(0, 2, 4, 0);
        overlaps.addOverlap(2, 4, 24, 0);
        overlaps.addOverlap(4, 6, 2, 0);

        overlaps.addOverlap(0, 8, 4, 0);
        overlaps.addOverlap(8, 10, 24, 0);
        overlaps.addOverlap(10, 6, 2, 0);
        saveOverlaps();

        layouter.load();
        layouter.sortOverlaps();
        layouter.mergeGraph();

        assertEquals(1, layouter.merges);
    }
}
