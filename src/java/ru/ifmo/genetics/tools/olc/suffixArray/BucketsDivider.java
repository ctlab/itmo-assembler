package ru.ifmo.genetics.tools.olc.suffixArray;

import ru.ifmo.genetics.statistics.Timer;
import ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString;
import ru.ifmo.genetics.tools.olc.arrays.FiveByteArrayWriter;
import ru.ifmo.genetics.utils.FileUtils;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;
import ru.ifmo.genetics.utils.tool.values.Yielder;

import java.io.File;
import java.io.IOException;

import static ru.ifmo.genetics.tools.olc.gluedDnasString.GluedDnasString.ALPHABET;
import static java.lang.Math.log;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static ru.ifmo.genetics.utils.NumUtils.groupDigits;

public class BucketsDivider extends Tool {
    public static final String NAME = "buckets-divider";
    public static final String DESCRIPTION = "divides all suffix array into buckets";


    // input params
    public final Parameter<File> fullStringFile = addParameter(new FileParameterBuilder("full-string-file")
            .mandatory()
            .withDescription("file with glued dnas string")
            .create());

    public final Parameter<File> bucketsDir = addParameter(new FileParameterBuilder("buckets-dir")
            .optional()
            .withDefaultValue(workDir.append("buckets"))
            .withDescription("directory with divided buckets")
            .create());


    private Yielder<Integer> bucketCharsNumberYielder = new Yielder<Integer>() {
        @Override
        public Integer yield() {
            if (fullString == null) {
                return null;
            }

            double len = fullString.length;
            double mem = Misc.availableMemory();

            long A = (long) ceil(5 * len / (mem * 0.9 - 100e6));
            long B = (long) ceil(len / 2e9);
            
            int k = (int) max(0, ceil(log(max(A, B)) / log(4)));

            return k;
        }
        @Override
        public String description() {
            return "auto";
        }
    };

    public final Parameter<Integer> bucketCharsNumberIn = addParameter(new IntParameterBuilder("bucket-chars-number")
            .optional()
            .withDefaultValue(bucketCharsNumberYielder)
            .withDescription("bucket chars number")
            .create());


    // internal variables
    private int bucketCharsNumber;
    private int bucketsNumber;
    private GluedDnasString fullString;


    // output params
    private final InMemoryValue<Integer> bucketCharsNumberOutValue = new InMemoryValue<Integer>();
    public final InValue<Integer> bucketCharsNumberOut = addOutput("bucket-chars-number", bucketCharsNumberOutValue, Integer.class);
    private final InMemoryValue<Integer> bucketsNumberOutValue = new InMemoryValue<Integer>();
    public final InValue<Integer> bucketsNumberOut = addOutput("buckets-number", bucketsNumberOutValue, Integer.class);



    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            info("Loading full string...");
            fullString = new GluedDnasString(fullStringFile.get());
            debug("fullString.len = " + groupDigits(fullString.length));

            bucketCharsNumber = bucketCharsNumberIn.get();
            bucketsNumber = (int) Math.pow(ALPHABET, bucketCharsNumber);
            info("bucket chars number = " + bucketCharsNumber + ", all buckets number = " + bucketsNumber);

            divideIntoBuckets();

            bucketCharsNumberOutValue.set(bucketCharsNumber);
            bucketsNumberOutValue.set(bucketsNumber);

        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
    }



    /**
     * Divides all fullString suffixes into buckets and writes them to files bucketX.high/low in dir.
     */
    private void divideIntoBuckets() throws IOException {
        info("Dividing into buckets...");
        Timer t = new Timer();

        File dir = bucketsDir.get();
        FileUtils.createOrClearDir(dir);
        
        FiveByteArrayWriter[] writers = new FiveByteArrayWriter[bucketsNumber];
        for (int i = 0; i < bucketsNumber; i++) {
            writers[i] = new FiveByteArrayWriter(dir.toString() + File.separator + 
                    getBucketFileName(i, bucketCharsNumber));
        }
        
        int[] count = new int[bucketsNumber];
        progress.setTotalTasks(fullString.length);
        progress.createProgressBar();
        for (long i = 0; i < fullString.length; i++) {
            int bn = getBucketNumber(i);
            if (count[bn] == Integer.MAX_VALUE) {
                throw new RuntimeException("Size of bucket " + bn + " is larger than Integer.MAX_VALUE ! " +
                        "Increase bucket-chars-number parameter!");
            }
            count[bn]++;

            writers[bn].write(i);

            progress.updateDoneTasks(i);
        }
        progress.destroyProgressBar();

        for (int i = 0; i < bucketsNumber; i++) {
            writers[i].close();
        }
        
        info("Done, it took " + t);
    }


    private int getBucketNumber(long pos) {
        return getBucketNumber(fullString, pos, bucketCharsNumber);
    }

    public static int getBucketNumber(GluedDnasString fullString, long startPosInText, int charsNumber) {
        assert charsNumber <= GluedDnasString.nucsInLong;

        long nucs = fullString.getPackWithLastZeros(startPosInText);
        int res = 0;
        for (int i = 0; i < charsNumber; i++) {
            int nuc = GluedDnasString.getFirstNucFromPack(nucs);
            nucs <<= GluedDnasString.nucWidth;

            assert (nuc >= 0) && (nuc < ALPHABET);
            res = res * ALPHABET + nuc;
        }
        return res;
    }
    
    public static String getBucketFileName(int bucket, int charsNumber) {
        return "bucket" + getBucketName(bucket, charsNumber);
    }

    public static String getBucketName(int bucket, int charsNumber) {
        String s = "";
        for (int i = 0; i < charsNumber; i++) {
            int curChar = bucket % ALPHABET;
            s = (char) GluedDnasString.charCodes[curChar] + s;
            bucket /= ALPHABET;
        }
        return s;
    }

    static final byte $code = GluedDnasString.$index;
    /**
     * Checks if bucket's chars specified by bucket number contain char '$'.
     */
    public static boolean bucketCharsContain$(int bucketN, int charsNumber) {
        for (int i = 0; i < charsNumber; i++) {
            int curChar = bucketN % ALPHABET;
            if (curChar == $code) {
                return true;
            }
            bucketN /= ALPHABET;
        }
        return false;
    }


    @Override
    protected void cleanImpl() {
        fullString = null;
    }

    public BucketsDivider() {
        super(NAME, DESCRIPTION);
    }

}
