package ru.ifmo.genetics.distributed.contigsJoining.tasks;

import org.junit.Test;
import ru.ifmo.genetics.distributed.contigsJoining.types.Hole;
import ru.ifmo.genetics.tools.microassembly.types.AlignmentWritable;
import ru.ifmo.genetics.tools.microassembly.types.PairedMaybeAlignedDnaQWritable;

import static org.junit.Assert.*;

import static ru.ifmo.genetics.distributed.contigsJoining.tasks.FillHoles.Reduce.normalizePread;
import static ru.ifmo.genetics.distributed.contigsJoining.tasks.FillHoles.Reduce.preadIsWellOriented;

public class FillHolesTest {
    @Test
    public void testIsWellOriented() {
        Hole hole = new Hole();
        hole.set(0, false, 1, false);

        PairedMaybeAlignedDnaQWritable pread = new PairedMaybeAlignedDnaQWritable();
        pread.first.isAligned = true;
        pread.second.isAligned = true;

        AlignmentWritable align1 = pread.first.alignment;
        AlignmentWritable align2 = pread.second.alignment;


        /*
         * |---leftContig---->   |---rightContig--->
         *     |-firstRest->       <-secondRead-|
         */
        align1.contigId = 0;
        align1.onForwardStrand = true;
        align2.contigId = 1;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

        /*
         * |------------rightContig---------------->
         *     |-firstRest->       <-secondRead-|
         */
        align1.contigId = 1;
        align1.onForwardStrand = true;
        align2.contigId = 1;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

        /*
         * |------------leftContig---------------->
         *     |-firstRest->       <-secondRead-|
         */
        align1.contigId = 0;
        align1.onForwardStrand = true;
        align2.contigId = 0;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

        /*
         * |---leftContig---->   |---otherContig--->
         *     |-firstRest->       <-secondRead-|
         */
        align1.contigId = 0;
        align1.onForwardStrand = true;
        align2.contigId = 2;
        align2.onForwardStrand = true;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

        /*
         * |---leftContig---->
         *     |-firstRest->       <-secondRead-|
         */
        pread.second.isAligned = false;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        pread.second.isAligned = true;

        /*
         * |---otherContig---->   |---rightContig--->
         *     |-firstRest->       <-secondRead-|
         */
        align1.contigId = 3;
        align1.onForwardStrand = false;
        align2.contigId = 1;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();


        /*
         *                     |---rightContig--->
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = false;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        pread.first.isAligned = true;




        hole.set(0, true, 1, false);
        /*
         * <---leftContig----|   |---rightContig--->
         *     |-firstRest->       <-secondRead-|
         */
        align1.contigId = 0;
        align1.onForwardStrand = false;
        align2.contigId = 1;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

        /*
         * |------------rightContig---------------->
         *     |-firstRest->       <-secondRead-|
         */
        align1.contigId = 1;
        align1.onForwardStrand = true;
        align2.contigId = 1;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

        /*
         * <------------leftContig----------------|
         *     |-firstRest->       <-secondRead-|
         */
        align1.contigId = 0;
        align1.onForwardStrand = false;
        align2.contigId = 0;
        align2.onForwardStrand = true;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

        hole.set(0, false, 1, true);
        /*
         * |---leftContig---->   <---rightContig---|
         *     |-firstRest->       <-secondRead-|
         */
        align1.contigId = 0;
        align1.onForwardStrand = true;
        align2.contigId = 1;
        align2.onForwardStrand = true;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

        /*
         * <------------rightContig----------------|
         *     |-firstRest->       <-secondRead-|
         */
        align1.contigId = 1;
        align1.onForwardStrand = false;
        align2.contigId = 1;
        align2.onForwardStrand = true;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

        /*
         * |------------leftContig---------------->
         *     |-firstRest->       <-secondRead-|
         */
        align1.contigId = 0;
        align1.onForwardStrand = true;
        align2.contigId = 0;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

        // Bad pair - both forward and reverse-complemented are not well-oriented

        /*
        * |---leftContig---->   |---rightContig--->
        *     <-firstRest-|       |-secondRead->
        */
        hole.set(0, false, 1, false);
        align1.contigId = 0;
        align1.onForwardStrand = false;
        align2.contigId = 1;
        align2.onForwardStrand = true;
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

        // Shadow zone

        /*
         * |---leftContig---->   |---rightContig--->
         *     |-firstRest->       |-secondRead->
         *
         * rightContig works as otherContig
         */
        hole.set(0, false, 1, false);
        align1.contigId = 0;
        align1.onForwardStrand = true;
        align2.contigId = 1;
        align2.onForwardStrand = true;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

        /*
         * |---leftContig---->   |---rightContig--->
         *     <-firstRest-|       <-secondRead-|
         *
         * leftContig works as otherContig
         */
        hole.set(0, false, 1, false);
        align1.contigId = 0;
        align1.onForwardStrand = false;
        align2.contigId = 1;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        pread.reverseComplement();
        assertFalse(preadIsWellOriented(hole, pread));
        pread.reverseComplement();

    }

