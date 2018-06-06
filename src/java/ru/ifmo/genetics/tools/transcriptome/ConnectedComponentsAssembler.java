package ru.ifmo.genetics.tools.transcriptome;

import org.apache.commons.lang.mutable.MutableLong;
import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.structures.set.BigLongHashSet;
import ru.ifmo.genetics.structures.set.LongHashSetInterface;
import ru.ifmo.genetics.transcriptome.CompactDeBruijnGraphWF;
import ru.ifmo.genetics.utils.KmerUtils;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.iterators.IterableIterator;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;

import java.io.*;
import java.util.*;

public class ConnectedComponentsAssembler extends Tool {
    public static final String NAME = "connected-components";
    public static final String DESCRIPTION = "show connected components in De Bruijn graph";

    // input params
    public final Parameter<File[]> kmersFiles = addParameter(new FileMVParameterBuilder("kmers-files")
            .mandatory()
            .withDescription("files with (k+1)-mers to add")
            .create());

    public final Parameter<Integer> kParameter = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size (vertex, not edge)")
            .create());

    public final Parameter<File> outFilePrefix = addParameter(new FileParameterBuilder("file-prefix")
            .withShortOpt("po")
            .withDescription("output prefix")
            .withDefaultValue(workDir.append("/components"))
            .create());

    public final SmallComponentsAssembler sCA = new SmallComponentsAssembler();
    {
        setFix(sCA.filePrefix, workDir.append(outFilePrefix.get().getName()));
        setFix(sCA.kParameter,kParameter);
        addSubTool(sCA);
    }

    // internal vars
    private int k;
    private int maxFreq;
    private CompactDeBruijnGraphWF graph;
    long graphSizeBytes;

    private LongHashSetInterface wasEdges;

    @Override
    protected void runImpl() throws ExecutionFailedException {

        k = kParameter.get();
        maxFreq = k/2 + k/4;
        Timer timer = new Timer();
        info("Building graph...");
        try {
            buildGraph();
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
        info("Building graph done, it took " + timer);
        timer.start();
        wasEdges = new BigLongHashSet(graphSizeBytes / 8);
        wasEdges.add(0L);

        int numOfComponents = 0;
        int total = 0;

        outFilePrefix.get().mkdir();

        Iterator<MutableLong> iter = graph.getIterator();
        for (MutableLong value: new IterableIterator<MutableLong>(iter)) {
            total++;
            long curE = value.longValue();
            if (!wasEdges.contains(Math.min(curE,graph.reverseComplementEdge(curE)))){
                numOfComponents++;
                try {
                    bfs(curE&graph.vertexMask,outFilePrefix.get().getAbsolutePath()+"/"+numOfComponents);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (numOfComponents%10000 == 0){
                    info("I found " + numOfComponents + " components :)");
                }
            }
            /*
            if (numOfComponents > 10){
                break;
            } */
        }
        info("Done, it took " + timer);

        addStep(sCA);

    }

    private boolean checkKmerForSame(long kmer){
        int []fr = new int[4];
        String dna = KmerUtils.kmer2String(kmer,k);
        for (int i = 0; i < k; i++){
            switch(dna.charAt(i)){
                case 'A':
                    fr[0]++;
                    break;
                case 'C':
                    fr[1]++;
                    break;
                case 'G':
                    fr[2]++;
                    break;
                case 'T':
                    fr[3]++;
                    break;
            }
            for (int j = 0; j < fr.length; j++){
                if (fr[j] > maxFreq){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean bfs(long start, String fnout) throws IOException {
       // System.out.println("start = " + Long.toBinaryString(start));
        //PrintWriter out = new PrintWriter(fnout);
        DataOutputStream outEdges = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fnout)));
        Queue<Long> queue = new LinkedList<Long>();
        queue.add(start);
        int size = 0;
        //System.out.println("Start");
        while(!queue.isEmpty()){
            long cur = queue.poll();
            if ((graph.outcomeEdges(cur).length == 4) || (graph.incomeEdges(cur).length == 4) || (graph.outcomeEdges(cur).length + graph.incomeEdges(cur).length > 5) ||checkKmerForSame(cur)){
                for (long outcome : graph.outcomeEdges(cur)){
                    wasEdges.add(Math.min(outcome,graph.reverseComplementEdge(outcome)));
                }
                for (long income : graph.incomeEdges(cur)){
                    wasEdges.add(Math.min(income,graph.reverseComplementEdge(income)));
                }
                continue;
            }

            for (long outcome : graph.outcomeEdges(cur)){
                if (!wasEdges.contains(Math.min(outcome,graph.reverseComplementEdge(outcome)))){
                    long outV = outcome&graph.vertexMask;
                    //out.println(cur + " " + outV);
                    //out.println(KmerUtils.kmer2String(cur,k) + " " + KmerUtils.kmer2String(outV,k));
                    outEdges.writeLong(Math.min(outcome, graph.reverseComplementEdge(outcome)));
                    outEdges.writeInt(graph.getFreg(outcome));
                            //.println(Math.min(outcome, graph.reverseComplementEdge(outcome)));
                    queue.add(outV);
                    wasEdges.add(Math.min(outcome,graph.reverseComplementEdge(outcome)));
                    size++;
                }
            }
            for (long income : graph.incomeEdges(cur)){
                if (!wasEdges.contains(Math.min(income,graph.reverseComplementEdge(income)))){
                    long outV = (income>>2)&graph.vertexMask;
                    //out.println(outV + " " + cur);
                    //out.println(KmerUtils.kmer2String(outV,k) + " " + KmerUtils.kmer2String(cur,k));
                    outEdges.writeLong(Math.min(income, graph.reverseComplementEdge(income)));
                    outEdges.writeInt(graph.getFreg(income));
                    queue.add(outV);
                    wasEdges.add(Math.min(income,graph.reverseComplementEdge(income)));
                    size++;
                }
            }
        }
        //System.out.println("Finish with " + size);
        //out.close();
        outEdges.close();
        if ((size >= 0) && (size < 150)){
            File toDel = new File(fnout);
            toDel.delete();
            return false;
        }
        return true;
    }

    private void buildGraph() throws IOException {
        long totalToRead = 0;
        for (File kmersFile : kmersFiles.get()) {
            FileInputStream fileIn = new FileInputStream(kmersFile);
            long toRead = fileIn.getChannel().size() / 12;
            totalToRead += toRead;
            fileIn.close();
        }

        debug("have to read " + totalToRead + " k-mers");

        graphSizeBytes = Math.min(totalToRead * 24, (long)(Misc.availableMemory() * 0.85));//TODO check mem
        debug("graph size = " + graphSizeBytes + " bytes");

        graph = new CompactDeBruijnGraphWF(k, graphSizeBytes);

        long xx = 0;
        Timer timer = new Timer();
        timer.start();
        long kmerMask = 0;
        for (File kmersFile : kmersFiles.get()) {
            FileInputStream fileIn = new FileInputStream(kmersFile);
            DataInputStream in = new DataInputStream(new BufferedInputStream(fileIn));
            long toRead = fileIn.getChannel().size() / 12;
            xx += toRead;
            for (long i = 0; i < toRead; ++i) {
                long kmer = in.readLong();
                int freq = in.readInt();
                kmerMask |= kmer;
                graph.addEdge(kmer,freq);
                if (i % 1000000 == 0) {
                    System.err.println(i + " k-mers read");
                }
                //graph.addEdge(new ShallowBigKmer(kmer, k + 1));
            }
            fileIn.close();
            debug(xx + " out of " + totalToRead + " k-mers loaded");

            double done = ((double) xx) / totalToRead;
            double total = timer.getTime() / done / 1000;
            double remained = total * (1 - done);
            double elapsed = total * done;

            showProgressOnly(100 * done + "% done");
            debug("estimated  total time: " + total + ", remaining: " + remained + ", elapsed: "
                    + elapsed);
        }
        if (kmerMask != ((1L << (2 * k + 2)) - 1)) {
            warn("k-mer size mismatch");
            warn("set: " + k);
            debug(String.format("kmerMask: 0x%x", kmerMask));
            for (int i = 1; i < 30; ++i) {
                if (kmerMask == ((1L << (2 * i)) - 1)) {
                    warn("found: " + (i - 1));
                    break;
                }
            }
        }
        info("Graph size: " + graph.edgesSize());
    }

    @Override
    protected void cleanImpl() {
        graph = null;

    }

    public ConnectedComponentsAssembler() {
        super(NAME, DESCRIPTION);
    }

    // ----------------------------------------------------------------------------------------------------------------
    public static void main(String[] args) {
        new ConnectedComponentsAssembler().mainImpl(args);
    }
}
