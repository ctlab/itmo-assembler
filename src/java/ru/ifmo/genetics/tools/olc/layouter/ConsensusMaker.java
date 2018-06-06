package ru.ifmo.genetics.tools.olc.layouter;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.LineReader;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaView;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.io.CommentableSink;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.readers.ReaderInSmallMemory;
import ru.ifmo.genetics.io.writers.FastaDedicatedWriter;
import ru.ifmo.genetics.tools.microassembly.FilledHole;
import ru.ifmo.genetics.utils.pairs.MutablePair;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static ru.ifmo.genetics.utils.NumUtils.groupDigits;

public class ConsensusMaker extends Tool {
    public static final String NAME = "consensus-maker";
    public static final String DESCRIPTION = "makes consensus for contigs";


    // input parameters
    public final Parameter<File> readsFile = addParameter(new FileParameterBuilder("reads-file")
            .mandatory()
            .withDescription("file with all reads")
            .create());

    public final Parameter<File> layoutFile = addParameter(new FileParameterBuilder("layout-file")
            .mandatory()
            .withDescription("layout file")
            .create());

    public final Parameter<File> contigsFile = addParameter(new FileParameterBuilder("contigs-file")
            .optional()
            .withDefaultValue(workDir.append("contigs.fasta"))
            .withDescription("file with resulting contigs")
            .create());

    public final Parameter<Integer> minReadsInContig = addParameter(new IntParameterBuilder("min-reads-in-contig")
            .optional()
            .withDefaultValue(2)
            .withDescription("minimal reads in contig")
            .create());

    public final Parameter<Integer> minReadLength = addParameter(new IntParameterBuilder("min-read-length")
            .optional()
            .withDefaultValue(200)
            .withDescription("minimal read length to output")
            .create());

    public final Parameter<File> holesFile = addParameter(new FileParameterBuilder("holes-file")
            .optional()
            .withDescription("file with holes")
            .create());


    // internal variables
    private int readsNumber;
    private ArrayList<Dna> reads;


    @Override
    protected void runImpl() throws IOException, ExecutionFailedException {
        loadReads();
        makeConsensus();
    }

    private void loadReads() throws IOException {
        info("Loading reads...");
        reads = ReadersUtils.loadDnasAndAddRC(readsFile.get());
        readsNumber = reads.size();
    }

    private static class ShiftedFiller {
        int beginContig;
        int shift;
        LightDna sequence;

        private ShiftedFiller(int beginContig, int shift, LightDna sequence) {
            this.beginContig = beginContig;
            this.shift = shift;
            this.sequence = sequence;
        }
    }

    private void makeConsensus() throws IOException, ExecutionFailedException {
        info("Making consensus...");

        Map<Integer, ArrayList<ShiftedFiller>> fillersMap = new HashMap<Integer, ArrayList<ShiftedFiller>>();
        for (int i = 0; i < readsNumber; ++i) {
            fillersMap.put(i, new ArrayList<ShiftedFiller>());
        }

        LightDna[] prependix = new LightDna[readsNumber];
        LightDna[] appendix = new LightDna[readsNumber];


        for (int i = 0; i < readsNumber; ++i) {
            appendix[i] = prependix[i] = Dna.emptyDna;
        }


        if (holesFile.get() != null) {
            LineReader holesReader = new LineReader(new BufferedInputStream(new FileInputStream(holesFile.get())));
            Text line = new Text();
            while (holesReader.readLine(line) != 0) {
                String s = line.toString();
                FilledHole fh = new FilledHole(s);

                if (fh.filler.sequence.length() == 0) {
                    continue;
                }


                int iFrom = fh.hole.leftContigId * 2 + (fh.hole.leftComplemented ? 1 : 0);
                int iTo = fh.hole.rightContigId * 2 + (fh.hole.rightComplemented ? 1 : 0);

                if (fh.hole.isOpen()) {
                    appendix[iFrom] = fh.filler.sequence;
                    prependix[iFrom ^ 1] = DnaView.rcView(fh.filler.sequence);
                    continue;
                }

                int shift = reads.get(iFrom).length() + fh.filler.distance;
                int rcShift = reads.get(iTo).length() + fh.filler.distance;

                fillersMap.get(iTo).add(new ShiftedFiller(iFrom, shift, new Dna(fh.filler.sequence)));
                fillersMap.get(iFrom ^ 1).add(new ShiftedFiller(iTo ^ 1, rcShift, new Dna(fh.filler.sequence).reverseComplement()));
            }
            holesReader.close();
        }

        FastaDedicatedWriter writer = new FastaDedicatedWriter(contigsFile.get(), false);
        writer.start();
        CommentableSink<LightDna> sink = writer.getLocalCommentableSink();

        ReaderInSmallMemory layoutReader = new ReaderInSmallMemory(layoutFile.get());
        int contigsMade = 0;
        LayoutPart p;
        Consensus consensus = new Consensus(reads, 0);
        int layoutNumber = 0;
        while (true) {
            HashSet<MutablePair<Integer, Integer>> backLayoutIndex = new HashSet<MutablePair<Integer, Integer>>();
            int readsInContig = 0;
            consensus.reset();

            LayoutPartIterator it = new LayoutPartIterator(layoutReader);
            if (!it.hasNext()) {
                break;
            }

            int startOffset = Integer.MAX_VALUE;
            int finishOffset = Integer.MIN_VALUE;

            for (p = it.next();; p = it.next()) {
                consensus.addLayoutPart(p);
                consensus.addDna(appendix[p.readNum], p.shift + reads.get(p.readNum).length());
                consensus.addDna(prependix[p.readNum], p.shift - prependix[p.readNum].length());

                backLayoutIndex.add(new MutablePair<Integer, Integer>(p.readNum, p.shift));
                for (ShiftedFiller filler: fillersMap.get(p.readNum)) {

                    if (backLayoutIndex.contains(new MutablePair<Integer, Integer>(filler.beginContig, p.shift - filler.shift))) {
                        consensus.addDna(filler.sequence, p.shift - filler.sequence.length());
                    }

                }
                int centerOffset = p.shift + reads.get(p.readNum).length() / 2;
                startOffset = Math.min(startOffset, centerOffset);
                finishOffset = Math.max(finishOffset, centerOffset);
                ++readsInContig;
                if (!it.hasNext()) {
                    break;
                }
            }

            if (readsInContig >= minReadsInContig.get() && consensus.totalSize() >= minReadLength.get()) {
                String comment = "contig_" + contigsMade;// + " <- " + layoutNumber;
                Dna dna = new Dna(consensus.toString());
                sink.put(comment, dna);
                contigsMade++;
            }
            layoutNumber++;

        }
        sink.close();
        try {
            writer.stopAndWaitForFinish();
        } catch (InterruptedException e) {
            throw new ExecutionFailedException(e);
        }

        info(groupDigits(contigsMade) + " contigs made from " + groupDigits(layoutNumber) + " layouts");
    }



    @Override
    protected void cleanImpl() {
        reads = null;
    }

    public ConsensusMaker() {
        super(NAME, DESCRIPTION);
    }

}
