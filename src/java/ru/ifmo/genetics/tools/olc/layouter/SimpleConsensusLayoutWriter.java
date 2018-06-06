package ru.ifmo.genetics.tools.olc.layouter;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

//import ru.ifmo.genetics.tools.longReadsAssembler.AssemblyStat;
import ru.ifmo.genetics.dna.Dna;

public class SimpleConsensusLayoutWriter implements ConsensusLayoutWriter {

    Consensus consensus;
    Writer writer;
    int readsInContig = 0;
    int minReadsInContig;
    int contigsMade = 0;
    
//    AssemblyStat stat = new AssemblyStat();

    public SimpleConsensusLayoutWriter(ArrayList<Dna> reads, Writer writer, int minReadsInContig, double percentForElect,
                                       int minimalCoverage) {
        consensus = new Consensus(reads, percentForElect, minimalCoverage);
        this.writer = writer;
        this.minReadsInContig = minReadsInContig;
    }
    
    @Override
    public void addLayout(int cur, int shift) throws IOException {
        consensus.addLayoutPart(new LayoutPart(cur, shift));
        ++readsInContig;
    }

    @Override
    public void close() throws IOException {
        flush();
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        if (readsInContig >= minReadsInContig) {
            writer.write(">" + contigsMade++ + "\n");
            writer.write(consensus + "\n");
            
//            stat.add(consensus.totalSize());
        }
        consensus.reset();
        readsInContig = 0;
    }
    
//    @Override
//    public AssemblyStat getStat() {
//        return stat;
//    }

}
