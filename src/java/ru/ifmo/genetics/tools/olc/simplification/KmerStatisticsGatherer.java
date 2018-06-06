package ru.ifmo.genetics.tools.olc.simplification;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.kmers.KmerIteratorFactory;
import ru.ifmo.genetics.dna.kmers.ShortKmerIteratorFactory;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.sources.Source;
import ru.ifmo.genetics.structures.map.ArrayLong2IntHashMap;
import ru.ifmo.genetics.tools.ec.DnaQReadDispatcher;
import ru.ifmo.genetics.tools.ec.KmerLoadWorker;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.KmerIteratorFactoryParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class KmerStatisticsGatherer extends Tool {
    public static final String NAME = "kmer-statistics-gatherer";
    public static final String DESCRIPTION = "gathers kmer statistic from fastq files";

    static final int LOAD_TASK_SIZE = 1 << 15;


    // input parameters
    public final Parameter<Integer> k = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size")
            .create());

    public final Parameter<File[]> inputFiles = addParameter(new FileMVParameterBuilder("reads")
            .mandatory()
            .withShortOpt("i")
            .withDescription("list of input files")
            .create());

    public final Parameter<KmerIteratorFactory> kmerIteratorFactory = Parameter.createParameter(
            new KmerIteratorFactoryParameterBuilder("kmer-iterator-factory")
                    .optional()
                    .withDescription("factory used for iterating through kmers")
                    .withDefaultValue(new ShortKmerIteratorFactory())
                    .create());


    // output parameters
    private final InMemoryValue<ArrayLong2IntHashMap> hmOutValue = new InMemoryValue<ArrayLong2IntHashMap>();
    public final InValue<ArrayLong2IntHashMap> hmOut = addOutput("bad-kmers-number", hmOutValue, ArrayLong2IntHashMap.class);



    @Override
    protected void runImpl() throws ExecutionFailedException {
        ArrayLong2IntHashMap hm = null;
        try {
            hm = gather(inputFiles.get());
        } catch (IOException e) {
            throw new ExecutionFailedException("Couldn't load kmers", e);
        }

        hmOutValue.set(hm);
    }


    ArrayLong2IntHashMap gather(File[] files) throws IOException {
        info("Gathering k-mer statistic...");

        ArrayLong2IntHashMap hm = new ArrayLong2IntHashMap((int)(Math.log(availableProcessors.get()) / Math.log(2)) + 4);
        Source<DnaQ> reader = ReadersUtils.readDnaQLazy(files);

        DnaQReadDispatcher dispatcher = new DnaQReadDispatcher(reader, LOAD_TASK_SIZE, progress);
        KmerLoadWorker[] workers = new KmerLoadWorker[availableProcessors.get()];
        CountDownLatch latch = new CountDownLatch(workers.length);

        for (int i = 0; i < workers.length; ++i) {
            workers[i] = new KmerLoadWorker(dispatcher, latch, new Random(42),
                    k.get(), Misc.availableMemory(), hm, 0, 0, 0, kmerIteratorFactory.get());
            new Thread(workers[i]).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            warn("Main thread interrupted");
            for (KmerLoadWorker worker : workers) {
                worker.interrupt();
            }
        }

        info("Done");
        return hm;
    }



    @Override
    protected void cleanImpl() {
    }

    public KmerStatisticsGatherer() {
        super(NAME, DESCRIPTION);
    }
}


