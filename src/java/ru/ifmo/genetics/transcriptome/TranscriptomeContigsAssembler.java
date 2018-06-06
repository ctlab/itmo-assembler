package ru.ifmo.genetics.transcriptome;

import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.io.readers.BinqReader;
import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.utils.KmerUtils;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.LongParameterBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;


public class TranscriptomeContigsAssembler extends Tool {
    public static final String NAME = "transcriptome-contigs-assembler";
    public static final String DESCRIPTION = "assemble quasi-contigs";


    public final Parameter<File> folderNameWithBaskets = addParameter(new FileParameterBuilder("folder-name-with-baskets")
            .withDescription("path to folder with k-mer baskets")
            .create());

    public final Parameter<Integer> kMerSize = addParameter(new IntParameterBuilder("k-mer-size")
            .withDescription("k-mer size")
            .withDefaultValue(30) //TODO <--constant
            .create());

    public final Parameter<Long> memSize = addParameter(new LongParameterBuilder("calculatesize-in-bytes")
            .withDescription("mem size in bytes")
            .withDefaultValue((Misc.availableMemory() - (long) 300e6) / 4)
            .withDefaultComment("auto")
            .create());

    public final Parameter<File> inputDir = addParameter(new FileParameterBuilder("input-dir")
            .withDescription("dir with short reads")
            .create());
    /*
public final Parameter<File> inputFile1 = addParameter(new FileParameterBuilder("input-file1")
.withDescription("file with first short reads")
.create());

public final Parameter<File> inputFile2 = addParameter(new FileParameterBuilder("input-file2")
.withDescription("file with second short reads")
.create());
      */
    public final Parameter<File> outFile = addParameter(new FileParameterBuilder("out-file")
            .withDescription("out file")
            .withDefaultValue(workDir.append("contigs"))
            .create());

    public final Parameter<Integer> minContigLenght = addParameter(new IntParameterBuilder("min-contig-lenght")
            .withDefaultValue(20)                  //TODO <--constant?
            .withDefaultComment("auto")
            .withDescription("minimal contig lenght")
            .create());

    public final Parameter<Integer> maxContigLenght = addParameter(new IntParameterBuilder("max-contig-lenght")
            .withDefaultValue(550)                  //TODO <--constant?
            .withDefaultComment("auto")
            .withDescription("maximum contig lenght")
            .create());

    //private CompactDeBruijnGraphWithStat graph;

    private final int SPEC_GO = 100000;

    private final int MAX_QUEUE_SIZE = 50000;

    private double canGo = 2.5;

    public TranscriptomeContigsAssembler() {
        super(NAME, DESCRIPTION);
    }

    //static PrintWriter deepf;

    private long setFirstBitToOne(long toSet) {
        return (1L << 63) | toSet;
    }
                                   /*
    private long setFirstBitToZero(long toSet) {
        return ((1L << 63) - 1L) & toSet;
    }                                */

    private boolean isFirstBitOne(long toCheck) {
        return (toCheck >> 63) != 0;
    }

