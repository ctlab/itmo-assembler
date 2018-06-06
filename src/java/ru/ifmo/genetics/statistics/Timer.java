package ru.ifmo.genetics.statistics;

public class Timer {
    private long time;

    public Timer() {
        start();
    }

    public void start() {
        time = System.currentTimeMillis();
    }

    public long getTime() {
        return System.currentTimeMillis() - time;
    }
    
    public static String timeToString(long timeMillis) {
        long ms = timeMillis % 1000;
        long s = timeMillis / 1000;
        if (s == 0) {
            return ms + " ms";
        }
        
        long m = s / 60;
        s %= 60;
        if (m == 0) {
            return s + " s " + ms + " ms";
        }

        long h = m / 60;
        m %= 60;
        if (h == 0) {
            return m + " min " + s + " s";
        }
        
        long d = h / 24;
        h %= 24;
        if (d == 0) {
            return h + " h " + m + " min";
        }
        
        return d + " day" + ((d > 1)? "s" : "") + " " + h + " h";
    }

    public static String timeToStringWithoutMs(long timeMillis) {
        long s = Math.round(timeMillis / 1000.0);
        if (s < 60) {
            return s + " s";
        }

        return timeToString(s * 1000);
    }

    @Override
    public String toString() {
        return timeToString(getTime());
    }
    
    public static String toClockLikeString(long timeMillis) {
        long s = Math.round(timeMillis / 1000.0);
        long m = s / 60;
        long h = m / 60;
        s %= 60;
        m %= 60;
        return h + ":" + (m / 10) + (m % 10) + ":" + (s / 10) + (s % 10);
    }

    public String toClockLikeString() {
        return toClockLikeString(getTime());
    }
    
}
