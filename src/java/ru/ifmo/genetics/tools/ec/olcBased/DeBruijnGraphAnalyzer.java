package ru.ifmo.genetics.tools.ec.olcBased;

import ru.ifmo.genetics.tools.io.LazyLongReader;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.Yielder;

import java.io.*;
import java.util.*;

public class DeBruijnGraphAnalyzer extends Tool {
    public static final String NAME = "DBGraph-analyzer";
    public static final String DESCRIPTION = "Analyze de Bruijn graph ...";


    // input parameters
    public final Parameter<File[]> goodKmerFiles = addParameter(new FileMVParameterBuilder("good-kmer-files")
            .mandatory()
            .withShortOpt("g")
            .withDescription("good kmer files")
            .create());

    public final Parameter<Integer> anchorLen = addParameter(new IntParameterBuilder("anchor-length")
            .withShortOpt("a")
            .withDefaultValue(19)
            .withDescription("anchor length")
            .create());

    public final Parameter<Integer> chainMaxLen = addParameter(new IntParameterBuilder("chain-max-length")
            .withShortOpt("cl")
            .withDefaultValue(new Yielder<Integer>() {
                @Override
                public Integer yield() {
                    return anchorLen.get()/2 + 3;
                }
                @Override
                public String description() {
                    return "anchorLength/2 + 3 (" + yield() + " for anchorLength=" + anchorLen.get() + ")";
                }
            })
            .withDescription("chain maximal length in error correction")
            .create());

    public final Parameter<File> outputChainFile = addParameter(new FileParameterBuilder("output-chain-file")
            .withShortOpt("o")
            .withDefaultValue(workDir.append("chains"))
            .withDescription("output chain file")
            .create());


    // internal variables
    private Set<Long> kmers;
    private Map<Integer, Integer> stat;

    // output parameters


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            info("Loading data...");
            loadKmers();

            analyse();
            printStatistics();

        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
    }


    void loadKmers() throws IOException {
        LazyLongReader reader = new LazyLongReader(goodKmerFiles.get());
        kmers = new HashSet<Long>();
        while (true) {
            try {
                long kmer = reader.readLong();
                kmers.add(kmer);
                kmers.add(rc(kmer, anchorLen.get()));
            } catch (EOFException e) {
                break;
            }
        }
        info(kmers.size() + " kmers loaded");
    }

    void analyse() throws IOException {
        info("Starting analysis...");

        Set<Long> processed = new HashSet<Long>();
        stat = new TreeMap<Integer, Integer>();
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputChainFile.get())));

        for (long kmer : kmers) {
            if (processed.contains(kmer)) {
                continue;
            }

            processed.add(kmer);
            processed.add(rc(kmer, anchorLen.get()));

            List<Long> leftHalf = new ArrayList<Long>();
            List<Long> rightHalf = new ArrayList<Long>();
            leftHalf.add(kmer);

            if ((leftNumber(kmer, anchorLen.get(), kmers) > 1) || (rightNumber(kmer, anchorLen.get(), kmers) > 1)) {
                if (!stat.containsKey(1)) {
                    stat.put(1, 0);
                }
                stat.put(1, stat.get(1) + 2);
                dump(out, leftHalf, rightHalf, anchorLen.get());
                continue;
            }

            long left = kmer, right = kmer;
            int length = 1;

            while ((length < chainMaxLen.get()) && (leftNumber(left, anchorLen.get(), kmers) == 1)) {
                long old = left;
                left = goLeft(left, anchorLen.get(), kmers);
                if (processed.contains(left) || (leftNumber(left, anchorLen.get(), kmers) > 1) || (rightNumber(left, anchorLen.get(), kmers) > 1)) {
                    break;
                }
                processed.add(left);
                processed.add(rc(left, anchorLen.get()));
                ++length;
                leftHalf.add(left);
            }

            while ((length < chainMaxLen.get()) && (rightNumber(right, anchorLen.get(), kmers) == 1)) {
                long old = right;
                right = goRight(right, anchorLen.get(), kmers);
                if (processed.contains(right) || (leftNumber(right, anchorLen.get(), kmers) > 1) || (rightNumber(right, anchorLen.get(), kmers) > 1)) {
                    break;
                }
                processed.add(right);
                processed.add(rc(right, anchorLen.get()));
                ++length;
                rightHalf.add(right);
            }

            if (!stat.containsKey(length)) {
                stat.put(length, 0);
            }
            stat.put(length, stat.get(length) + 2);

            dump(out, leftHalf, rightHalf, anchorLen.get());
        }
        out.close();
    }

    void printStatistics() {
        int totalChains = 0;
        int totalKmers = 0;
        for (int l : stat.keySet()) {
            debug(l + " x " + stat.get(l) + ": " + l * stat.get(l));

            totalChains += stat.get(l);
            totalKmers += stat.get(l) * l;
        }
        info("Total kmers : " + totalKmers);
        info("Total chains: " + totalChains);
    }


    void dump(DataOutputStream out, List<Long> left, List<Long> right, int len) throws IOException {
        out.writeInt(left.size() + right.size());
        for (int i = left.size() - 1; i >= 0; --i) {
            out.writeLong(left.get(i));
        }
        for (int i = 0; i < right.size(); ++i) {
            out.writeLong(right.get(i));
        }

        out.writeInt(left.size() + right.size());
        for (int i = right.size() - 1; i >= 0; --i) {
            out.writeLong(rc(right.get(i), len));
        }
        for (int i = 0; i < left.size(); ++i) {
            out.writeLong(rc(left.get(i), len));
        }
    }

    static long leftNumber(long kmer, int len, Set<Long> kmers) {
        int n = 0;
        for (long nuc = 0; nuc < 4; ++nuc) {
            if (kmers.contains(shiftLeft(kmer, nuc, len))) {
                ++n;
            }
        }
        return n;
    }

    static long goLeft(long kmer, int len, Set<Long> kmers) {
        for (long nuc = 0; nuc < 4; ++nuc) {
            long t = shiftLeft(kmer, nuc, len);
            if (kmers.contains(t)) {
                return t;
            }
        }
        return kmer;
    }

    static long rightNumber(long kmer, int len, Set<Long> kmers) {
        int n = 0;
        for (long nuc = 0; nuc < 4; ++nuc) {
            if (kmers.contains(shiftRight(kmer, nuc, len))) {
                ++n;
            }
        }
        return n;
    }

    static long goRight(long kmer, int len, Set<Long> kmers) {
        for (long nuc = 0; nuc < 4; ++nuc) {
            long t = shiftRight(kmer, nuc, len);
            if (kmers.contains(t)) {
                return t;
            }
        }
        return kmer;
    }

    static long shiftLeft(long kmer, long nuc, int len) {
        return ((kmer << 2) | nuc) & ((1L << (2 * len)) - 1);
    }

    static long shiftRight(long kmer, long nuc, int len) {
        return (kmer >> 2) | (nuc << (2 * len - 2));
    }

    static long rc(long nuc) {
        return 3 - nuc;
    }

    static long rc(long kmer, int len) {
        long res = 0;
        for (int i = 0; i < len; ++i) {
            long nuc = (kmer >> (2 * i)) & 3;
            res = (res << 2) | rc(nuc);
        }
        return res;
    }

    static long min(long kmer, int len) {
        return Math.min(kmer, rc(kmer, len));
    }



    @Override
    protected void cleanImpl() {
        kmers = null;
        stat = null;
    }

    public DeBruijnGraphAnalyzer() {
        super(NAME, DESCRIPTION);
    }

}
