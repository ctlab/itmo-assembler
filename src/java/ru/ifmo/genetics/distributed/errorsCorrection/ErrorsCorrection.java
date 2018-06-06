package ru.ifmo.genetics.distributed.errorsCorrection;

import org.apache.hadoop.fs.Path;
import ru.ifmo.genetics.distributed.errorsCorrection.tasks.FixesApplier;
import ru.ifmo.genetics.distributed.errorsCorrection.tasks.FixesFinder;
import ru.ifmo.genetics.distributed.util.JobUtils;

import java.io.IOException;

public class ErrorsCorrection {

    public static void main(String[] args) throws Exception {
        final Path source2Binq = new Path(args[0]);
        final Path workDir = new Path(args[1]);
        makeErrorCorrection(source2Binq, workDir, 25, 9);
    }

    public static Path makeErrorCorrection(Path source2Binq, Path workDir, int kmerLength, int prefixLength) throws IOException, ClassNotFoundException, InterruptedException {

        final Path kmersFixes = new Path(workDir, "1_kmersFixes");
        final Path fixedReads = new Path(workDir, "2_fixedReads");

        if (!JobUtils.jobSucceededOrRemove(kmersFixes)) {
            FixesFinder.findKmersFixes(source2Binq, kmersFixes, kmerLength, prefixLength);
        }
        if (!JobUtils.jobSucceededOrRemove(fixedReads)) {
            FixesApplier.applyFixes(kmersFixes, source2Binq, fixedReads);
        }
        return fixedReads;
    }
}
