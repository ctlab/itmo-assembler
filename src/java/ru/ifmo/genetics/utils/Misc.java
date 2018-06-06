package ru.ifmo.genetics.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang.mutable.MutableLong;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.utils.pairs.UniPair;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Misc {
    public static long availableMemory() {
        Runtime rt = Runtime.getRuntime();
        System.gc();
        return rt.maxMemory() - rt.totalMemory() + rt.freeMemory();
    }
    public static long availableMemoryWithoutRunningGC() {
        Runtime rt = Runtime.getRuntime();
        return rt.maxMemory() - rt.totalMemory() + rt.freeMemory();
    }
    public static long usedMemory() {
        Runtime rt = Runtime.getRuntime();
        System.gc();
        return rt.totalMemory() - rt.freeMemory();
    }
    public static long usedMemoryWithoutRunningGC() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }


    public static String availableMemoryAsString() {
        return NumUtils.memoryAsString(availableMemory());
    }
    public static String availableMemoryWithoutRunningGCAsString() {
        return NumUtils.memoryAsString(availableMemoryWithoutRunningGC());
    }
    public static String usedMemoryAsString() {
        return NumUtils.memoryAsString(usedMemory());
    }
    public static String usedMemoryWithoutRunningGCAsString() {
        return NumUtils.memoryAsString(usedMemoryWithoutRunningGC());
    }

    public static String availableMemoryAsStringForJVM() {
        long mem = availableMemory();
        mem /= 1024;    // now in kb
        mem /= 1024;    // now in mb

        if ((mem < 1024) || (mem < 5 * 1024)) {
            return mem + "M";
        }
        mem /= 1024;    // now in gb
        return mem + "G";
    }

    

    public static long getPrefixCode(String prefix, int len) {
        long res = 0;
        for (int i = 0; i < prefix.length(); i++) {
            res = (res << 2) | DnaTools.fromChar(prefix.charAt(i));
        }
        res = res << (2 * (len - prefix.length()));
        return res;
    }

    public static long getPrefixMask(String prefix, int len) {
        long res = 0;
        for (int i = 0; i < prefix.length(); i++) {
            res = (res << 2) | 3;
        }
        res = res << (2 * (len - prefix.length()));
        return res;
    }

    public static long getCode(String string) {
        return getPrefixCode(string, string.length());
    }

    public static String getString(long kmer, int len) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < len; ++i) {
            long c = (kmer >> (2 * (len - i - 1))) & 3;
            sb.append(DnaTools.toChar((byte)c));
        }
        return sb.toString();
    }

    public static String getFixDescription(long fix, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; ++i) {
            long cfix = (fix >> (8 * i)) & 255;
            if (cfix == 0) {
                continue;
            }
            long type = cfix >> 5;
            long pos = len - (cfix & 31);
            if (type == 0) {
                sb.append("dup at pos " + pos + "\n");
            } else if (type == 4) {
                sb.append("del at pos " + pos + "\n");
            } else {
                sb.append("^ " + type + " at pos " + pos + "\n");
            }
        }
        return sb.toString();
    }

    public static <K> void addInt(Map<K, Integer> map, K key, int value) {
        Integer t = map.get(key);
        if (t == null)
            t = 0;
        map.put(key, NumUtils.addAndBound(t,value));
    }

    public static <K> void addLong(Map<K, Long> map, K key, long value) {
        Long t = map.get(key);
        if (t == null)
            t = 0L;
        map.put(key, NumUtils.addAndBound(t, value));
    }

    public static <K> void addMutableInt(Map<K, MutableInt> map, K key, int value) {
        MutableInt t = map.get(key);
        if (t == null) {
            t = new MutableInt();
            map.put(key, t);
        }
        t.add(value);
    }

    public static <K> void addMutableLong(Map<K, MutableLong> map, K key, long value) {
        MutableLong t = map.get(key);
        if (t == null) {
            t = new MutableLong();
            map.put(key, t);
        }
        t.add(value);
    }

    public static <K> void incrementInt(Map<K, Integer> map, K key) {
        addInt(map, key, 1);
    }

    public static <K> void incrementLong(Map<K, Long> map, K key) {
        addLong(map, key, 1L);
    }

    public static long DnaQ2Long(DnaQ d) {
        int l = d.length();
        long r = 0;
        for (int i = 0; i < l; ++i) {
            r = (r << 2) | (d.byteAt(i) & 3);
        }
        return r;
    }

    public static String join(Iterable<String> ss, String delimiter) {
        StringBuilder sb = new StringBuilder();
        boolean firstly = true;
        for (String s : ss) {
            if (!firstly) {
                sb.append(delimiter);
            }
            sb.append(s);
            firstly = false;
        }
        return sb.toString();
    }

    public static String join(String[] ss, String delimiter) {
        return join(Arrays.asList(ss), delimiter);
    }

    public static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            while (!pool.awaitTermination(60, TimeUnit.SECONDS)) ;
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public static int sumOfDnaLengths(LightDna... dnas) {
        int sum = 0;
        for (LightDna dna : dnas) {
            sum += dna.length();
        }
        return sum;
    }

    public static <E> Iterable<E> extractFirsts(final Iterable<UniPair<E>> pairs) {
        return new Iterable<E>() {
            public Iterator<E> iterator() {
                return new Iterator<E>() {
                    Iterator<UniPair<E>> pairIt = pairs.iterator();

                    public boolean hasNext() {
                        return pairIt.hasNext();
                    }

                    public E next() {
                        return pairIt.next().first;
                    }

                    public void remove() {
                        pairIt.remove();
                    }
                };
            }
        };
    }

    public static <E> Iterable<E> extractSeconds(final Iterable<UniPair<E>> pairs) {
        return new Iterable<E>() {
            public Iterator<E> iterator() {
                return new Iterator<E>() {
                    Iterator<UniPair<E>> pairIt = pairs.iterator();

                    public boolean hasNext() {
                        return pairIt.hasNext();
                    }

                    public E next() {
                        return pairIt.next().second;
                    }

                    public void remove() {
                        pairIt.remove();
                    }
                };
            }
        };
    }

    // replaces "-" with "_"
    public static void addOptionToConfig(CommandLine cmd, Configuration config, String option) {
        addOptionToConfig(cmd, config, option, option.replace("-", "_"));
    }

    public static void addOptionToConfig(
            CommandLine cmd,
            Configuration config,
            String option,
            String property) {
        if (cmd.hasOption(option)) {
            String optionValue = cmd.getOptionValue(option);
            if (optionValue == null) {
                // option doesn't have an argument
                config.setProperty(property, true);
            } else {
                config.setProperty(property, optionValue);
            }
        }
    }

    public static Configuration mergeConfigurations(Configuration config, Configuration globalConfig) {
        Configuration result = new MapConfiguration(new HashMap());

        addAllWithReplacement(result, globalConfig);
        addAllWithReplacement(result, config);

        return result;
    }

    public static void addAllWithReplacement(Configuration toConfig, Configuration fromConfig) {
        if (fromConfig == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Iterator<String> iterator = fromConfig.getKeys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            toConfig.addProperty(key, fromConfig.getProperty(key));
        }
    }

    public static int compare(boolean a, boolean b) {
        return (a == b) ? 0 : (a ? 1 : -1);
    }



    public static boolean checkEquals(Object[] v1, Object[] v2) {
        if (v1 == v2) {
            return true;
        }
        if (v1 == null || v2 == null) {
            return false;
        }
        if (v1.length != v2.length) {
            return false;
        }

        boolean equals = true;
        for (int i = 0; i < v1.length; i++) {
            equals &= checkEquals(v1[i], v2[i]);
        }
        return equals;
    }

    public static boolean checkEquals(Object v1, Object v2) {
        if (v1 == v2) {
            return true;
        }
        if (v1 == null || v2 == null) {
            return false;
        }

        if (v1 instanceof File) {
            return ((File) v1).getAbsolutePath().equals(((File) v2).getAbsolutePath());
        }

        return v1.equals(v2);
    }

    public static long longHashCode(int[] array) {
        long res = 0;
        for (int anArray : array) {
            res *= 1000000009;
            res += anArray;
        }
        return res;
    }

}
