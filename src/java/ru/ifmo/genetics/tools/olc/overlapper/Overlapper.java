package ru.ifmo.genetics.tools.olc.overlapper;

import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.io.writers.BufferDedicatedWriter;
import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString;
import ru.ifmo.genetics.utils.IntComparator;
import ru.ifmo.genetics.executors.PatientExecutorService;
import ru.ifmo.genetics.tools.olc.overlaps.Overlaps;
import ru.ifmo.genetics.tools.olc.suffixArray.BucketsDivider;
import ru.ifmo.genetics.tools.olc.suffixArray.BucketsSorter;
import ru.ifmo.genetics.tools.olc.suffixArray.SuffixArray;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString.$index;
import static ru.ifmo.genetics.utils.NumUtils.groupDigits;

public class Overlapper extends Tool {
    public static final String NAME = "overlapper";
    public static final String DESCRIPTION = "finds overlaps between quasi-contigs";

    // input params
    public final Parameter<File> fullStringFile = addParameter(new FileParameterBuilder("full-string-file")
            .mandatory()
            .withDescription("file with glued dnas string")
            .create());

    public final Parameter<File> sortedBucketsDir = addParameter(new FileParameterBuilder("sorted-buckets-dir")
            .mandatory()
            .withDescription("directory with sorted buckets")
            .create());

    public final Parameter<File> overlapsDir = addParameter(new FileParameterBuilder("overlaps-dir")
            .optional()
            .withDefaultValue(workDir.append("overlaps"))
            .withDescription("directory with found overlaps")
            .create());

    public final Parameter<Integer> minOverlap = addParameter(new IntParameterBuilder("min-overlap")
            .optional()
            .withShortOpt("mo")
            .withDefaultValue(40)
            .withDescriptionShort("Minimal overlap length")
            .withDescription("minimal quasicontigs overlap length")
            .withDescriptionRuShort("Минимальная длина перекрытия")
            .withDescriptionRu("Минимальная длина перекрытия квазиконтигов")
            .create());

    public final Parameter<Integer> errorsNumber = addParameter(new IntParameterBuilder("errors-number")
            .optional()
            .withShortOpt("en")
            .withDefaultValue(2)
            .withDescription("number of allowed errors while searching overlaps between quasicontigs")
            .create());

    public final Parameter<Integer> errorsWindowSize = addParameter(new IntParameterBuilder("errors-window-size")
            .optional()
            .withShortOpt("ews")
            .withDefaultValue(100)
            .withDescription("errors window size while searching overlaps between quasicontigs")
            .create());

    public final Parameter<Integer> bucketCharsNumberIn = addParameter(new IntParameterBuilder("bucket-chars-number")
            .mandatory()
            .withDescription("bucket chars number")
            .create());
    public final Parameter<Integer> bucketsNumberIn = addParameter(new IntParameterBuilder("buckets-number")
            .mandatory()
            .withDescription("buckets number")
            .create());



    // internal variables
    private int bucketCharsNumber;
    private int bucketsNumber;

    /**
     * String with all reads, formed as '$read1$read2$...$readN$'.
     */
    private GluedDnasString fullString;

    /**
     * <code>readBegin[i]</code> = position in <code>fullString</code> where read number <code>i</code> begins.
     */
    private long[] readBegin;
    private int readsNumber;
    private int realReadsNumber;

    private AtomicLong foundOverlaps = new AtomicLong();


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            info("Loading full string...");
            fullString = new GluedDnasString(fullStringFile.get());

            prepare();

            findOverlaps();

