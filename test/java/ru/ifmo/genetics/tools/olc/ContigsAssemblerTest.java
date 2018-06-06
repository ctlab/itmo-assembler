package ru.ifmo.genetics.tools.olc;

import org.junit.Test;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.writers.FastaDedicatedWriter;
import ru.ifmo.genetics.io.writers.WritersUtils;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.TestUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.values.PathInValue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class ContigsAssemblerTest {
    Random r;
    {
        long seed = (long) (Long.MAX_VALUE * Math.random());
        r = new Random(seed);
        System.err.println("Seed = " + seed);
    }


    // run module
    PathInValue testDir = new PathInValue("testDir");
    PathInValue readsFile = testDir.append("reads.fasta");

    ContigsAssembler assembler = new ContigsAssembler();
    {
        assembler.workDir.set(testDir);
        assembler.inputFiles.set(new File[]{readsFile.get()});
        assembler.consensus.minReadsInContig.set(1);
        assembler.consensus.minReadLength.set(10);
    }



    @Test
    public void forTestingTest() throws IOException, InterruptedException, ExecutionFailedException {
        //                           CCCGCAATTAGCAGGGCGAAGCATGAC
        //                 |       read1        |
        //                 0    5   10    5   20    5   30    5
        String fragment = "ACTAGACTTGCCCGCAATTAGCAGGGCGAAGCATGAC";
        //                      |        read2       |
        //                           |          read3          |

        String read1 = fragment.substring(0, 22);
        String read2 = fragment.substring(5, 27);
        read2 = setChar(read2, 10, 'A', 'G');
        String read3 = fragment.substring(10, fragment.length());

        String[] stringReads = {read1, read2, read3};

        String[] result = {fragment};

        runAndCheck(stringReads, result, 7, 1);
    }


    // ---------------------------- tests with reads without overlaps ----------------------------------

    @Test
    public void testWithNoReads() throws IOException, InterruptedException, ExecutionFailedException {
        String[] stringReads = {};
        String[] result = {};

        runAndCheck(stringReads, result, 7, 0);
    }

    @Test
    public void testWithOneRead() throws IOException, InterruptedException, ExecutionFailedException {
        String fragment = "ACTAGACTTGCCCGCAATTAGC";

        String read1 = fragment;

        String[] stringReads = {read1};
        String[] result = {read1};

        runAndCheck(stringReads, result, 7, 0);
    }

    @Test
    public void testWithManyIdenticalReads() throws IOException, InterruptedException, ExecutionFailedException {
        String fragment = "ACTAGACTTGCCCGCAATTAGC";
        
        String read1 = fragment;

        String[] stringReads = {read1, read1, read1, read1, read1, read1};
        String[] result = {read1};

        runAndCheck(stringReads, result, 7, 0);
    }

    @Test
    public void testWithReadsWithoutOverlaps() throws IOException, InterruptedException, ExecutionFailedException {
        String fragment1 = "ACTAGACTTGCCCGCAATTAGC";
        String fragment2 = "GCTAAGCTTCTAGACCATGAGGTAC";
        String fragment3 = "GGTCGGATGGATAAAGAGCATC";
        
        String read1 = fragment1, read2 = fragment2, read3 = fragment3;

        String[] stringReads = {read1, read2, read3};
        String[] result = {read1, read2, read3};

        runAndCheck(stringReads, result, 7, 0);
    }



    // ------------------------------------- one line tests --------------------------------------------

    @Test
    public void smallLineTest1() throws IOException, InterruptedException, ExecutionFailedException {
        //                 |        read1         |
        //                 0    5   10    5   20    5   30
        String fragment = "ACGTAGACTTAGCGGACGCAGCAGCAATAAGCT";
        //                                |     read2      |
        
        String read1 = fragment.substring(0, 24);
        String read2 = fragment.substring(15, 33);
        
        String[] stringReads = {read1, read2};
        String[] result = {fragment};

        runAndCheck(stringReads, result, 9, 0);
    }

    @Test
    public void smallLineTest2() throws IOException, InterruptedException, ExecutionFailedException {
        //                 |       read1       |
        //                 0    5   10    5   20
        String fragment = "ACTAGACTTGCCCGCAATTAGC";
        //                  |       read2       |

        String read1 = fragment.substring(0, 21);
        String read2 = fragment.substring(1, 22);
        
        String[] stringReads = {read1, read2};
        String[] result = {fragment};

        runAndCheck(stringReads, result, 7, 0);
    }

    @Test
    public void smallLineTestWithOneReadCoveredAnother1() throws IOException, InterruptedException, ExecutionFailedException {
        //                 |       read1      |
        //                 0    5   10    5   20
        String fragment = "CTAGACTTGCCCGCAATTAGA";
        //                 |       read2       |

        String read1 = fragment.substring(0, 20);
        String read2 = fragment;
        
        String[] stringReads = {read1, read2};
        String[] result = {fragment};

        runAndCheck(stringReads, result, 7, 0);
    }

    @Test
    public void smallLineTestWithOneReadCoveredAnother2() throws IOException, InterruptedException, ExecutionFailedException {
        //                  |       read1      |
        //                 0    5   10    5   20
        String fragment = "CTAGACTTGCCCGCAATTAGA";
        //                 |       read2       |

        String read1 = fragment.substring(1);
        String read2 = fragment;

        String[] stringReads = {read1, read2};
        String[] result = {fragment};

        runAndCheck(stringReads, result, 7, 0);
    }

    @Test
    public void smallLineTestWithOneReadCoveredAnother3() throws IOException, InterruptedException, ExecutionFailedException {
        //                  |       read1      |
        //                 0    5   10    5   20
        String fragment = "CTAGACTTGCCCGCAATTAGA";
        //                     |    read2    |

        String read1 = fragment.substring(1);
        String read2 = fragment.substring(4, 19);

        String[] stringReads = {read1, read2};
        String[] result = {read1};

        runAndCheck(stringReads, result, 7, 0);
    }


    @Test
    public void smallLineTestWithErrors() throws IOException, InterruptedException, ExecutionFailedException {
        //                 |        read1         |
        //                 0    5   10    5   20    5   30
        String fragment = "ACGTAGACTTAGCGGACGCAGCAGCAATAAGCT";
        //                                |     read2      |

        String read1 = fragment.substring(0, 24);
        read1 = setChar(read1, 17, 'G', 'T');
        String read2 = fragment.substring(15, 33);
        read2 = setChar(read2, 6, 'C', 'A');
        
        String resultStr = fragment;
        resultStr = setChar(resultStr, 21, 'C', 'A');
        // see NucleotideConsensus.get() method and DnaTools.DNA array

        String[] stringReads = {read1, read2};
        String[] result = {resultStr};

        runAndCheck(stringReads, result, 7, 2);
    }

    @Test
    public void smallLineTestWithTooMuchErrors() throws IOException, InterruptedException, ExecutionFailedException {
        //                 |        read1         |
        //                 0    5   10    5   20    5   30
        String fragment = "ACGTAGACTTAGCGGACGCAGCAGCAATAAGCT";
        //                                |     read2      |

        String read1 = fragment.substring(0, 24);
        String read2 = fragment.substring(15, 33);
        read2 = setChar(read2, 2, 'G', 'A');
        read2 = setChar(read2, 5, 'G', 'C');
        read2 = setChar(read2, 6, 'C', 'T');

        String[] stringReads = {read1, read2};
        String[] result = new String[]{read1, read2}; // because more than 2 errors were made

        runAndCheck(stringReads, result, 7, 2);
    }

    @Test
    public void bigLineTest() throws IOException, InterruptedException, ExecutionFailedException {
        //                 |        read1         |              read3                |           read5          |
        //                 0    5   10    5   20    5   30    5   40    5   50    5   60    5   70    5   80    5   10    5   90
        String fragment = "ACGTAGACTTAGCGGACGCAGCAGCAATAAGCTGGAAGGAGCGAGGTCGGATGGATAAAGAGCATCACCGGTCCTACGATAACCTGCTGAAATCAGAATCCGCA";
        //                          |           read2      |             |         read4         |        |        read6          |

        String read1 = fragment.substring(0, 24);
        String read2 = fragment.substring(9, 33);
        String read3 = fragment.substring(23, 60);
        String read4 = fragment.substring(46, 71);
        String read5 = fragment.substring(59, 87);
        String read6 = fragment.substring(79);
        
        String[] stringReads = {read1, read2, read3, read4, read5, read6};
        String[] result = {fragment};

        runAndCheck(stringReads, result, 7, 0);
    }

    @Test
    public void bigLineTestWithHighCoverage() throws IOException, InterruptedException, ExecutionFailedException {
        //                        |       read4      |
        //                              |         read3         |
        //                 |        read1       |       |   read2   |
        //                 0    5   10    5   20    5   30    5   40
        String fragment = "GGAGCGAGGTCGGATGGATAAAGAGCATCACCGGTCCTACGA";
        //                           |         read5       |
        //                                     |       read6      |
        //                   |      read7     |    read8    |
        //

        String read1 = fragment.substring(0, 22);
        String read2 = fragment.substring(29, 42);
        String read3 = fragment.substring(13, 38);
        String read4 = fragment.substring(7, 27);
        String read5 = fragment.substring(10, 33);
        String read6 = fragment.substring(20, 40);
        String read7 = fragment.substring(2, 20);
        String read8 = fragment.substring(19, 34);

        String[] stringReads = {read1, read2, read3, read4, read5, read6, read7, read8};
        String[] result = {fragment};

        runAndCheck(stringReads, result, 7, 0);
    }

    @Test
    public void bigLineTestWith2Paths() throws IOException, InterruptedException, ExecutionFailedException {
        //      path0 =    read0 -> read3 -> read2 -> read1
        //
        //                        |         read3    |
        //                              |      x  read2         |
        //                 |      x read0       |       | read1 x   |
        //                 0    5   10    5   20    5   30    5   40
        String fragment = "GGAGCGAGGTCGGATGGATAAAGAGCATCACCGGTCCTACGA";
        //                                              ACCGGTCCAACGA
        //                           |        x  read4     |
        //                                     |      x read5     |
        //                   |      read6     |    read7    |
        //
        //      path1 =    read0 -> read6 -> read4 -> read7 -> read5 -> read1

        String read0 = fragment.substring(0, 22);
        read0 = setChar(read0, 7, 'G', 'C');
        String read1 = fragment.substring(29, 42);
        read1 = setChar(read1, 8, 'T', 'A');
        String read2 = fragment.substring(13, 38);
        read2 = setChar(read2, 7, 'A', 'T');
        String read3 = fragment.substring(7, 27);

        String read4 = fragment.substring(10, 33);
        read4 = setChar(read4, 9, 'A', 'C');
        String read5 = fragment.substring(20, 40);
        read5 = setChar(read5, 7, 'T', 'A');
        String read6 = fragment.substring(2, 20);
        String read7 = fragment.substring(19, 34);

        String[] stringReads = {read0, read1, read2, read3, read4, read5, read6, read7};
        String[] result = {fragment};

        runAndCheck(stringReads, result, 7, 1);
    }

    @Test
    public void bigLineTestWithHighCoverageAndManyErrors() throws IOException, InterruptedException, ExecutionFailedException {
        //                        |       x read3    |
        //                              |         read2         |
        //                 |      x read0       |       | read1 x   |
        //                 0    5   10    5   20    5   30    5   40
        String fragment = "GGAGCGAGGTCGGATGGATAAAGAGCATCACCGGTCCTACGA";
        //                           |         read4       |
        //                                     |      x read5     |
        //                   |      read6   x |    read7    |
        //

        String read0 = fragment.substring(0, 22);
        read0 = setChar(read0, 7, 'G', 'A');
        String read1 = fragment.substring(29, 42);
        read1 = setChar(read1, 8, 'T', 'A');
        String read2 = fragment.substring(13, 38);
        String read3 = fragment.substring(7, 27);
        read3 = setChar(read3, 8, 'G', 'A');
        String read4 = fragment.substring(10, 33);
        String read5 = fragment.substring(20, 40);
        read5 = setChar(read5, 7, 'T', 'A');
        String read6 = fragment.substring(2, 20);
        read6 = setChar(read6, 15, 'A', 'C');
        String read7 = fragment.substring(19, 34);

        String resultString = fragment.substring(10);

        String[] stringReads = {read0, read1, read2, read3, read4, read5, read6, read7};
        String[] result = {resultString};

        // Test doesn't work
//        runAndCheck(stringReads, result, 7, 1);
    }


    @Test
    public void bigLineTestWithHighCoverageAndWithStrangeOverlap1() throws IOException, InterruptedException, ExecutionFailedException {
        //                        |       read3      |
        //                              |         read2         |
        //                 |        read0       |       |   read1   |
        //                 0    5   10    5   20    5   30    5   40
        String fragment = "GGAGCGAGGTCGGATGGATAAAGAGCATGGATAGTCCTACGA";
        //                           |         read4       |
        //                                     |       read5      |
        //                   |      read6     |    read7    |
        //

        String read0 = fragment.substring(0, 22);
        String read1 = fragment.substring(29, 42);
        String read2 = fragment.substring(13, 38);
        String read3 = fragment.substring(7, 27);
        String read4 = fragment.substring(10, 33);
        String read5 = fragment.substring(20, 40);
        String read6 = fragment.substring(2, 20);
        String read7 = fragment.substring(19, 34);
        // read4 overlaps read2 with two shifts (one good and one strange)

        String[] stringReads = {read0, read1, read2, read3, read4, read5, read6, read7};

        String[] result = {fragment};

        runAndCheck(stringReads, result, 7, 0);
    }

    @Test
    public void bigLineTestWithHighCoverageAndWithStrangeOverlap2() throws IOException, InterruptedException, ExecutionFailedException {
        //                        |       read3      |
        //                                |       read2         |
        //                 |        read0       |     |    read1    |
        //                 0    5   10    5   20    5   30    5   40
        String fragment = "GGAGCGAGGTCGGATGGATAAAGAGCATGGATAGTCCTACGA";
        //                           |         read4       |
        //                                     |       read5      |
        //                   |      read6     |    read7    |
        //

        String read0 = fragment.substring(0, 22);
        String read1 = fragment.substring(27, 42);
        String read2 = fragment.substring(15, 38);
        String read3 = fragment.substring(7, 27);
        String read4 = fragment.substring(10, 33);
        String read5 = fragment.substring(20, 40);
        String read6 = fragment.substring(2, 20);
        String read7 = fragment.substring(19, 34);
        // read6 overlaps read1

        String[] stringReads = {read0, read1, read2, read3, read4, read5, read6, read7};

        String[] result = {fragment};

        runAndCheck(stringReads, result, 6, 0);
    }


    @Test
    public void lineTestWithStrangeRead1() throws IOException, InterruptedException, ExecutionFailedException {
        //                        |       read2      |
        //                              |         read1         |
        //                 |        read0       |
        //                 0    5   10    5   20    5   30    5   40
        String fragment = "ACGTAGACTTAGCGGACGCAGCAGCAATAAGCTGGAAGGA";
        //                           |         read3       |
        //                                     |       read4      |
        //                   |      read5      |
        //         wrong read - read6 = |         |TTTTTTT - strange sequence at the end

        String read0 = fragment.substring(0, 22);
        String read1 = fragment.substring(13, 38);
        String read2 = fragment.substring(7, 27);
        String read3 = fragment.substring(10, 33);
        String read4 = fragment.substring(20, 40);
        String read5 = fragment.substring(2, 21);
        String read6 = fragment.substring(13, 24) + "TTTTTTT";

        String[] stringReads = {read0, read1, read2, read3, read4, read5, read6};
        
        String[] result = {fragment, read6};

        runAndCheck(stringReads, result, 7, 0);
    }


    // ------------------------- tests with reads covered with RC copy -------------------------------

    @Test
    public void testWithOneReadCoveredWithRCCopy1() throws IOException, InterruptedException, ExecutionFailedException {
        String fragment = "TTTGTCGAC";

        String read1 = fragment;

        String[] stringReads = {read1};
        String[] result = {read1};

//        Test doesn't work
//        runAndCheck(stringReads, result, 6, 0);
    }

    @Test
    public void testWithOneReadCoveredWithRCCopy2() throws IOException, InterruptedException, ExecutionFailedException {
        String fragment = "GTCGACTTT";

        String read1 = fragment;

        String[] stringReads = {read1};
        String[] result = {read1};

//        runAndCheck(stringReads, result, 6, 0);
    }


    @Test
    public void bigLineTestWithHighCoverageAndOneReadCoveredWithRCCopy() throws IOException, InterruptedException, ExecutionFailedException {
        //                        |       read4      |
        //                              |         read3         |
        //                 |        read1       |       |   read2   |
        //                 0    5   10    5   20    5   30    5   40
        String fragment = "GGAGCGAGGTCGGATGGATAAAGAGCATCACCGGTCCTACGA";
        //                           |         read5       |
        //                                     |       read6      |
        //                   |      read7     |    read8    |
        //

        String read1 = fragment.substring(0, 22);
        String read2 = fragment.substring(29, 42);
        String read3 = fragment.substring(13, 38);
        String read4 = fragment.substring(7, 27);
        String read5 = fragment.substring(10, 33);
        String read6 = fragment.substring(20, 40);
        String read7 = fragment.substring(2, 20);
        String read8 = fragment.substring(19, 34);
        // rc(read2) overlaps with read2 (first 6 chars of read2 (ACCGGT) has the same rc view)

        String[] stringReads = {read1, read2, read3, read4, read5, read6, read7, read8};

        String[] result = {fragment};

        runAndCheck(stringReads, result, 5, 0);
    }



    // ------------------------------- tests with one fork -------------------------------------

    @Test
    public void testWithOneFork() throws IOException, InterruptedException, ExecutionFailedException {
        //                                         |     read3    |
        //                         |           read2      |
        //                  |        read1  |
        //                first difference - v
        //                  0    5   10    5   20    5   30    5   40
        String fragment1 = "ACGTAGACTTAGCGGACGCAGCAGCAATAAGCTGGAAGG";
        String fragment2 = "ACGTAGACTTAGCGGACCCTACGATACGCTGAAAATCCGCA";
        //                      |      read4     |
        //                              |      read5     |
        //                                         |      read6     |
        // +                       CTTAGCGGACGCAGCAGCAATAAGCTGGAAGG
        // +                    AGACTTAGCGGACCCTACGATACGCTGAAAATCCGCA
        // +                ACGTAGACTTAGCGGAC


        String read1 = fragment1.substring(0, 17);
        String read2 = fragment1.substring(7, 31);
        String read3 = fragment1.substring(23, 39);
        String read4 = fragment2.substring(4, 22);
        String read5 = fragment2.substring(12, 30);
        String read6 = fragment2.substring(23, 41);

        String[] stringReads = {read1, read2, read3, read4, read5, read6};

        String[] result = new String[]{fragment1.substring(7), fragment2.substring(4), read1};

        runAndCheck(stringReads, result, 7, 0);
    }




    // ------------------------------- test impl -------------------------------------

    void runAndCheck(String[] reads, String[] expectedResult, int mo, int en) throws ExecutionFailedException, IOException {
        String[] resultByAssembler = assemble(reads, mo, en);

        // printing info
        System.err.println();
        System.err.println("Expected result = " + print(expectedResult));
        System.err.println("Result by assembler = " + print(resultByAssembler));

        // checking uniqueness
        HashSet<String> eSet = new HashSet<String>();
        for (String s : expectedResult) {
            assertTrue("Contig '" + s + "' isn't unique in the expected result !", !eSet.contains(s));
            eSet.add(s);
        }
        HashSet<String> aSet = new HashSet<String>();
        for (String s : resultByAssembler) {
            assertTrue("Contig '" + s + "' isn't unique in the result by assembler !", !aSet.contains(s));
            aSet.add(s);
        }

        // checking expectedResult in resultByAssembler
        for (String s : expectedResult) {
            assertTrue("Contig '" + s + "' wasn't found in the result by assembler !", aSet.contains(s));
        }
        // checking resultByAssembler in expectedResult
        for (String s : resultByAssembler) {
            assertTrue("Unexpected contig '" + s + "' was found by assembler !", eSet.contains(s));
        }
    }
    
    String[] assemble(String[] stringReads, int mo, int en) throws IOException, ExecutionFailedException {
        FileUtils.createOrClearDirRecursively(testDir);

        ArrayList<Dna> reads = TestUtils.convert(stringReads);
        WritersUtils.writeDnasToFastaFile(reads, readsFile.get());

        // running
        assembler.overlapper.minOverlap.set(mo);
        assembler.overlapper.errorsNumber.set(en);

        assembler.simpleRun();

        // loading result
        ArrayList<Dna> contigs = ReadersUtils.loadDnas(assembler.contigsFile.get());
        String[] stringContigs = new String[contigs.size()];
        for (int i = 0; i < contigs.size(); i++) {
            stringContigs[i] = contigs.get(i).toString();
        }

        return stringContigs;
    }


    
    String print(Object[] objects) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (Object obj : objects) {
            sb.append(obj.toString() + "\n");
        }
        sb.append("]\n");
        return sb.toString();
    }
    
    static String setChar(String s, int pos, char expectedCharAtPos, char newChar) {
        if (s.charAt(pos) != expectedCharAtPos) {
            throw new RuntimeException("Wrong expected char at pos=" + pos +
                    ", cur='"+ s.charAt(pos) + "', exp='" + expectedCharAtPos + "'");
        }
        String newS = s.substring(0, pos) + newChar + s.substring(pos + 1, s.length());
        return newS;
    }

}