    private String bfs(long km1, long km2, CompactDeBruijnGraphWithStat graph) {

        if (!graph.containsEdge(km1)) {
  //          System.err.println("Bad edge = " + km1);
            return "-3";
        }

        long e = 0; //TODO is needed?
        int totalPaths = 0;
        int deep = -1;
        Set<Long> queue = new HashSet<Long>();
        Set<Long> nextQueue = new HashSet<Long>();
        Set<Long> swap;

        Map<Long, Long> pathLayers[] = new HashMap[maxContigLenght.get()];

        long answer = 0;
        int layerAnswer = 0;

        queue.add(km1);
        while (true) {
            deep++;
            if ((deep == maxContigLenght.get()) || (totalPaths > 2)) {
                break;
            }
            if (queue.isEmpty()) {
                break;
            }
            pathLayers[deep] = new HashMap<Long, Long>();
            for (long cur : queue) {
                long[] outE = graph.outcomeEdges(cur & graph.vertexMask);
                double prob1 = (double) graph.getValue(cur);

                for (long e2 : outE) {
                    e = e2;
                    double prob2 = (double) graph.getValue(e);
                    if (/*(Math.abs(prob1-prob2)<canGo)*/(Math.max(prob1 / prob2, prob2 / prob1) < canGo) || (prob1 > SPEC_GO) || (prob2 > SPEC_GO)) {
                        if (nextQueue.size() < MAX_QUEUE_SIZE) {
                            if (isFirstBitOne(cur)){
                                e = setFirstBitToOne(e);
                            }
                            nextQueue.add(e);
                            pathLayers[deep].put(e&graph.edgeMask, cur&graph.edgeMask);
                        }
                        else{
//                            System.err.println("Deep when reach max size = " + deep);
                            return "-2";
                        }
                    } else {/*
                        if (!isFirstBitOne(cur)) {
                            e = setFirstBitToOne(e);
                            nextQueue.add(e);
                            pathLayers[deep].put(e&graph.edgeMask, cur&graph.edgeMask);
                        }     */
                    }
                }

                if ((deep >= minContigLenght.get()) && ((graph.getEdgeKey(cur)) == (graph.getEdgeKey(km2)))) {
                    answer = cur&graph.edgeMask;
                    layerAnswer = deep - 1;
                    totalPaths++;
                }
            }
            queue.clear();
            swap = nextQueue;
            nextQueue = queue;
            queue = swap;
            nextQueue.clear();
        }
        if (totalPaths == 0) {
            /*
            System.err.println("DEEP = " + deep);
            System.err.println();*/
            return "0";
        }

        if (totalPaths > 1) {
            return "-1";
        }
        //System.err.println("DONE!!!!");
        StringBuilder sbAns = new StringBuilder();

        long lastKMerBegin = km2 >> 2;
        //TODO speed up
        for (int i = layerAnswer; i > -1; i--) {
            if (lastKMerBegin == (answer&graph.vertexMask)){
                sbAns.append(KmerUtils.kmer2String(answer, kMerSize.get()).charAt(kMerSize.get()-1));
                lastKMerBegin = answer >> 2;
            }
            else{
                sbAns.append(KmerUtils.kmer2String(graph.reverseComplementEdge(answer), kMerSize.get()).charAt(kMerSize.get()-1));
                lastKMerBegin = graph.reverseComplementEdge(answer) >> 2;
            }
            //ans += KmerUtils.kmer2String(answer,SIZE+1).charAt(SIZE);
            answer = pathLayers[i].get(answer);
        }
        StringBuilder sbFinAns = new StringBuilder(KmerUtils.kmer2String(km1, kMerSize.get()));
        //String finAns = KmerUtils.kmer2String(answer,SIZE+1);
        for (int i = layerAnswer; i > -1; i--) {
            sbFinAns.append(sbAns.charAt(i));
            //finAns+=ans.charAt(i);
        }
        //return finAns;
        return sbFinAns.toString();
    }

