package ru.ifmo.genetics.tools.irf;

import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.dna.LightDnaQ;
import ru.ifmo.genetics.executors.BlockingThreadPoolExecutor;
import ru.ifmo.genetics.io.writers.AbstractDedicatedWriter;
import ru.ifmo.genetics.io.PairedLibraryInfo;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.io.sources.PairSource;
import ru.ifmo.genetics.io.sources.PairedLibrary;
import ru.ifmo.genetics.io.sources.ZippingPairedLibrary;
import ru.ifmo.genetics.io.writers.DedicatedWriter;
import ru.ifmo.genetics.io.writers.FastaDedicatedWriter;
import ru.ifmo.genetics.io.writers.ListDedicatedWriter;
import ru.ifmo.genetics.statistics.reporter.LocalMonitor;
import ru.ifmo.genetics.statistics.reporter.LocalReporter;
import ru.ifmo.genetics.structures.debriujn.WeightedDeBruijnGraph;
import ru.ifmo.genetics.tools.io.LazyDnaQReaderTool;
import ru.ifmo.genetics.tools.io.ToFastqConverter;
import ru.ifmo.genetics.tools.rf.Orientation;
import ru.ifmo.genetics.utils.IteratorUtils;
import ru.ifmo.genetics.utils.TextUtils;
import ru.ifmo.genetics.utils.iterators.ProgressableIterator;
import ru.ifmo.genetics.utils.pairs.UniPair;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.StringMVParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReadsFiller extends Tool {
    public static final String NAME = "reads-filler";
    public static final String DESCRIPTION = "fills gaps in paired reads with indel errors";

    private final static int TASK_SIZE = 1 << 8;
    private final static int MAX_FRONT_SIZE = 1000;

    public final Parameter<Integer> kParameter = addParameter(new IntParameterBuilder("k")
            .mandatory()
            .withShortOpt("k")
            .withDescription("k-mer size (vertex, not edge)")
            .create());

    public final Parameter<Integer> minInsertSize = addParameter(new IntParameterBuilder("min-size")
            .withShortOpt("l")
            .withDefaultValue(0)
            .withDescriptionShort("Minimal insert size")
            .withDescription("minimal insert size of paired-end library to check")
            .withDescriptionRuShort("Минимальный размер инсерта")
            .withDescriptionRu("Минимально возоможный размер инсерта для проверки")
            .create());

    public final Parameter<Integer> maxInsertSize = addParameter(new IntParameterBuilder("max-size")
            .withShortOpt("L")
            .withDefaultValue(1000)
            .withDescriptionShort("Maximal insert size")
            .withDescription("maximal insert size of paired-end library to check")
            .withDescriptionRuShort("Максимальный размер инсерта")
            .withDescriptionRu("Максимально возможный размер инсерта для проверки")
            .create());

    /*
    public final Parameter<Boolean> printNs = addParameter(new BoolParameterBuilder("print-ns")
            .optional()
            .withDescription("if set prints 'N' when nucleotide is ambigous")
            .create());
    */


    public final Parameter<File> graphFile = addParameter(new FileParameterBuilder("graph-file")
            .mandatory()
            .withShortOpt("g")
            .withDescription("file with weighted De Bruijn graph")
            .create());

    public final Parameter<File[]> readFiles = addParameter(new FileMVParameterBuilder("read-files")
            .mandatory()
            .withShortOpt("i")
            .withDescription("files with paired reads")
            .create());

    public final Parameter<String[]> sOrientationsToCheck = addParameter(new StringMVParameterBuilder("orientations")
            .withDefaultValue(new String[] {"FR"})
            .withDescriptionShort("Orientations to try to assemble")
            .withDescription("list of orientations to try to assemble, variants are RF, FR, FF, RR")
            .withDescriptionRuShort("Список ориентаций")
            .withDescriptionRu("Список ориентаций, которые следует попробовать при сборке квазиконтигов (возможны RF, FR, FF, RR)")
            .create());

    public final Parameter<File> outputDir = addParameter(new FileParameterBuilder("output-dir")
            .optional()
            .withShortOpt("o")
            .withDescription("directory to output built quasicontigs")
            .withDefaultValue(workDir.append("quasicontigs"))
            .create());


    // output parameters
    private final InMemoryValue<File[]> resultingQuasicontigsOutValue = new InMemoryValue<File[]>();
    public final InValue<File[]> resultingQuasicontigsOut = addOutput("resulting-quasicontigs", resultingQuasicontigsOutValue, File[].class);

    // internal variables
    private int k;
    private ArrayList<Orientation> orientationsToCheck;
    private WeightedDeBruijnGraph graph = null;
    private ArrayList<File> resultingFiles;

    @Override
    protected void cleanImpl() {
        graph = null;
        resultingFiles = null;
    }


    @Override
    protected void runImpl() throws ExecutionFailedException {
        k = kParameter.get();
        outputDir.get().mkdirs();
        orientationsToCheck = new ArrayList<Orientation>();
        for (String s: sOrientationsToCheck.get()) {
            orientationsToCheck.add(Orientation.fromString(s.toUpperCase()));
        }
        resultingFiles = new ArrayList<File>();


        info("Loading graph...");
        try {
            FileInputStream fis = new FileInputStream(graphFile.get());
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
            graph = new WeightedDeBruijnGraph();
            graph.readFields(dis);
            dis.close();
        } catch (IOException e) {
            throw new ExecutionFailedException("Can't load graph", e);
        }


//        HashMap<Long, List<LightDna>> index = buildIndex(k, graph);

        try {
            File[] files = readFiles.get();

            ArrayList<PairedLibrary<? extends LightDnaQ>> libraries = splitFilesToLibraries(files);

            fillReadsInLibraries(libraries);
        } catch (InterruptedException e) {
            throw new ExecutionFailedException(e);
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }

//        List<PairedLibrary<DnaQ>> libraries = new ArrayList<PairedLibrary<DnaQ>>();
//        {
//            File[] files = readFiles.get();
//            NamedSource<DnaQ> leftReader = LazyDnaQReaderTool.sourceFromFile(files[0]);
//            NamedSource<DnaQ> rightReader = LazyDnaQReaderTool.sourceFromFile(files[1]);
//            PairedLibrary<DnaQ> library = ZippingPairedLibrary.create(leftReader, rightReader, null);
//
//            libraries.add(library);
//        }
//
//        try {
//            processPairedLibraries(graph, libraries);
//        } catch (InterruptedException e) {
//            throw new ExecutionFailedException(e);
//        }

        resultingQuasicontigsOutValue.set(resultingFiles.toArray(new File[resultingFiles.size()]));
    }

    private ArrayList<PairedLibrary<? extends LightDnaQ>> splitFilesToLibraries(File[] files) throws ExecutionFailedException, InterruptedException, IOException {
//        assert files.length % 2 == 0;

        info("Splitting files to paired libraries");

        final int MAX_HAMMING_DISTANCE = 1;
        final int SAMPLE_SIZE = 1000;

        ArrayList<PairedLibrary<? extends LightDnaQ>> libraries = new ArrayList<PairedLibrary<? extends LightDnaQ>>();


        int n = files.length;

        ArrayList<NamedSource<? extends LightDnaQ>> sources = new ArrayList<NamedSource<? extends LightDnaQ>>(n);
        for (File file : files) {
            LazyDnaQReaderTool dnaQReader = new LazyDnaQReaderTool();
            dnaQReader.fileIn.set(file);
            dnaQReader.simpleRun();
            NamedSource<? extends LightDnaQ> source = dnaQReader.dnaQsSourceOut.get();
            sources.add(source);
        }

        int[][] d = new int[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(d[i], -1);
        }
        PairedLibraryInfo[][] stats = new PairedLibraryInfo[n][n];



        for (int i = 0; i < n; ++i) {
            for (int j = i + 1; j < n; ++j) {
                if (TextUtils.hammingDistance(sources.get(i).name(), sources.get(j).name()) > MAX_HAMMING_DISTANCE) {
                    // :ToDo: check all pairs?
                    continue;
                }

                FillingReport report = new FillingReport();
                ArrayList<LightDna> results = new ArrayList<LightDna>();

                DedicatedWriter<LightDna> writer = new ListDedicatedWriter<LightDna>(results);
                Queue<List<UniPair<DnaQ>>> writeFailedQueue = new ConcurrentLinkedQueue<List<UniPair<DnaQ>>>();

                GlobalContext env = new GlobalContext(k, graph,
                        minInsertSize.get(), maxInsertSize.get(),
                        MAX_FRONT_SIZE,
                        null,
                        orientationsToCheck,
                        writer,
                        5,
                        new LocalReporter<FillingReport>(report)
                );

                NamedSource<? extends LightDnaQ> source1 = sources.get(i);
                NamedSource<? extends LightDnaQ> source2 = sources.get(j);

                PairSource<LightDnaQ> pairedSource = PairSource.create(source1, source2);
                List<? extends UniPair<? extends LightDnaQ>> task = IteratorUtils.head(SAMPLE_SIZE, pairedSource.iterator());

                String name = source1.name() + " and " + source2.name();
                debug("Checking as paired " + name);

                FillingTask ft = new FillingTask(env, task);
                ft.runImpl();
                d[j][i] = d[i][j] = (int) ft.report.ok.value();
                debug("Got " + d[i][j] + " ok for library " + name);

                if (d[i][j] >= 50) {
                    ArrayList<Integer> lengths = new ArrayList<Integer>(results.size());
                    for (LightDna dna: results) {
                        lengths.add(dna.length());
                    }

                    Collections.sort(lengths);
                    int minSize = lengths.get(0);
                    int maxSize = lengths.get(lengths.size() - 1);
                    int sum = 0;
                    for (Integer l: lengths) {
                        sum += l;
                    }
                    int avgSize = sum / lengths.size();

                    long sumSq = 0;
                    for (Integer l: lengths) {
                        long x = (long) l - avgSize;
                        sumSq += x * x;
                    }
                    int stdDev = (int)Math.sqrt(sumSq / (lengths.size() - 1));
                    minSize = Math.max(0, minSize - stdDev);
                    maxSize += stdDev;
                    stats[j][i] = stats[i][j] = new PairedLibraryInfo(minSize, maxSize, avgSize, stdDev);
                } else {
                    debug(ft.report.toString());
                }
            }
        }

        int[] best = new int[n];
        for (int i = 0; i < n; ++i) {
            best[i] = -1;
            int max = -1;
            for (int j = 0; j < n; ++j) {
                if (i != j && d[i][j] > max) {
                    max = d[i][j];
                    best[i] = j;
                }
            }
        }

        for (int i = 0; i < n; ++i) {
            int j = best[i];
            if (j == -1 || d[i][j] < 50 || best[j] != i) {
                NamedSource<? extends LightDnaQ> source = sources.get(i);
                debug("Source " + source.name() + " seems not to be paired, skipping");
                debug("Trying to add source " + source.name() + " to resulting quasicontigs:");
                ProgressableIterator<? extends LightDnaQ> it = source.iterator();
                int len = 0, c = 0;
                for (int p = 0; (p < 1000) && it.hasNext(); p++) {
                    len += it.next().length();
                    c++;
                }
                if (c != 0) {
                    len = len / c;
                    debug("Avg len = " + len + ", c = " + c);
                    if (len >= 70) {
                        debug("Average length of dna in source " + source.name() + " is " + len + ", " +
                                "adding source to resulting quasicontigs...");
                        ToFastqConverter converter = new ToFastqConverter();
                        converter.inputFiles.set(new File[]{files[i]});
                        converter.outputDir.set(outputDir);
                        converter.simpleRun();
                        Collections.addAll(resultingFiles, converter.convertedReadsOut.get());
                    } else {
                        debug("Average length of dna in source " + source.name() + " is " + len + ", skipping");
                    }
                } else {
                    debug("Source " + source.name() + " is empty, skipping");
                }
                continue;
            }
            if (i > j) {
                continue;
            }

            ZippingPairedLibrary<LightDnaQ> library = ZippingPairedLibrary.create(sources.get(i), sources.get(j), stats[i][j]);
            info("Found paired-end library: " + library.name() + " (" + stats[i][j] + ")");
            libraries.add(library);
        }
        return libraries;
    }
    private void fillReadsInLibraries(List<PairedLibrary<? extends LightDnaQ>> libraries) throws ExecutionFailedException, InterruptedException, IOException {
        for (PairedLibrary<? extends LightDnaQ> library: libraries) {
            info("Processing library " + library.name());

            DedicatedWriter<LightDna> writer = new FastaDedicatedWriter(new File(outputDir.get(), library.name() + ".fasta"), false);

            FillingReport report = new FillingReport();
            LocalMonitor monitor = new LocalMonitor(report);
            monitor.start();

            GlobalContext context = new GlobalContext(
                    k,
                    graph,
                    minInsertSize.get(),
                    maxInsertSize.get(),
                    MAX_FRONT_SIZE,
                    outputDir.get(), orientationsToCheck, writer, 5, new LocalReporter<FillingReport>(report));


            writer.start();

            BlockingThreadPoolExecutor pool = new BlockingThreadPoolExecutor(availableProcessors.get());
            processPairedLibrary(library, pool, context);

            debug("Dataset read, waiting for termination");

            pool.shutdownAndAwaitTermination();
            monitor.stop();

            try {
                writer.stopAndWaitForFinish();
            } catch (InterruptedException e) {
                throw new ExecutionFailedException(e);
            }
            Collections.addAll(resultingFiles, writer.getResultingFiles());
        }
    }


    private void processPairedLibrary(PairedLibrary<? extends LightDnaQ> library, BlockingThreadPoolExecutor pool, GlobalContext env) throws InterruptedException {
        int pairCounter = 0;
        List<UniPair<? extends LightDnaQ>> task = new ArrayList<UniPair<? extends LightDnaQ>>(TASK_SIZE);

        final int PROGRESS_MAX = 1 << 13;
        progress.setTotalTasks(PROGRESS_MAX);
        progress.createProgressBar();

        for (ProgressableIterator<? extends UniPair<? extends LightDnaQ>> it = library.iterator(); it.hasNext();) {
            UniPair<? extends LightDnaQ> pair = it.next();
            pairCounter++;

            task.add(pair);
            if (task.size() == TASK_SIZE) {
                pool.blockingExecute(new FillingTask(env, task));
                progress.updateDoneTasks((int)(it.progress() * PROGRESS_MAX));

                task = new ArrayList<UniPair<? extends LightDnaQ>>(TASK_SIZE);
            }
        }

        progress.destroyProgressBar();

        if (task.size() != 0) {
            pool.blockingExecute(new FillingTask(env, task));
        }

    }


    private HashMap<Long, List<LightDna>> buildIndex(int k, WeightedDeBruijnGraph graph) throws IOException, ExecutionFailedException {
        HashMap<Long, List<LightDna>> index = new HashMap<Long, List<LightDna>>();

        for (File file: readFiles.get()) {
            NamedSource<DnaQ> reader = ReadersUtils.readDnaQLazy(file);

            int done = 0;
            for (DnaQ dnaq: reader) {
                ++done;
                debug(done);
                if (dnaq.length() <= k)
                    continue;

                long maxWeightKmer = 0;
                int maxWeight = 0;
                long cur = 0;

                for (int i = 0; i < k; i++) {
                    cur = (cur << 2) | dnaq.nucAt(i);
                }
                // String s = dnaq.toString();
                for (int i = k; i < dnaq.length(); i++) {
                    cur = cur & graph.vertexMask;
                    cur = (cur << 2) | dnaq.nucAt(i);
                    int weight = graph.getWeight(cur);
                    if (weight > maxWeight) {
                        maxWeight = weight;
                        maxWeightKmer = cur;
                    }
                }

                List<LightDna> dnas = index.get(maxWeightKmer);
                if (dnas == null) {
                    dnas = new ArrayList<LightDna>();
                    index.put(maxWeightKmer, dnas);
                }
                dnas.add(dnaq);

                if (done % 100000 == 0) {
                    info("Index built for " + done + " sequences");
                }
            }
        }
        return index;
    }
    public static void main(String args[]) {
        new ReadsFiller().mainImpl(args);
    }

    public ReadsFiller() {
        super(NAME, DESCRIPTION);
    }

}
