package ru.ifmo.genetics.tools.rf;

public enum Orientation {
    FF(true, true),
    FR(true, false),
    RF(false, true),
    RR(false, false);

    public final boolean firstIsForward;
    public final boolean secondIsForward;

    private Orientation(boolean firstIsForward, boolean secondIsForward) {
        this.firstIsForward = firstIsForward;
        this.secondIsForward = secondIsForward;
    }

    public static Orientation fromString(String s) {
        if (s.equals("FF")) {
            return FF;
        } else if (s.equals("FR")) {
            return FR;
        } else if (s.equals("RF")) {
            return RF;
        } else if (s.equals("RR")) {
            return RR;
        } else {
            throw new IllegalArgumentException("Unknown orientation '" + s + "', expecting FF, FR, RF or RR.");
        }
    }
}
