package ru.ifmo.genetics.tools.scaffolding.stupid;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Erf;
import ru.ifmo.genetics.tools.scaffolding.MostProbableDistance;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA. User: niyaznigmatul Date: 26.03.12 Time: 21:07 To
 * change this template use File | Settings | File Templates.
 */
public class Solver {

    static void drawGraph(final int contigLength1,
                                           final int contigLength2, final int[] d1, final int[] d2,
                                           final int dnaLength, final int allReads) throws MathException {
        JFrame frame = new JFrame() {
            @Override
            public void paint(Graphics g) {
                g.setColor(Color.RED);
                for (int d = 100; d <= 6000; d++) {
                    try {
                        double x = Math.abs(MostProbableDistance.getProbabilityThatAllMatepairsMatch(d,
                                contigLength1, contigLength2, d1, d2, dnaLength, allReads));
                        g.drawOval((d - 100) / 10, (int) (x * 2) + 50, 2, 2);
                    } catch (MathException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