    private void assembl(File inputfile1, File inputfile2, File outfile, CompactDeBruijnGraphWithStat graph) {
        //deepf = new PrintWriter("deep");

        try {
            Iterator<DnaQ> iter1 = (new BinqReader(inputfile1)).iterator();
            Iterator<DnaQ> iter2 = (new BinqReader(inputfile2)).iterator();
            PrintWriter out = new PrintWriter(outfile);
            DnaQ first;
            DnaQ second;
            int total = 0;
            /*
            StringBuilder badKMmerBuilder = new StringBuilder();
            for (int i = 0; i < kMerSize.get(); i++) {
                badKMmerBuilder.append('A');
            }
            String badString = badKMmerBuilder.toString();
               */
            while (iter1.hasNext()) {
                first = iter1.next();
                second = iter2.next();

                if ((first.length - 10 < kMerSize.get() + 1) || (second.length - 10 < kMerSize.get() + 1)) {
                    continue;
                } /*
                if (first.toString().endsWith(badString)) {
                    continue;
                }   */
                int firstStart = first.length - kMerSize.get();
                int secondStart = 0;

                for (;firstStart > -1; firstStart--){
                    if (graph.containsEdge(KmerUtils.toLong(first.substring(firstStart, firstStart+kMerSize.get())))){
                        break;
                    }
                }
                if (firstStart == -1){
                    continue;
                }
                for (;secondStart<second.length-kMerSize.get(); secondStart++){
                    if (graph.containsEdge(KmerUtils.toLong((second.reverseComplement()).substring(secondStart, secondStart + kMerSize.get())))){
                        break;
                    }
                }
                if (secondStart == second.length-kMerSize.get()){
                    continue;
                }
                //System.err.println("From " + first.substring(first.length() - kMerSize.get()-10, first.length()-10) + " to " + (second.reverseComplement()).substring(10, kMerSize.get()+10));



                out.println(bfs(KmerUtils.toLong(first.substring(firstStart, firstStart+kMerSize.get())),
                        KmerUtils.toLong((second.reverseComplement()).substring(secondStart, secondStart + kMerSize.get())), graph));
                total++;
                if (total % 10000 == 0) {
                    System.err.println("Done " + total);
                    if (total == 50000) {
                        out.close();
                        return;
                    }
                }

            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        //deepf.println("num of kmers = " + graph.edgesSize());
        //deepf.close();
    }


    public void test(int k, long mem) {
        System.out.println("Start");
        CompactDeBruijnGraphWithStat graph = new CompactDeBruijnGraphWithStat(k, mem);
        Dna dna = new Dna("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCC");
        graph.addEdge(KmerUtils.toLong(dna), 20);
        dna = new Dna("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCG");
        graph.addEdge(KmerUtils.toLong(dna), 20);
        dna = new Dna("CCCCCCCCCCCCCCCCCCCCCCCCCCCCGG");
        graph.addEdge(KmerUtils.toLong(dna), 20);
        dna = new Dna("CCCCCCCCCCCCCCCCCCCCCCCCCCCGGC");
        graph.addEdge(KmerUtils.toLong(dna), 20);
        Dna dna2 = new Dna("CCCCCCCCCCCCCCCCCCCCCCCCCCGGCC");
        graph.addEdge(KmerUtils.toLong(dna2), 20);
        dna = new Dna("TTTTTTTTTTTTTTTTTTTTTTTTTTTTTC");
        graph.addEdge(KmerUtils.toLong(dna), 20);
        dna = new Dna("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCC");
        System.err.println(bfs(KmerUtils.toLong(dna), KmerUtils.toLong(dna2), graph));
        System.out.println("Finish");
    }

    @Override
    protected void cleanImpl() {

    }

    @Override
    protected void runImpl() throws ExecutionFailedException {


        //test(30,1024);

        Timer timer = new Timer();
        System.err.println("Building graph with k = " + kMerSize.get() + " ...");
        GraphBuilder gb = new GraphBuilder(kMerSize.get()-1, memSize.get());
        gb.build(folderNameWithBaskets.get());
        System.err.println("Building graph done, it took " + timer);
        timer.start();
        System.err.println("Assembling kmers...");
        File[] inputFiles = inputDir.get().listFiles();//TODO rewrite
        assembl(inputFiles[0], inputFiles[1], outFile.get(), gb.getGraph());
        System.err.println("Assembling kmers done, it took " + timer);

        /*
        long[] badKmers = new long[]{270715583017615934L,
                212333377684823965L,
                213641733454185135L,
                207656886566090838L,
                8829172181165316L,
                40620427718950964L,
                252750573932384798L,
                195671385682565397L,
                285803771394855720L,
                152795298285147526L,
                76694042560476800L,
                141010741507912751L,
                52389173910227192L,
                221253795052725719L};

        int numOfBad = 0;
        for (long cur:badKmers){
            System.err.println(KmerUtils.kmer2dna(cur, kMerSize.get()));
            /*
            if (!gb.getGraph().containsEdge(cur)){
                numOfBad++;
            }
        }    */

        //System.err.println("There is " + numOfBad + " kmers from " + badKmers.length + " kmers");

        /*
        Dna test = new Dna("AGTCGCCCTGGGCACTCGAACAATTTTCA");


        if (!gb.getGraph().containsEdge(KmerUtils.toLong(test))){
             System.err.println("Dont contain AGTCGCCCTGGGCACTCGAACAATTTTCA");
        }
        else{
            System.err.println("Outcome of AGTCGCCCTGGGCACTCGAACAATTTTCA = " + gb.getGraph().outcomeEdges(KmerUtils.toLong(test) & gb.getGraph().vertexMask).length);
        }
        */


    }
}
