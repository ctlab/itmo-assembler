package ru.ifmo.genetics.statistics;

public class UpdateOracleEveryNew {

    private double eps = 1e-5;
    private double lastValue = -999;

    public UpdateOracleEveryNew() {}

    public void reset() {
        lastValue = -999;
    }


    public boolean shouldUpdate(double curProgress) {
        if (Math.abs(curProgress - lastValue) >= eps) {
            lastValue = curProgress;
            return true;
        }
        return false;
    }
}