            debug("Found " + groupDigits(foundOverlaps.get()) + " overlaps " +
                    "(" + String.format("%.1f", foundOverlaps.get()*2 / (double) (realReadsNumber*2)) + " per read)");
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            throw new ExecutionFailedException(e);
        }
    }

    private void prepare() {
        info("preparing...");

        bucketCharsNumber = bucketCharsNumberIn.get();
        bucketsNumber = bucketsNumberIn.get();

        FileUtils.createOrClearDir(overlapsDir.get());

        assert fullString.get(0) == $index;
        assert fullString.get(fullString.length - 1) == $index;

        int $count = 0, allReadsNumber = 0;
        int firstEmptyRead = -1, lastEmptyRead = -1;
        for (long i = 0; i < fullString.length; i++) {
            if (fullString.get(i) == $index) {
                $count++;
                if (i + 1 < fullString.length && fullString.get(i + 1) != $index) {
                    allReadsNumber++;
                }
                if (i + 1 < fullString.length && fullString.get(i + 1) == $index) {
                    // found empty string
                    if (firstEmptyRead == -1) {
                        firstEmptyRead = allReadsNumber;
                    }
                    lastEmptyRead = allReadsNumber;
                }
            }
        }

        readsNumber = $count - 1;   // any read starts after $, without last $ in fullString

        debug("Reads number found in fullString (real, doubled): " + groupDigits(allReadsNumber));
        assert allReadsNumber % 2 == 0;    // any read with it's rc copy
        realReadsNumber = allReadsNumber / 2;

        if (firstEmptyRead != lastEmptyRead || firstEmptyRead != realReadsNumber) {
            throw new RuntimeException("Empty reads in full string!");
        }


        readBegin = new long[readsNumber + 1];
        int j = 0;
        for (long i = 0; i < fullString.length; i++) {
            if (fullString.get(i) == $index) {
                readBegin[j] = i + 1;
                j++;
            }
        }
    }


    /**
     * Searching for overlaps in all buckets. <br>
     * Loads suffix array buckets from files and saves overlaps to files. <br>
     *
     * Attention! It doesn't try to fix errors in prefix!
     */
    public void findOverlaps() throws IOException, InterruptedException {
        info("Searching for overlaps in all buckets...");
        Timer gt = new Timer();

        showProgress("Dividing reads into buckets...");
        ArrayList<Integer>[] readsByBucket = divideReadsIntoBuckets();

        progress.setTotalTasks(bucketsNumber);
        for (int bucket = 0; bucket < bucketsNumber; bucket++) {
            showProgress("Bucket " + (bucket + 1) + " of " + bucketsNumber + ": loading... ");
            SuffixArray sa = BucketsSorter.loadSuffixArrayBucket(fullString, sortedBucketsDir.get(), bucket, bucketCharsNumber);

            addProgress("searching overlaps... ");
            findOverlaps(bucket, readsByBucket[bucket], sa, null);
            progress.updateDoneTasks(bucket + 1);
        }
        clearProgress();

        info("Searching overlaps finished in " + gt);
    }

    ArrayList<Integer>[] divideReadsIntoBuckets() {
        ArrayList<Integer>[] readsByBucket = new ArrayList[bucketsNumber];
        for (int i = 0; i < bucketsNumber; i++) {
            readsByBucket[i] = new ArrayList<Integer>();
        }
        for (int read = 0; read < readsNumber; read++) {
            long rb = readBegin[read];
            int bucket = BucketsDivider.getBucketNumber(fullString, rb, bucketCharsNumber);
            readsByBucket[bucket].add(read);
        }
        return readsByBucket;
    }


    /**
     * Finds overlaps in one bucket. <br>
     * if (overlaps != null) then found overlaps are saved to overlaps class,
     * else found overlaps are written to files.<br>
     *  
     * Attention! It doesn't try to fix errors in prefix!
     */
    void findOverlaps(int bucket, ArrayList<Integer> reads, SuffixArray sa, Overlaps overlaps) throws IOException, InterruptedException {
        debug("Bucket = " + bucket + ": reads size = " + groupDigits(reads.size()) + ", " +
                                            "SA length = " + groupDigits(sa.length));

        Collections.sort(reads, new ReadsComparator());

        PatientExecutorService executor = new PatientExecutorService(availableProcessors.get());

        BufferDedicatedWriter writer = null;
        if (overlaps == null) {
            writer = new BufferDedicatedWriter(overlapsDir.get().toString() + File.separator +
                    getOverlapsFileName(bucket, bucketCharsNumber));
            new Thread(writer).start();
        }
        OverlapTaskContext overlapTaskContext = new OverlapTaskContext(
                fullString,
                reads,
                readBegin,
                readsNumber,
                realReadsNumber,
                sa,
                executor,
                writer,
                overlaps,
                errorsNumber.get(),
                errorsWindowSize.get(),
                minOverlap.get()
        );

        IntervalList readsInterval = new IntervalList();
        int[] lastErrors = new int[errorsNumber.get()];
        Arrays.fill(lastErrors, -errorsWindowSize.get());
        readsInterval.add(0, reads.size(), lastErrors);

        executor.execute(new OverlapTask(overlapTaskContext, 0, sa.length, readsInterval, 0, 5));

        executor.waitForShutdown();

        if (writer != null) {
            writer.close();
        }
        foundOverlaps.addAndGet(overlapTaskContext.foundOverlaps.get());
    }
    


    private class ReadsComparator extends IntComparator {
        public int compare(int a, int b) {
            long aLen = readBegin[a + 1] - readBegin[a];
            long bLen = readBegin[b + 1] - readBegin[b];
            long aBegin = readBegin[a];
            long bBegin = readBegin[b];
            long len = Math.min(aLen, bLen);
            for (long i = 0; i < len; ++i) {
                int ca = fullString.get(aBegin + i);
                int cb = fullString.get(bBegin + i);
                if (ca != cb)
                    return ca - cb;
            }
            if (aLen == bLen)
                return 0;
            return (aLen < bLen) ? -1 : 1;
        }

    }

    public static String getOverlapsFileName(int bucket, int charsNumber) {
        return "overlaps" + BucketsDivider.getBucketName(bucket, charsNumber) + ".raw";
    }


    public static <T extends LightDna> boolean checkErrorsNumber(T a, T b, int en, int ews) {
        return checkErrorsNumber(a, b, 0, 0, Math.min(a.length(), b.length()), en, ews);
    }

    public static <T extends LightDna> boolean checkErrorsNumber(T a, T b, int aBegin, int bBegin, int len, int en, int ews) {
        List<Integer> errors = new ArrayList<Integer>();
        for (int i = 0; i < len; i++) {
            if (a.nucAt(aBegin + i) != b.nucAt(bBegin + i)) {
                errors.add(i);
            }
        }
        return checkErrorsNumber(errors, en, ews);
    }

    public static boolean checkErrorsNumber(String a, String b, int en, int ews) {
        return checkErrorsNumber(a, b, 0, 0, Math.min(a.length(), b.length()), en, ews);
    }

    public static boolean checkErrorsNumber(String a, String b, int aBegin, int bBegin, int len, int en, int ews) {
        List<Integer> errors = new ArrayList<Integer>();
        for (int i = 0; i < len; i++) {
            if (a.charAt(aBegin + i) != b.charAt(bBegin + i)) {
                errors.add(i);
            }
        }
        return checkErrorsNumber(errors, en, ews);
    }

    public static boolean checkErrorsNumber(List<Integer> errors, int en, int ews) {
        for (int i = 0; i + en < errors.size(); i++) {
            int ll = errors.get(i);
            int rr = errors.get(i + en);
            // There are (en+1) errors in [ll, rr]

            if (rr - ll + 1 <= ews) {
                return false;
            }
        }
        return true;
    }



    @Override
    protected void cleanImpl() {
        fullString = null;
        readBegin = null;
    }

    public Overlapper() {
        super(NAME, DESCRIPTION);
    }

}
