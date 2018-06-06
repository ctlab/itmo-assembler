package ru.ifmo.genetics.tools.irf;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.ifmo.genetics.dna.LightDna;

public class FillingResult {

    public int leftSkip = 0;
    public int rightSkip = 0;

    public enum ResultType { OK, AMBIGUOUS, FAIL}

    @Nullable
    public final LightDna dna;

    @NotNull
    public final ResultType type;

    public FillingResult(LightDna dna, @NotNull ResultType type) {
        this.dna = dna;
        this.type = type;
    }

    public static FillingResult ok(@NotNull LightDna dna) {
        return new FillingResult(dna, ResultType.OK);
    }

    public static FillingResult ok(@NotNull LightDna dna, int leftSkip, int rightSkip) {
        FillingResult res = new FillingResult(dna, ResultType.OK);
        res.leftSkip = leftSkip;
        res.rightSkip = rightSkip;
        return res;
    }

    public static FillingResult fail() {
        return new FillingResult(null, ResultType.FAIL);
    }

    public static FillingResult ambiguous() {
        return new FillingResult(null, ResultType.AMBIGUOUS);
    }
}
