package ru.ifmo.genetics.tools.olc.suffixArray;

import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.executors.PatientExecutorService;
import ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.File;
import java.io.IOException;

import static ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString.$index;
import static ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString.ALPHABET;

public class BucketsSorter extends Tool {
    public static final String NAME = "buckets-sorter";
    public static final String DESCRIPTION = "sorts all suffixes in suffix array";

    // input params
    public final Parameter<File> fullStringFile = addParameter(new FileParameterBuilder("full-string-file")
            .mandatory()
            .withDescription("file with glued dnas string")
            .create());

    public final Parameter<File> bucketsDir = addParameter(new FileParameterBuilder("buckets-dir")
            .mandatory()
            .withDescription("directory with buckets to sort")
            .create());

    public final Parameter<File> sortedBucketsDir = addParameter(new FileParameterBuilder("sorted-buckets-dir")
            .optional()
            .withDefaultValue(workDir.append("sorted-buckets"))
            .withDescription("directory with sorted buckets")
            .create());

    public final Parameter<Integer> bucketCharsNumberIn = addParameter(new IntParameterBuilder("bucket-chars-number")
            .mandatory()
            .withDescription("bucket chars number")
            .create());

    public final Parameter<Integer> smallBucketCharsNumberIn = addParameter(new IntParameterBuilder("small-bucket-chars-number")
            .optional()
            .withDefaultValue(5)
            .withDescription("small bucket chars number (using to divide bucket into small buckets)")
            .create());


    // internal variables
    private int bucketCharsNumber;
    private int bucketsNumber;
    private int smallBucketCharsNumber;
    private int smallBucketsNumber;
    private GluedDnasString fullString;


    // output params
    private final InMemoryValue<Integer> smallBucketsNumberOutValue = new InMemoryValue<Integer>();
    public final InValue<Integer> smallBucketsNumberOut = addOutput("small-buckets-number", smallBucketsNumberOutValue, Integer.class);



    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            bucketCharsNumber = bucketCharsNumberIn.get();
            bucketsNumber = (int) Math.pow(ALPHABET, bucketCharsNumber);
            smallBucketCharsNumber = smallBucketCharsNumberIn.get();
            smallBucketsNumber = (int) Math.pow(ALPHABET, smallBucketCharsNumber);
            progress.setTotalTasks(bucketsNumber);

            info("Loading full string...");
            fullString = new GluedDnasString(fullStringFile.get());

            sortAllBuckets();

