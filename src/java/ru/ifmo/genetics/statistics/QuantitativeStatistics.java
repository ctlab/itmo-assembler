package ru.ifmo.genetics.statistics;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.Variance;
import ru.ifmo.genetics.utils.Misc;

import java.io.*;
import java.util.*;

/**
 * Class that can help you to answer the question
 *   "how mush times does certain element G present in input data?"
 */
public class QuantitativeStatistics<G> {
    TreeMap<G, Integer> map;
    
    public QuantitativeStatistics() {
        map = new TreeMap<G, Integer>();
    }
    public QuantitativeStatistics(Comparator<? super G> comparator) {
        map = new TreeMap<G, Integer>(comparator);
    }

    public void add(G g) {
        Misc.addInt(map, g, 1);
    }
    
    public void set(G g, int value) {
        map.put(g, value);
    }
    
    public int get(G g) {
        Integer v = map.get(g);
        if (v == null) {
            v = 0;
        }
        return v;
    }
    
    public Set<Map.Entry<G, Integer>> entrySet() {
        return map.entrySet();
    }
    public Set<G> keySet() {
        return map.keySet();
    }


    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean printZeroValues) {
        StringBuilder sb = new StringBuilder();
        if (!printZeroValues) {
            for (Map.Entry<G, Integer> e : map.entrySet()) {
                sb.append(e.getKey() + " : " + e.getValue());
                sb.append('\n');
            }
        } else {
            int first = (Integer) map.firstKey();
            int last = (Integer) map.lastKey();
            for (int i = first; i <= last; i++) {
                Integer v = map.get(i);
                if (v == null) {
                    v = 0;
                }
                sb.append(i + " : " + v);
                sb.append('\n');
            }
        }

        return sb.toString();
    }


    public void printToFile(String filename) {
        printToFile(filename, null, false);
    }

    public void printToFile(String filename, boolean printZeroValues) {
        printToFile(filename, null, printZeroValues);
    }

    /**
     * @param headerString may be null, in such case no header string are written to file
     */
    public void printToFile(String filename, String headerString, boolean printZeroValues) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(filename);
        } catch (FileNotFoundException e) {
            System.err.println("Can't create file: " + e);
            return;
        }

        if (headerString != null) {
            out.println(headerString);
        }
        out.println(this.toString(printZeroValues));
        out.close();
    }



    public G getMaxValuePosition() {
        G result = null;
        int resultValue = 0;
        for (Map.Entry<G, Integer> entry : map.entrySet()) {
            int curValue = entry.getValue();
            if (curValue > resultValue) {
                result = entry.getKey();
                resultValue = curValue;
            }
        }

        if (resultValue == 0) {
            throw new RuntimeException("Can't find distribution maximum. All values are zeroes.");
        }

        return result;
    }

    /**
     * Returns such first element G after center, from which following numberOfElements elements
     * are less than epsValue.
     */
    public G getTail(G center, int dx, int epsValue, int numberOfElements) {
        int v = (Integer) center;
        int c = 0;
        for (int cv = v; ; cv += dx) {
            if (get((G) (Integer) cv) >= epsValue) {
                v = cv + dx;
                c = 0;
            } else {
                c++;
                if (c == numberOfElements) {
                    return (G) (Integer) v;
                }
            }
        }
    }
    

    public static class Threshold<G> {
        /**
         * First element from which tail begins.
         * May be null, that means that no threshold found, all elements should be before it.
         */
        public G threshold;

        /**
         * Before threshold sum, after threshold sum, and all.
         */
        public int before, after, all;
    }

    public Threshold<G> getTailThreshold(double maxPercentOfTailSum) {
        int all = 0;
        for (Map.Entry<G, Integer> entry : map.entrySet()) {
            all += entry.getValue();
        }

        G first = null;
        int beforeSum = 0;
        for (Map.Entry<G, Integer> entry : map.entrySet()) {
            // check if we can use this (entry.key) threshold
            if ((all - beforeSum) * 100.0 / all <= maxPercentOfTailSum) {
                first = entry.getKey();
                break;
            }
            beforeSum += entry.getValue();
        }

        Threshold<G> result = new Threshold<G>();
        result.threshold = first;
        result.all = all;
        result.before = beforeSum;
        result.after = all - beforeSum;
        return result;
    }
    
    public Threshold<G> calculateThresholdInfo(G threshold) {
        int all = 0;
        int beforeSum = 0;
        for (Map.Entry<G, Integer> entry : map.entrySet()) {
            all += entry.getValue();
            Comparable<G> key = (Comparable<G>) entry.getKey();
            if ((threshold == null) || (key.compareTo(threshold) < 0)) {
                beforeSum += entry.getValue();
            }
        }

        Threshold<G> result = new Threshold<G>();
        result.threshold = threshold;
        result.all = all;
        result.before = beforeSum;
        result.after = all - beforeSum;
        return result;
    }


    /**
     * Calculates threshold, from which main component of distribution begins, starting from values,
     * that are more than or equal to <param>maxValuePercent</param> percent of distribution center's value.
     */
    public Threshold<G> getDistributionLeftThreshold(G distributionCenter, double maxValuePercent) {
        int distributionCenterValue = map.get(distributionCenter);
        int thresholdValue = (int) Math.ceil(distributionCenterValue * (maxValuePercent / 100.0));

        G curThreshold = null;
        int prevValue = 0;
        boolean descendingSegmentFound = false;
        for (Map.Entry<G, Integer> entry : map.entrySet()) {
            if (entry.getKey().equals(distributionCenter)) {
                break;
            }

            int curValue = entry.getValue();
            if (curValue >= thresholdValue) {
                if (curThreshold == null) {
                    curThreshold = entry.getKey();
                }
                if (curValue < prevValue) {
                    descendingSegmentFound = true;
                }
            } else {
                curThreshold = null;
                descendingSegmentFound = false;
            }
            prevValue = curValue;
        }

        if (descendingSegmentFound) {
            System.err.println("getDistributionLeftThreshold method: " +
                    "Descending segment found after left threshold and before distribution center!");
        }
        Threshold<G> result = calculateThresholdInfo(curThreshold);
        return result;
    }

    /**
     * Calculates threshold, from which tail of the main component of distribution begins,
     * i.e. first element after the distribution center with value less
     * than <param>maxValuePercent</param> percent of distribution center's value.
     */
    public Threshold<G> getDistributionRightThreshold(G distributionCenter, double maxValuePercent) {
        int distributionCenterValue = map.get(distributionCenter);
        int thresholdValue = (int) Math.ceil(distributionCenterValue * (maxValuePercent / 100.0));

        boolean foundCenter = false;
        G curThreshold = null;
        int prevValue = 0;
        boolean ascendSegmentFound = false;
        for (Map.Entry<G, Integer> entry : map.entrySet()) {
            if (!foundCenter && !entry.getKey().equals(distributionCenter)) {
                continue;
            }
            foundCenter = true;

            int curValue = entry.getValue();
            if (curValue < thresholdValue) {
                curThreshold = entry.getKey();
                break;
            } else {
                if (prevValue != 0 && curValue > prevValue) {
                    ascendSegmentFound = true;
                }
            }
            prevValue = curValue;
        }

        if (ascendSegmentFound) {
            System.err.println("getDistributionRightThreshold method: " +
                    "Ascending segment found after distribution center and before right threshold!");
        }
        Threshold<G> result = calculateThresholdInfo(curThreshold);
        return result;
    }



    public double calculateVariance() {
        int min = (Integer) map.firstKey();
        int max = (Integer) map.lastKey();
        return calculateVariance(min, max);
    }

    /**
     * Calculates variance from <code>from</code> to <code>to</code> inclusive.
     */
    public double calculateVariance(int from, int to) {
        Variance variance = new Variance();
        
        double[] v = new double[to - from + 1];
        double[] w = new double[to - from + 1];
        
        for (int i = from; i <= to; i++) {
            Integer cv = get((G) (Integer) i);
            v[i - from] = i;
            w[i - from] = cv;
        }

        double var = variance.evaluate(v, w);
        return var;
    }

    public double calculateMean() {
        int min = (Integer) map.firstKey();
        int max = (Integer) map.lastKey();
        return calculateMean(min, max);
    }

    public double calculateMean(int from, int to) {
        Mean mean = new Mean();

        double[] v = new double[to - from + 1];
        double[] w = new double[to - from + 1];

        for (int i = from; i <= to; i++) {
            Integer cv = get((G) (Integer) i);
            v[i - from] = i;
            w[i - from] = cv;
        }

        double m = mean.evaluate(v, w);
        return m;
    }


    public void smoothOut(double variance) {
        NormalDistribution distrib = new NormalDistribution(0, variance);

        int left = (Integer) ((TreeMap<G, Integer>) map).firstKey();
        int right = (Integer) ((TreeMap<G, Integer>) map).lastKey();
        Map<Integer, Double> calcMap = new HashMap<Integer, Double>();

        for (Map.Entry<G, Integer> entry : map.entrySet()) {
            Integer x = (Integer) entry.getKey();
            int v = entry.getValue();

            int dx = 0;
            while (true) {
                double nv = v * distrib.getProb(dx);

                if (nv * (right - left) < 0.5) {
                    break;
                }
                if ((x - dx < left) && (right < x + dx)) {
                    break;
                }

                if (x - dx >= left) {
                    Double fv = calcMap.get(x - dx);
                    if (fv == null) {
                        fv = 0.0;
                    }
                    fv += nv;
                    calcMap.put(x - dx, fv);
                }
                if ((dx != 0) && (x + dx <= right)) {
                    Double fv = calcMap.get(x + dx);
                    if (fv == null) {
                        fv = 0.0;
                    }
                    fv += nv;
                    calcMap.put(x + dx, fv);
                }

                dx++;
            }
        }

        TreeMap<G, Integer> newMap = new TreeMap<G, Integer>();
        for (Map.Entry<Integer, Double> entry : calcMap.entrySet()) {
            Integer x = entry.getKey();
//            double v = entry.getValue();
            double v = distrib.getProb(x) * 11800 * 64;
            newMap.put((G) x, (int) Math.round(v));
        }

        map = newMap;
    }

    public static void main(String[] args) {
        QuantitativeStatistics<Integer> statistics = new QuantitativeStatistics<Integer>();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (in.ready()) {
                String s = in.readLine();
                int i = Integer.parseInt(s);
                statistics.add(i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Statistics:");
        System.out.println(statistics);
    }
}
