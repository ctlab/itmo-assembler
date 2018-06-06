package ru.ifmo.genetics.utils.tool.values;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

public class IfYielder<T> extends Yielder<T> {
    private final InValue<Boolean> condition;
    private final InValue<T> ifTrueValue;
    private final InValue<T> ifFalseValue;

    public IfYielder(InValue<Boolean> condition, InValue<T> ifTrueValue, InValue<T> ifFalseValue) {
        this.condition = condition;
        this.ifTrueValue = ifTrueValue;
        this.ifFalseValue = ifFalseValue;
    }

    @Override
    public T yield() {
        return condition.get() ? ifTrueValue.get() : ifFalseValue.get();
    }

    @Override
    public String description() {
        return "selects value based on condition " + condition;
    }
}
