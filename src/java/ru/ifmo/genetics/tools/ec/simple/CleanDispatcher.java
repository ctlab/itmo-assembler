package ru.ifmo.genetics.tools.ec.simple;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongList;
import ru.ifmo.genetics.tools.io.LazyLongReader;
import ru.ifmo.genetics.utils.KmerUtils;
import ru.ifmo.genetics.utils.tool.Progress;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CleanDispatcher {
    LazyLongReader reader;
    int workRangeSize;
    long bad, total = 0;
    int LEN;
    int threadsNumber;
    Progress progress;

    public CleanDispatcher(LongCollection badKmers, int workRangeSize) {
        tasks = new ArrayList<List<Long>>();

        List<Long> task = new ArrayList<Long>(workRangeSize);
        for (long str : badKmers) {
            task.add(str);
            if (task.size() == workRangeSize) {
                tasks.add(task);
                task = new ArrayList<Long>(workRangeSize);
            }
        }

        if (task.size() != 0) {
            tasks.add(task);
        }

    }

    public CleanDispatcher(LazyLongReader reader, int workRangeSize, long bad, int LEN, int threadsNumber,
                           Progress progress)
            throws FileNotFoundException, EOFException {
        this.reader = reader;
        this.workRangeSize = workRangeSize;
        this.bad = bad;
        this.LEN = LEN;
        this.threadsNumber = threadsNumber;
        this.progress = progress;

        progress.setTotalTasks(bad << 1);
        progress.createProgressBar();
    }

    private LongList readRange(LazyLongReader reader, int workRangeSize) throws IOException {
        LongList list = new LongArrayList(workRangeSize);
        while (list.size() < workRangeSize) {
            try {
                long fw, rc;
                synchronized (reader) {
                    fw = reader.readLong();
                }
                rc = KmerUtils.reverseComplement(fw, LEN);
                list.add(fw);
                list.add(rc);
            } catch (EOFException e) {
                break;
            }
        }
        return list;
    }

    List<List<Long>> tasks;

    public LongList getWorkRange() {
        LongList list;
        try {
            synchronized (reader) {
                list = readRange(reader, workRangeSize);
            }
            total += workRangeSize;
            progress.updateDoneTasks(Math.max(0, total - threadsNumber * workRangeSize));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return list.isEmpty() ? null : list;
    }

}
