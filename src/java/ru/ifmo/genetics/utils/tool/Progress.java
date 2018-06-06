package ru.ifmo.genetics.utils.tool;

import ru.ifmo.genetics.statistics.ProcessTimer;

/**
 * Class to manage progress information.
 * Assuming this step is running like this:
 *   * runImpl()        -- one can use immediate progress bar
 *   * runAllSteps()    -- can also use doneSteps variable
 */
public class Progress {
    // Global progress bar, from 0.0 till 1.0.
    public double progress = 0;

    // Timer from the beginning of this step, without stopping
    public ProcessTimer timer = new ProcessTimer();


    // high level progress
    public int allSteps = 1;           // assuming runImpl is a step
    public int doneSteps = 0;
    public double[] stepCoef = null;

    public Tool currentRunningStep = null;

    // low level progress
    private long totalTasks = 0;
    private volatile long doneTasks = 0;


    public void reset() {
        progress = 0;
        timer.reset();
        allSteps = 1;
        doneSteps = 0;
        currentRunningStep = null;
        totalTasks = 0;
        doneTasks = 0;
    }
    public void start() {
        timer.timer.start();
    }



    public void updateProgress() {
        double lowLevelProgress = 0.0;
        if (currentRunningStep != null) {
            currentRunningStep.progress.updateProgress();
            lowLevelProgress = currentRunningStep.progress.progress;
        } else if (totalTasks != 0) {
            lowLevelProgress = Math.min(1, doneTasks * 1.0 / totalTasks);
        }

        if (stepCoef != null) {
            double done = 0;
            for (int i = 0; i < doneSteps; i++) {
                done += stepCoef[i];
            }
            double curStepCoef = (doneSteps < stepCoef.length) ? stepCoef[doneSteps] : 0.0;
            progress = Math.min(1, done + lowLevelProgress * curStepCoef);
        } else {
            progress = Math.min(1, (doneSteps + lowLevelProgress) / allSteps);
        }
    }

    public String getRemainingTime() {
        updateProgress();
        return timer.getRemainingTime(progress);
    }
    
    public String getRunningTime() {
        return timer.timer.toClockLikeString();
    }


    // high level progress
    void setStepsNumber(int steps) {
        assert steps >= 0;
        if (steps != 0) {
            allSteps = steps;
            doneSteps = 0;
        }
    }
    
    void finishedCurrentStep() {
        doneSteps++;
        currentRunningStep = null;
    }
    
    void startingStep(Tool step) {
        currentRunningStep = step;
    }


    // low level progress
    private long updateMask = -1;

    public void setTotalTasks(long totalTasks) {
        assert totalTasks >= 0;
        this.totalTasks = totalTasks;
        doneTasks = 0;

        long dv = Long.highestOneBit(totalTasks);
        dv >>= 10;    // ~0.1%
        updateMask = (dv == 0) ? 0 : (dv - 1);
    }

    /**
     * Use this method if program will call it with every next value of doneTasks
     */
    public void updateDoneTasks(long doneTasks) {
        if ((doneTasks & updateMask) == 0) {
            this.doneTasks = doneTasks;
        }
    }

    /**
     * Use this method if program will rarely call it or doneTasks will be increased a lot.
     */
    public void updateDoneTasksGreatly(long doneTasks) {
        this.doneTasks = doneTasks;
    }


    // progress bar (shows only low level progress)
    private ProcessTimer pbTimer;
    private Thread progressWriterThread = null;

    public void createProgressBar() {
        assert progressWriterThread == null;
        progressWriterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    double progress = 1.0;
                    if (totalTasks != 0) {
                        progress = Math.min(1, doneTasks * 1.0 / totalTasks);
                    }
                    String curProgress = "Progress: " + String.format("%.1f", progress * 100.0) + "%";
                    String remTime = pbTimer.getRemainingTimeUS(progress);
                    if (remTime.length() > 0) {
                        curProgress += ", remaining time: " + remTime + " ";
                    }

                    Tool.showProgressOnly(curProgress);

                    try {
                        Thread.sleep(1000);     // 1 second
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                Tool.clearProgress();
            }
        });
        pbTimer = new ProcessTimer();
        progressWriterThread.start();
    }

    public void destroyProgressBar() {
        progressWriterThread.interrupt();
        try {
            progressWriterThread.join();    // waiting for thread to finish
        } catch (InterruptedException e) {
            Tool.debug(Tool.mainLogger, "Interrupted in destroyProgressBar while waiting for progressWriterThread to finish");
        }
        progressWriterThread = null;
    }
}
