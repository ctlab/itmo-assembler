package ru.ifmo.genetics.tools.olc.layouter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.apache.hadoop.mapred.OutputCollector;

//import ru.ifmo.genetics.tools.longReadsAssembler.AssemblyStat;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.dna.Dna;

public class ConsensusLayoutCollectorWriter implements ConsensusLayoutWriter {

    OutputCollector<Int128WritableComparable, DnaWritable> output;
    Consensus consensus;
    int readsInContig = 0;
    int minReadsInContig = 0;
    private MessageDigest md5;

    {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

//    AssemblyStat stat = new AssemblyStat();

    public ConsensusLayoutCollectorWriter(ArrayList<Dna> reads, OutputCollector<Int128WritableComparable, DnaWritable> output,
                                          int minReadsInContig, double percentForElect, int minimalCoverage) {
        this.output = output;
        consensus = new Consensus(reads, percentForElect, minimalCoverage);
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
    }

    @Override
    public void flush() throws IOException {
        if (readsInContig >= minReadsInContig) {
            Dna dna = consensus.getDna();

            md5.reset();
            md5.update(dna.toByteArray());
            Int128WritableComparable key = new Int128WritableComparable();
            key.set(md5.digest());
            output.collect(key, new DnaWritable(dna));

//            stat.add(dna.length());
        }
        consensus.reset();
        readsInContig = 0;
    }

//    @Override
//    public AssemblyStat getStat() {
//        return stat;
//    }

}
