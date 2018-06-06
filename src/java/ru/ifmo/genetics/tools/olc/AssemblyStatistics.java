package ru.ifmo.genetics.tools.olc;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.LineReader;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.sources.Source;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;

import java.io.*;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

import static ru.ifmo.genetics.utils.NumUtils.groupDigits;

public class AssemblyStatistics extends Tool {
    public static final String NAME = "assembly-statistics";
    public static final String DESCRIPTION = "calculates simply statistics on assembled contigs";


    // input parameters
    public final Parameter<File> readsFile = addParameter(new FileParameterBuilder("reads-file")
            .mandatory()
            .withShortOpt("i")
            .withDescription("file with all reads")
            .create());


    // internal variables
    private TreeMap<Long, Integer> lengths = new TreeMap<Long, Integer>(new Comparator<Long>() {
        @Override
        public int compare(Long o1, Long o2) {
            return o1 < o2 ? 1 : o1 > o2 ? -1 : 0;
        }
    });
    private int contigsNumber, contigsNumberMore500;
    private long totalLength;
    private long[] n;


    @Override
    protected void runImpl() throws IOException, ExecutionFailedException {
        n = new long[101];
        contigsNumber = 0;
        contigsNumberMore500 = 0;
        totalLength = 0;
        lengths.clear();

        info("Loading reads and calculating statistics...");
        Source<DnaQ> source = ReadersUtils.readDnaQLazy(readsFile.get());
        Iterator<DnaQ> it = source.iterator();

        while (it.hasNext()) {
            add(it.next().length());
        }

        info("Statistics:\n" + toString());
    }

    public boolean hasStatistics() {
        return (contigsNumber != 0);
    }


    private void add(long length) {
        Misc.incrementInt(lengths, length);
        totalLength += length;
        contigsNumber++;
        if (length >= 500) {
            contigsNumberMore500++;
        }
    }

    private long getN(int percent) {
        return n[percent];
    }
    
    private long getMaxLength() {
        return lengths.isEmpty() ? 0 : lengths.firstKey();
    }

    private long getMinLength() {
        return lengths.isEmpty() ? 0 : lengths.lastKey();
    }

    private long getMeanLength() {
        return lengths.isEmpty() ? 0 : Math.round(((double)totalLength) / contigsNumber);
    }
    
    private void update() {
        int p = 0;
        long curLength = 0;
        for (long length : lengths.keySet()) {
            curLength += length * lengths.get(length);
            int percent = (int)(curLength * 100 / totalLength);
            for (int i = p; i <= percent; ++i) {
                n[i] = length;
            }
            p = percent + 1;
        }
    }
    
    @Override
    public String toString() {
        update();

        StringBuilder sb = new StringBuilder();
        sb.append("Total contigs: " + groupDigits(contigsNumber) + "\n");
        sb.append("Contigs>=500bp: " + groupDigits(contigsNumberMore500) + "\n");
        sb.append("Total length: " + groupDigits(totalLength) + "\n");
        sb.append("Maximal length: " + groupDigits(getMaxLength()) + "\n");
        sb.append("Mean length: " + groupDigits(getMeanLength()) + "\n");
        sb.append("Minimal length: " + groupDigits(getMinLength()) + "\n");
        sb.append("N50: " + groupDigits(getN(50)) + "\n");
        sb.append("N90: " + groupDigits(getN(90)) + "\n");
//        sb.append("lens: " + lengths);
        
        return sb.toString();
    }


    @Override
    protected void cleanImpl() {
    }

    public AssemblyStatistics() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new AssemblyStatistics().mainImpl(args);
    }
}