    @Test
    public void testNormalizePread() throws Exception {
        Hole hole = new Hole();
        hole.set(0, false, 1, false);

        PairedMaybeAlignedDnaQWritable pread = new PairedMaybeAlignedDnaQWritable();
        pread.first.isAligned = true;
        pread.second.isAligned = true;

        AlignmentWritable align1 = pread.first.alignment;
        AlignmentWritable align2 = pread.second.alignment;


        /*
         * |---leftContig---->   |---rightContig--->
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 0;
        align1.onForwardStrand = true;
        align2.contigId = 1;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(pread.first.isAligned && pread.second.isAligned);

        /*
         * |------------rightContig---------------->
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 1;
        align1.onForwardStrand = true;
        align2.contigId = 1;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(!pread.first.isAligned && pread.second.isAligned);

        /*
         * |------------leftContig---------------->
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 0;
        align1.onForwardStrand = true;
        align2.contigId = 0;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(pread.first.isAligned && !pread.second.isAligned);

        /*
         * |---leftContig---->   |---otherContig--->
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 0;
        align1.onForwardStrand = true;
        align2.contigId = 2;
        align2.onForwardStrand = true;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(pread.first.isAligned && !pread.second.isAligned);

        /*
         * |---leftContig---->
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = true;
        pread.second.isAligned = false;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(pread.first.isAligned && !pread.second.isAligned);

        /*
         * |---otherContig---->   |---rightContig--->
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 3;
        align1.onForwardStrand = false;
        align2.contigId = 1;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(!pread.first.isAligned && pread.second.isAligned);


        /*
         *                     |---rightContig--->
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = false;
        pread.second.isAligned = true;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(!pread.first.isAligned && pread.second.isAligned);



        hole.set(0, true, 1, false);

        /*
         * <---leftContig----|   |---rightContig--->
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 0;
        align1.onForwardStrand = false;
        align2.contigId = 1;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(pread.first.isAligned && pread.second.isAligned);

        /*
         * |------------rightContig---------------->
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 1;
        align1.onForwardStrand = true;
        align2.contigId = 1;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(!pread.first.isAligned && pread.second.isAligned);

        /*
         * <------------leftContig----------------|
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 0;
        align1.onForwardStrand = false;
        align2.contigId = 0;
        align2.onForwardStrand = true;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(pread.first.isAligned && !pread.second.isAligned);

        hole.set(0, false, 1, true);
        /*
         * |---leftContig---->   <---rightContig---|
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 0;
        align1.onForwardStrand = true;
        align2.contigId = 1;
        align2.onForwardStrand = true;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(pread.first.isAligned && pread.second.isAligned);

        /*
         * <------------rightContig----------------|
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 1;
        align1.onForwardStrand = false;
        align2.contigId = 1;
        align2.onForwardStrand = true;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(!pread.first.isAligned && pread.second.isAligned);

        /*
         * |------------leftContig---------------->
         *     |-firstRest->       <-secondRead-|
         */
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 0;
        align1.onForwardStrand = true;
        align2.contigId = 0;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(pread.first.isAligned && !pread.second.isAligned);


        // Shadow zone

        /*
         * |---leftContig---->   |---rightContig--->
         *     |-firstRest->       |-secondRead->
         *
         * rightContig works as otherContig
         */
        hole.set(0, false, 1, false);
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 0;
        align1.onForwardStrand = true;
        align2.contigId = 1;
        align2.onForwardStrand = true;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(pread.first.isAligned && !pread.second.isAligned);

        /*
         * |---leftContig---->   |---rightContig--->
         *     <-firstRest-|       <-secondRead-|
         *
         * leftContig works as otherContig
         */
        hole.set(0, false, 1, false);
        pread.first.isAligned = true;
        pread.second.isAligned = true;
        align1.contigId = 0;
        align1.onForwardStrand = false;
        align2.contigId = 1;
        align2.onForwardStrand = false;
        assertTrue(preadIsWellOriented(hole, pread));
        normalizePread(hole, pread);
        assertTrue(!pread.first.isAligned && pread.second.isAligned);
    }

}
