package ru.ifmo.genetics.statistics;

public class UpdateOracle {

    double[] updatePoints = {5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95};    // %
    private int barrier = 0;

    public static final int FIRST_UPDATE_TIME = 3000;   // 3 sec
    private boolean wasFirstUpdate = false;
    
    public UpdateOracle() {}
    public UpdateOracle(double[] updatePoints) {
        this.updatePoints = updatePoints;
    }

    public void reset() {
        barrier = 0;
        wasFirstUpdate = false;
    }


    public boolean shouldUpdate(double curProgress, long timePassed) {
        if (barrier >= updatePoints.length) {
            return false;
        }

        curProgress *= 100;
        if (curProgress >= updatePoints[barrier]) {
            while (barrier < updatePoints.length && curProgress >= updatePoints[barrier]) {
                barrier++;
            }
            return true;
        }
        if (barrier == 0 && !wasFirstUpdate && timePassed >= FIRST_UPDATE_TIME && curProgress >= 0.5) {
            wasFirstUpdate = true;
            return true;
        }
        return false;
    }
}