            smallBucketsNumberOutValue.set(smallBucketsNumber);
        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            throw new ExecutionFailedException(e);
        }
    }


    /**
     * Loads all suffix buckets, sorts and saves them to files sbucketX.high/low in dir.
     */
    private void sortAllBuckets() throws IOException, InterruptedException {
        info("Sorting all buckets...");
        Timer t = new Timer();

        FileUtils.createOrClearDir(sortedBucketsDir.get());

        for (int i = 0; i < bucketsNumber; i++) {
            showProgress("Bucket " + (i + 1) + " of " + bucketsNumber + ": loading... ");
            String bucketFileName = BucketsDivider.getBucketFileName(i, bucketCharsNumber);
            SuffixArray sa = new SuffixArray(fullString, bucketsDir.get() + File.separator + bucketFileName);

            addProgress("sorting... ");
            sort(sa);

            addProgress("saving... ");
            sa.save(sortedBucketsDir.get() + File.separator + bucketFileName);
            sa = null;
            progress.updateDoneTasks(i + 1);
        }
        clearProgress();
        
        info("Done, it took " + t);
    }

    /**
     * Sorts all suffixes in suffix array.
     */
    public void sort(SuffixArray sa) throws InterruptedException {
        debug("SuffixArray.len = " + sa.length);

        PatientExecutorService executor = new PatientExecutorService(availableProcessors.get());
        bucketSort(executor, sa, 0, sa.length, bucketCharsNumber,
                2   // small bucket chars number, only for first run
        );
        executor.waitForShutdown();
    }

    private void sortWithForking(final PatientExecutorService executor, final SuffixArray sa, final int begin,
                                 final int len, final int charsSorted) {


        if (len < 500) {
            quickSort(sa, begin, len, charsSorted);
        } else {
            // using bucket sort

            int smallBucketCharsNumber = this.smallBucketCharsNumber;
            if (len < Math.pow(ALPHABET, smallBucketCharsNumber) / 5) {
                smallBucketCharsNumber = 3;
            }

            if (len > 10000) {
                final int sBCN = smallBucketCharsNumber;
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        bucketSort(executor, sa, begin, len, charsSorted, sBCN);
                    }
                });
            } else {
                bucketSort(executor, sa, begin, len, charsSorted, smallBucketCharsNumber);
            }
        }
    }

    /**
     * Sorts large arrays
     */
    private void bucketSort(final PatientExecutorService executor, final SuffixArray sa, int begin, int len,
                            int charsSorted, int smallBucketCharsNumber) {
        int smallBucketsNumber = (int) Math.pow(ALPHABET, smallBucketCharsNumber);

        // calculation
        int[] count = new int[smallBucketsNumber];
        for (int i = begin; i < begin + len; i++) {
            int b = BucketsDivider.getBucketNumber(sa.text, sa.get(i) + charsSorted, smallBucketCharsNumber);
            count[b]++;
        }

        int[] bi = new int[smallBucketsNumber];
        bi[0] = begin;
        for (int i = 1; i < smallBucketsNumber; i++) {
            bi[i] = bi[i - 1] + count[i - 1];
        }
        int[] cbi = bi.clone();

        int[] ei = new int[smallBucketsNumber];
//        int maxCount = 0;
        for (int i = 0; i < smallBucketsNumber; i++) {
            ei[i] = bi[i] + count[i];
//            maxCount = Math.max(maxCount, count[i]);
        }

        // reordering
        int firstBN = 0;
        while (firstBN < smallBucketsNumber) {
            if (cbi[firstBN] == ei[firstBN]) {
                firstBN++;
                continue;
            }

            long cur = sa.get(cbi[firstBN]);
            int bn = BucketsDivider.getBucketNumber(sa.text, cur + charsSorted, smallBucketCharsNumber);
            while (bn != firstBN) {
                long ncur = sa.get(cbi[bn]);
                sa.set(cbi[bn], cur);
                cbi[bn]++;
                cur = ncur;
                bn = BucketsDivider.getBucketNumber(sa.text, cur + charsSorted, smallBucketCharsNumber);
            }
            sa.set(cbi[firstBN], cur);
            cbi[firstBN]++;
        }

        charsSorted += smallBucketCharsNumber;

        // sorting in small buckets
        for (int i = 0; i < smallBucketsNumber; i++) {
            if ((ei[i] - bi[i]) > 1) {
                boolean bucketCharsContain$ = BucketsDivider.bucketCharsContain$(i, smallBucketCharsNumber);
                if (!bucketCharsContain$) {
                    sortWithForking(executor, sa, bi[i], ei[i] - bi[i], charsSorted);
                }
            }
        }
    }

    private void quickSort(final SuffixArray sa, int begin, int len, int charsSorted) {
        // Insertion sort on smallest arrays
        if (len <= 7) {
            for (int i = begin; i < begin + len; i++)
                for (int j = i; j > begin && compare(sa.text, sa.get(j), sa.get(j - 1), charsSorted) < 0; j--)
                    sa.swap(j, j - 1);
            return;
        }

        // Choose a partition element, v
        int m = begin + (len >> 1); // middle element
        long v = sa.get(m);

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = begin, b = a, c = begin + len - 1, d = c;
        int cr;
        while (true) {
            while (b <= c &&
                    ((cr = compare(sa.text, sa.get(b), v, charsSorted)) <= 0)) {
                if (cr == 0)
                    sa.swap(a++, b);
                b++;
            }
            while (c >= b &&
                    ((cr = compare(sa.text, sa.get(c), v, charsSorted)) >= 0)) {
                if (cr == 0)
                    sa.swap(c, d--);
                c--;
            }
            if (b > c)
                break;
            sa.swap(b++, c--);
        }

        int s, n = begin + len;
        s = Math.min(a - begin, b - a);
        sa.vecswap(begin, b - s, s);
        s = Math.min(d - c, n - d - 1);
        sa.vecswap(b, n - s, s);

        if ((s = b - a) > 1) {
            quickSort(sa, begin, s, charsSorted);
        }
        if ((s = d - c) > 1) {
            quickSort(sa, n - s, s, charsSorted);
        }
    }


    private int compare(GluedDnasString text, long pos1, long pos2, int charsSorted) {
        return fastCompareTill$(text, pos1 + charsSorted, pos2 + charsSorted);
    }

    /**
     * @param pos1 < length
     * @param pos2 < length
     */
    public static int fastCompareTill$(GluedDnasString array, long pos1, long pos2) {
        if (pos1 == pos2) {
            return 0;
        }

        long b1 = array.getPackWithLastZeros(pos1);
        long b2 = array.getPackWithLastZeros(pos2);

        if (b1 != b2) {
            return (b1 < b2) ? -1 : 1;
        }
        if (GluedDnasString.nucsContain$(b1)) {
            return 0;
        }

        int firstAddValue = array.addValueToGoodPack(pos1);
        pos1 += firstAddValue;
        pos2 += firstAddValue;

        while (true) {
            b1 = array.getPackWithLastZeros(pos1);
            b2 = array.getPackWithLastZeros(pos2);

            if (b1 != b2) {
                return (b1 < b2) ? -1 : 1;
            }
            if (GluedDnasString.nucsContain$(b1)) {
                return 0;
            }

            pos1 += GluedDnasString.nucsInLong;
            pos2 += GluedDnasString.nucsInLong;
        }

    }

    /**
     * @param pos1 < length
     * @param pos2 < length
     */
    public static int compareTill$(GluedDnasString array, long pos1, long pos2) {
        if (pos1 == pos2) {
            return 0;
        }

        while (true) {
            int b1 = array.getWithLastZeros(pos1);
            int b2 = array.getWithLastZeros(pos2);

            if (b1 != b2) {
                return b1 - b2;
            }
            if (b1 == $index) {
                return 0;
            }

            pos1++;
            pos2++;
        }
    }
    

    public static SuffixArray loadSuffixArrayBucket(GluedDnasString fullString, File bucketsDir,
                                                    int bucket, int bucketCharsNumber) throws IOException {
        File f = new File(bucketsDir, BucketsDivider.getBucketFileName(bucket, bucketCharsNumber));
        SuffixArray sa = new SuffixArray(fullString, f.toString());
        return sa;
    }


    @Override
    protected void cleanImpl() {
        fullString = null;
    }

    public BucketsSorter() {
        super(NAME, DESCRIPTION);
    }
}
