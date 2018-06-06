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
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;

import java.io.*;
import java.util.*;

public class SmallComponentsAssembler extends Tool {
    public static final String NAME = "small-components-assembler";
    public static final String DESCRIPTION = "assembles obvious transcripts";

    // input params

    public final Parameter<File> filePrefix = addParameter(new FileParameterBuilder("file-prefix")
            .mandatory()
            .withDescription("prefix of files with edges")
            .create());

    public final Parameter<Integer> kParameter = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size (vertex, not edge)")
            .create());

    //constants
    private final float minNextFreq = 0.25f;

    // internal vars
    private int k;
    private int minLenOfGen;
    //private CompactDeBruijnGraphWF graph;
    //long graphSizeBytes;

    //private LongsHashSet wasEdges;

    int total = 0;

    int totalTranscripts = 0;

    @Override
    protected void runImpl() throws ExecutionFailedException {
        String fPref = filePrefix.get().getAbsolutePath();

        k = kParameter.get();
        minLenOfGen = k+1;
        info("Min len of gen = " + minLenOfGen);
        Timer timer = new Timer();

        info("Assembling transcripts...");
        File outDir = new File(workDir.get().getAbsolutePath() + "/transcripts");
        outDir.mkdir();


        for (File in: filePrefix.get().listFiles()){
            //if (in.getAbsolutePath().contains(fPref) && in.getAbsolutePath().contains("19927")){
            //if (in.getName().equals("393667")){
                try {
                    assebleTranscripts(buildGraph(in),new File(outDir.getAbsolutePath()+ "/"+in.getName()));
                    //info(in.getName() + " started");
                    total++;
                    if (total%1000 == 0){
                        info(total + " transcripts assembled");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            //}
           // }
        }


        info("Assembling transcripts done, it took " + timer);

    }

    private void assebleTranscripts(CompactDeBruijnGraphWF graph, File out) throws IOException {
        if (graph==null ){
            return;
        }
        BigLongHashSet wasEdges = new BigLongHashSet(graph.getMemSize()/8);
        List<Long> starts = makeSimple(graph, wasEdges);
        if (starts.size() == 0){
            System.err.println("Bad = " + out.getName() + " Total = " + total );
        }
        else{
            visGraph(starts.get(0), out.getAbsolutePath(), graph, wasEdges);
            getTranscripts(starts,out.getAbsolutePath(),graph,wasEdges);
        }
    }


    private boolean dfs(long start, PrintWriter out, CompactDeBruijnGraphWF graph,LongHashSetInterface wasEdges,Set<Long> wasStrats){
        Stack<Long> dfsStack = new Stack<Long>();
        Stack<Integer> dfsStringLen = new Stack<Integer>();
        dfsStack.push(start);
        dfsStringLen.push(0);
        StringBuilder curTs = new StringBuilder();
        int curLen = 0;
        int lenGen = 0;
        int numOfTr = 1;
        Map<Long,Integer> wasVerts = new HashMap<Long, Integer>();
        while (!dfsStack.isEmpty()){
            long curV = dfsStack.pop();
            int nextLen = dfsStringLen.pop();
            wasStrats.add(Math.min(curV,graph.reverseComplementEdge(curV)>>2));

            if (curLen != nextLen){
                curTs.delete(nextLen,curTs.length());
                lenGen = 0;
                curLen = nextLen;
                numOfTr++;
            }

            curTs.append(KmerUtils.kmer2String(curV,k).charAt(0)); //rewrite
            curLen++;
            lenGen++;

            wasVerts.put(curV,numOfTr);

            int outEdgesCount = 0;
            for (long outcome: graph.outcomeEdges(curV)){
                if (!wasEdges.contains(Math.min(outcome,graph.reverseComplementEdge(outcome))) && (!wasVerts.containsKey(outcome&graph.vertexMask) || (wasVerts.get(outcome&graph.vertexMask)<numOfTr))){
                    float freqRatio = (float)(getFreq(curV,graph,wasEdges))/(float)(getFreq(outcome&graph.vertexMask,graph,wasEdges));
                    if (Math.min(freqRatio,1f/freqRatio)>minNextFreq){
                        outEdgesCount++;
                    }
                }
            }
            if (outEdgesCount > 1){
                lenGen = 0;
                wasVerts.put(curV,numOfTr);

            }
            for (long outcome: graph.outcomeEdges(curV)){
                if (!wasEdges.contains(Math.min(outcome,graph.reverseComplementEdge(outcome)))&& (!wasVerts.containsKey(outcome&graph.vertexMask) || (wasVerts.get(outcome&graph.vertexMask)<numOfTr))){
                    float freqRatio = (float)(getFreq(curV,graph,wasEdges))/(float)(getFreq(outcome&graph.vertexMask,graph,wasEdges));
                    if (Math.min(freqRatio,1f/freqRatio)>minNextFreq){
                        dfsStack.push(outcome&graph.vertexMask);
                        dfsStringLen.push(curLen);
                    }
                }
            }
            if ((outEdgesCount == 0) && (lenGen>= minLenOfGen)){
                totalTranscripts++;
                out.println(">" + totalTranscripts + " len="+curTs.length());
                out.println(curTs+KmerUtils.kmer2String(curV,k).substring(1));
            }

            if (numOfTr > 30){
                return false;
            }
        }
        return true;
        //}
    }

    private void getTranscripts(List<Long> starts, String fnout, CompactDeBruijnGraphWF graph,LongHashSetInterface wasEdges) throws IOException {
        File fout = new File(fnout+"tr.fasta");
        fout.createNewFile();
        PrintWriter out = new PrintWriter(fout);
        Set<Long> wasStarts = new HashSet<Long>();
        for (long start: starts){
            if (!wasStarts.contains(Math.min(start,graph.reverseComplementEdge(start)>>2))){
                if (!dfs(start,out,graph,wasEdges,wasStarts)){
                    out.close();
                    fout.delete();
                    return;
                }
            }
        }
        out.close();
    }


    private boolean checkFrom(CompactDeBruijnGraphWF graph, BigLongHashSet wasEdges, long curV, int dep){
        if (dep > minLenOfGen){
            return true;
        }
        if ((graph.incomeEdges(curV).length!=1) || (graph.outcomeEdges(curV).length!=1)){
            return false;
        }
        long curE = graph.outcomeEdges(curV)[0];
        if(checkFrom(graph, wasEdges, curE & graph.vertexMask, dep + 1)){
            return true;
        }
        else{
            //wasEdges.put(graph.outcomeEdges(curV)[0]);
            wasEdges.add(Math.min(curE,graph.reverseComplementEdge(curE)));
            return false;
        }
    }

    private boolean checkTo(CompactDeBruijnGraphWF graph, BigLongHashSet wasEdges, long curV, int dep){
        if (dep > minLenOfGen){
            return true;
        }
        if ((graph.incomeEdges(curV).length!=1) || (graph.outcomeEdges(curV).length!=1)){
            return false;
        }
        long curE = graph.incomeEdges(curV)[0];
        if(checkTo(graph, wasEdges, curE >> 2, dep + 1)){
            return true;
        }
        else{
            //wasEdges.add(graph.incomeEdges(curV)[0]);
            wasEdges.add(Math.min(curE,graph.reverseComplementEdge(curE)));
            return false;
        }
    }


    private List<Long> makeSimple(CompactDeBruijnGraphWF graph, BigLongHashSet wasEdges){
        Iterator<MutableLong> iter = graph.getIterator();
        List<Long> starts = new ArrayList<Long>();
        for (MutableLong value: new IterableIterator<MutableLong>(iter)){
            long curE = value.longValue();
            long curV = curE >> 2;
            long nextV = curE & graph.vertexMask;

            if ((graph.incomeEdges(graph.reverseComplementEdge(curE)>>>2).length == 0)){
                curV = graph.reverseComplementEdge(curE)>>2;
                nextV = graph.reverseComplementEdge(curE) & graph.vertexMask;
            }

            if (graph.incomeEdges(curV).length == 0){
                if (checkFrom(graph, wasEdges, nextV, 0)){
                    starts.add(curV);
                }
                else{
                    wasEdges.add(Math.min(curE,graph.reverseComplementEdge(curE)));
                }
            }

            /*
            if (graph.outcomeEdges(curE&graph.vertexMask).length == 0){
                if (checkTo(graph,wasEdges,curE>>2,0)){
                }
                else{
                    wasEdges.put(Math.min(curE,graph.reverseComplementEdge(curE)));
                }
            } */

        }
        return starts;
    }

    private CompactDeBruijnGraphWF buildGraph(File kmersFile) throws IOException {
        CompactDeBruijnGraphWF graph;

        FileInputStream fileIn = new FileInputStream(kmersFile);
        long toRead = fileIn.getChannel().size() / 12;

        fileIn.close();

        if (toRead > (1L<<20)){
            System.err.println("Big component");
            return null;
        }


        long graphSizeBytes = Math.min(toRead * 24, (long)(Misc.availableMemory() * 0.85));      //check mem
        //debug("graph size = " + graphSizeBytes + " bytes");

        graph = new CompactDeBruijnGraphWF(k, graphSizeBytes);


        Timer timer = new Timer();
        timer.start();
        long kmerMask = 0;

            fileIn = new FileInputStream(kmersFile);
            DataInputStream in = new DataInputStream(new BufferedInputStream(fileIn));

            for (long i = 0; i < toRead; ++i) {
                long kmer = in.readLong();
                int freq = in.readInt();
                kmerMask |= kmer;
                graph.addEdge(kmer,freq);
            }
            fileIn.close();


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

        return graph;
    }

    @Override
    protected void cleanImpl() {

    }

    public SmallComponentsAssembler() {
        super(NAME, DESCRIPTION);
    }

    // ----------------------------------------------------------------------------------------------------------------
    public static void main(String[] args) {
        new SmallComponentsAssembler().mainImpl(args);
    }

    private int getFreq(long v, CompactDeBruijnGraphWF graph, LongHashSetInterface wasEdges){
        return getFreqImpl(v,graph,wasEdges)+getFreqImpl(graph.reverseComplementEdge(v)>>2,graph,wasEdges);
    }

    private int getFreqImpl(long v, CompactDeBruijnGraphWF graph, LongHashSetInterface wasEdges){ //think about was (if)
        int res = 0;
        for (long outcome : graph.outcomeEdges(v)){
            if (!wasEdges.contains(Math.min(outcome,graph.reverseComplementEdge(outcome)))){
                res+=graph.getFreg(outcome);
            }
        }
        for (long income : graph.incomeEdges(v)){
            if (!wasEdges.contains(Math.min(income,graph.reverseComplementEdge(income)))){
                res+=graph.getFreg(income);
            }
        }
        return res;
    }

    private boolean visGraph(long start, String fnout, CompactDeBruijnGraphWF graph,LongHashSetInterface wasEdges) throws IOException {
        // System.out.println("start = " + Long.toBinaryString(start));
        PrintWriter out = new PrintWriter(fnout);
        //DataOutputStream outEdges = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fnout)));
        Queue<Long> queue = new LinkedList<Long>();
        queue.add(start);
        int size = 0;
        //System.out.println("Start");
        out.println("digraph G{");
        Set<Long> wasBfs = new HashSet<Long>();
        while(!queue.isEmpty()){
            long cur = queue.poll();

            for (long outcome : graph.outcomeEdges(cur)){
                if ((!wasEdges.contains(Math.min(outcome,graph.reverseComplementEdge(outcome)))) && (!wasBfs.contains(Math.min(outcome,graph.reverseComplementEdge(outcome))))){
                    long outV = outcome&graph.vertexMask;
                    //out.println(cur + " " + outV);
                    out.println(cur + " [ label = \"" + getFreq(cur,graph,wasEdges) + "\"];");
                    out.println(outV + " [ label = \"" + getFreq(outV,graph,wasEdges) + "\"];");
                    out.println(cur + " -> " + outV + " [ label = \"" + KmerUtils.kmer2String(cur, k) + KmerUtils.kmer2String(outV,k).charAt(KmerUtils.kmer2String(outV,k).length()-1) + "\"];");
                    //outEdges.writeLong(Math.min(outcome, graph.reverseComplementEdge(outcome)));
                    //.println(Math.min(outcome, graph.reverseComplementEdge(outcome)));
                    queue.add(outV);
                    //wasEdges.put(Math.min(outcome,graph.reverseComplementEdge(outcome)));
                    wasBfs.add(Math.min(outcome,graph.reverseComplementEdge(outcome)));
                    size++;
                }
            }
            for (long income : graph.incomeEdges(cur)){
                if ((!wasEdges.contains(Math.min(income,graph.reverseComplementEdge(income)))) && (!wasBfs.contains(Math.min(income,graph.reverseComplementEdge(income))))){
                    long outV = (income>>2)&graph.vertexMask;
                    //out.println(outV + " " + cur);
                    out.println(cur + " [ label = \"" + getFreq(cur,graph,wasEdges) + "\"];");
                    out.println(outV + " [ label = \"" + getFreq(outV,graph,wasEdges) + "\"];");
                    out.println(outV + " -> " + cur + " [ label = \"" + KmerUtils.kmer2String(outV,k) + KmerUtils.kmer2String(cur,k).charAt(KmerUtils.kmer2String(cur,k).length()-1) + "\"];");
                    //out.println(KmerUtils.kmer2String(outV,k) + " " + KmerUtils.kmer2String(cur,k));
                    //outEdges.writeLong(Math.min(income, graph.reverseComplementEdge(income)));
                    queue.add(outV);
                    //wasEdges.put(Math.min(income,graph.reverseComplementEdge(income)));
                    wasBfs.add(Math.min(income,graph.reverseComplementEdge(income)));
                    size++;
                }
            }
        }
        out.println("}");
        out.close();
        //outEdges.close();
        if ((size >= 0) && (size < 150)){
            File toDel = new File(fnout);
            toDel.delete();
            return false;
        }
        return true;
    }
}

