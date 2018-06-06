package ru.ifmo.genetics.tools;

import com.clearspring.analytics.stream.cardinality.CardinalityMergeException;
import com.clearspring.analytics.stream.cardinality.HyperLogLog;
import com.clearspring.analytics.stream.cardinality.ICardinality;
import org.apache.commons.lang.mutable.MutableLong;
import org.jetbrains.annotations.Nullable;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.DnaView;
import ru.ifmo.genetics.io.readers.BinqReader;
import ru.ifmo.genetics.executors.TaskWithSharedContext;
import ru.ifmo.genetics.executors.ThreadExecutorWithSharedContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ApproximateKmerCounter {
    public final static int minK = 21;
    public final static int maxK = 31;
    public final static int kd = 10;

    public static void main(String[] args) throws InterruptedException, IOException {

        CountingContext result = new CountingContext();
        ThreadExecutorWithSharedContext<CountingContext> executor = new ThreadExecutorWithSharedContext<CountingContext>(6);
        final int TASK_SIZE = 1 << 10;
        ArrayList<DnaQ> task = new ArrayList<DnaQ>(TASK_SIZE);
        long dnaQsNumber = 0;
        long sumLength = 0;
        ArrayList<MutableLong> lengthsStat = new ArrayList<MutableLong>();

        for (String file: args) {
//            FastqReader reader = new FastqReader(new File(file), Sanger.instance);
            BinqReader reader = new BinqReader(new File(file));
            for (DnaQ dnaq: reader) {
                task.add(dnaq);
                dnaQsNumber++;
                sumLength += dnaq.length();
                if (dnaq.length() >= lengthsStat.size()) {
                    for (int i = lengthsStat.size(); i <= dnaq.length(); ++i) {
                        lengthsStat.add(new MutableLong());
                    }
                }
                lengthsStat.get(dnaq.length()).increment();

                if (task.size() == TASK_SIZE) {
//                    executor.blockingExecute(new CountingTask(task, result));
                    executor.blockingExecute(new CountingTask(task));
                    task = new ArrayList<DnaQ>(TASK_SIZE);
                }
                /*
                String sRc = DnaTools.toString(DnaView.rcView(dnaq));
                for (int k = minK; k <= maxK; k += kd) {
                    for (int i = 0; i + k < s.length(); ++i) {
                        counters[k].offer(sRc.substring(i, i + k));
                    }
                }
                */
            }
        }

        if (!task.isEmpty()) {
            executor.blockingExecute(new CountingTask(task));
        }

        System.out.println("reads number: " + dnaQsNumber);
        System.out.println("total length: " + sumLength);

        executor.shutdownAndAwaitTermination();

//        System.out.println(x);
        System.out.println("number of k-mers:");

        for (CountingContext resultPart: executor.getContexts()) {
            try {
                result = result.merge(resultPart);
            } catch (CardinalityMergeException e) {
                throw new RuntimeException(e);
            }
        }

        long genomeSize = 0;
        for (int k = minK; k<= maxK; k += kd) {
            long cardinality = result.counters[k].cardinality();
            ICardinality counterWithRc;
            counterWithRc = result.counters[k].merge(result.rcCounters[k]);

            long cardinalityWithRc = counterWithRc.cardinality();

            long currentGenomeEstimation = (2 * cardinality - cardinalityWithRc) / 2;
            if (currentGenomeEstimation > genomeSize) {
                genomeSize = currentGenomeEstimation;
            }
            System.out.println(k + ":\t" + cardinality + "\t" + cardinalityWithRc + "\t" + currentGenomeEstimation);
        }

        System.out.println("Estimated genome size: " + genomeSize);
        System.out.println("Read length distribution:");
        for (int i = 0; i < lengthsStat.size(); ++i) {
            System.out.println(i + "\t" + lengthsStat.get(i));
        }
        double averageStartDistance = genomeSize / (double) dnaQsNumber;
        System.out.println("Average distance between starts: " + averageStartDistance);
        int maxLength = lengthsStat.size() - 1;

        System.err.println("Probability of having overlap of length k:");
        for (int k = 15; k < 91; ++k) {
            double prob = 0;
            double dprob = Math.exp(-averageStartDistance);
            for (int d = 0; d <= maxLength; ++d) {
                if (d > 0) {
                    dprob *= averageStartDistance / d;
                }
                long t = 0;
                for (int l1 = d + k; l1 <= maxLength; ++l1) {
                    for (int l2 = k; l2 <= maxLength; ++l2) {
                        t += lengthsStat.get(l1).longValue() * lengthsStat.get(l2).longValue();
                    }
                }
                prob += dprob * t / dnaQsNumber /dnaQsNumber;
            }
            System.out.println(k + "\t" + prob + "\t" + k * prob);
        }


    }

    public static class CountingContext {
        public final HyperLogLog[] counters;
        public final HyperLogLog[] rcCounters;

        public CountingContext(HyperLogLog[] counters, HyperLogLog[] rcCounters) {
            this.counters = counters;
            this.rcCounters = rcCounters;
        }

        public CountingContext() {
            this(new HyperLogLog[maxK + 1], new HyperLogLog[maxK + 1]);
            for (int i = minK; i <= maxK; i += kd) {
                counters[i] = new HyperLogLog(20);
            }

            for (int i = minK; i <= maxK; i += kd) {
                rcCounters[i] = new HyperLogLog(20);
            }
        }

        public CountingContext merge(CountingContext other) throws CardinalityMergeException {
            HyperLogLog[] newCounters = new HyperLogLog[maxK + 1];
            HyperLogLog[] newRcCounters = new HyperLogLog[maxK + 1];
            for (int i = minK; i <= maxK; i += kd) {
                newCounters[i] = (HyperLogLog) counters[i].merge(other.counters[i]);
            }

            for (int i = minK; i <= maxK; i += kd) {
                newRcCounters[i] = (HyperLogLog) rcCounters[i].merge(other.rcCounters[i]);
            }
            return new CountingContext(newCounters, newRcCounters);
        }
    }

    public static class CountingTask implements TaskWithSharedContext<CountingContext> {

        private Iterable<? extends DnaQ> task;
        private @Nullable CountingContext context;

        public CountingTask(Iterable<? extends DnaQ> task) {
            this.task = task;
        }

        public CountingTask(Iterable<DnaQ> task, CountingContext context) {
            this.task = task;
            this.context = context;
        }

        @Override
        public void run() {
            if (context == null) {
                context = new CountingContext();
            }

            for (DnaQ dnaq: task) {
                String s = DnaTools.toString(dnaq);
                for (int k = minK; k <= maxK; k += kd) {
                    for (int i = 0; i + k < s.length(); ++i) {
                        context.counters[k].offer(s.substring(i, i + k));
                    }
                }

                String sRc = DnaTools.toString(DnaView.rcView(dnaq));
                for (int k = minK; k <= maxK; k += kd) {
                    for (int i = 0; i + k < s.length(); ++i) {
                        context.rcCounters[k].offer(sRc.substring(i, i + k));
                    }
                }
            }
        }

        @Nullable
        public CountingContext getContext() {
            return context;
        }

        public void setContext(@Nullable CountingContext context) {
            if (context == null) {
                return;
            }
            this.context = context;
        }
    }
}
