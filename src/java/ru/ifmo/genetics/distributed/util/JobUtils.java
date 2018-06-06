package ru.ifmo.genetics.distributed.util;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;

import java.io.IOException;

public class JobUtils {
    public static boolean jobSucceededOrRemove(Path jobPath) throws IOException {
        FileSystem fs = FileSystem.get(new JobConf());
        boolean succeeded = fs.exists(new Path(jobPath, "_SUCCESS"));
        if (!succeeded) {
            fs.delete(jobPath, true);
        }
        return succeeded;
    }

    public static void remove(Path jobPath) throws IOException {
        FileSystem fs = FileSystem.get(new JobConf());
        fs.delete(jobPath, true);
    }
}
