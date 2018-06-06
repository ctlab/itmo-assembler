package ru.ifmo.genetics.statistics;

/**
 * Class for measuring process running time and predicting remaining time.
 */
public class ProcessTimer {
    public Timer timer = new Timer();
    UpdateOracle oracle = new UpdateOracle();

    public ProcessTimer() {
        reset();
    }

    public void reset() {
        allTime = -1;
        oracle.reset();
        timer.start();
    }

    public String getRunningTime() {
        return timer.toClockLikeString();
    }

    private long allTime;

    void updateAllTime(double curProgress) {
        allTime = (long) (timer.getTime() / curProgress);
    }

    void updateRemainingTime(double curProgress) {
        if (oracle.shouldUpdate(curProgress, timer.getTime())) {
            updateAllTime(curProgress);
        }
    }

    /**
     * Returns "", if progress < 10, and "H*:MM:SS" otherwise
     */
    public String getRemainingTime(double curProgress) {
        updateRemainingTime(curProgress);
        if (allTime != -1) {
            return Timer.toClockLikeString(Math.max(0, allTime - timer.getTime()));
        }
        return "";
    }

    /**
     * Returns "", if progress < 10, and "H*:MM:SS" otherwise
     */
    public String getRemainingTime(double curProgress, boolean update) {
        if (update) {
            updateAllTime(curProgress);
        }
        if (allTime != -1) {
            return Timer.toClockLikeString(Math.max(0, allTime - timer.getTime()));
        }
        return "";
    }

    public String getRemainingTimeUS(double curProgress) {
        updateRemainingTime(curProgress);
        if (allTime != -1) {
            return Timer.timeToStringWithoutMs(Math.max(0, allTime - timer.getTime()));
        }
        return "";
    }
}
